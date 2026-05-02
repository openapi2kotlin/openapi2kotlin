#!/usr/bin/env python3
from __future__ import annotations

import argparse
import html
import json
import re
from pathlib import Path
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
EXT_PATH = ROOT / "gradle-plugin/src/main/kotlin/dev/openapi2kotlin/gradleplugin/OpenApi2KotlinExtension.kt"
README_PATH = ROOT / "README.md"
SITE_DOCS_DIR = ROOT / "site/docs"
SITE_PUBLIC_DIR = ROOT / "site/public"
SITE_BASE_URL = "https://openapi2kotlin.dev"


def extract_class_body(text: str, class_name: str) -> str:
    m = re.search(rf"\bclass\s+{re.escape(class_name)}\b[^{{]*{{", text)
    if not m:
        raise RuntimeError(f"Class not found: {class_name}")
    i = m.end() - 1
    depth = 0
    start = i + 1
    for j in range(i, len(text)):
        ch = text[j]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return text[start:j]
    raise RuntimeError(f"Unbalanced braces for class: {class_name}")


def parse_kdoc_meta(block: str) -> dict[str, str]:
    out: dict[str, str] = {}
    current_key: str | None = None
    for raw in block.splitlines():
        if raw.strip() == "*/":
            continue
        line = raw.strip()
        if line.startswith("*"):
            line = line[1:]
            if line.startswith(" "):
                line = line[1:]
        line = line.rstrip()
        m = re.match(r"^(description|default|values|required):\s*(.+)$", line)
        if m:
            current_key = m.group(1)
            out[current_key] = m.group(2).strip()
            continue
        if current_key == "description":
            out[current_key] = out[current_key] + "\n" + line
            continue
        if current_key and line.strip():
            out[current_key] = out[current_key] + "\n" + line
    return out


def parse_props(class_body: str) -> list[dict[str, str]]:
    results: list[dict[str, str]] = []
    lines = class_body.splitlines()

    member_lines = [ln for ln in lines if ln.strip()]
    if not member_lines:
        return results

    # Top-level members in this class share the smallest indentation in its body.
    base_indent = min(len(ln) - len(ln.lstrip(" ")) for ln in member_lines)

    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        indent = len(line) - len(line.lstrip(" "))

        if indent == base_indent and stripped.startswith("/**"):
            kdoc_lines: list[str] = [line]
            while i + 1 < len(lines) and "*/" not in lines[i]:
                i += 1
                kdoc_lines.append(lines[i])

            # find the next meaningful line
            j = i + 1
            while j < len(lines) and not lines[j].strip():
                j += 1

            if j >= len(lines):
                break

            next_line = lines[j]
            next_indent = len(next_line) - len(next_line.lstrip(" "))
            next_stripped = next_line.strip()

            if next_indent == base_indent and next_stripped.startswith("var "):
                m = re.match(r"var\s+(\w+)\s*:\s*([^=\n]+?)(?:\s*=\s*(.+))?$", next_stripped)
                if m:
                    name = m.group(1)
                    typ = m.group(2).strip()
                    if "Extension" not in typ:
                        kdoc = parse_kdoc_meta("\n".join(kdoc_lines))
                        results.append({
                            "name": name,
                            "type": typ,
                            "description": kdoc.get("description", ""),
                            "default": kdoc.get("default", ""),
                            "values": kdoc.get("values", ""),
                            "required": kdoc.get("required", "false"),
                        })
                i = j
        i += 1

    return results


def parse_enum_values(text: str, enum_name: str) -> list[str]:
    body = extract_class_body(text, enum_name)
    vals: list[str] = []
    for line in body.splitlines():
        s = line.strip()
        if not s or s.startswith("companion") or s.startswith(";"):
            continue
        if s.startswith("}"):
            break
        m = re.match(r"^([A-Za-z0-9_]+)\(", s)
        if m:
            vals.append(m.group(1))
    return vals


def parse_all_enums(text: str) -> dict[str, list[str]]:
    out: dict[str, list[str]] = {}
    for m in re.finditer(r"\benum\s+class\s+(\w+)\b", text):
        enum_name = m.group(1)
        out[enum_name] = parse_enum_values(text, enum_name)
    return out


def normalize_type_name(type_name: str) -> str:
    base = type_name.strip().replace("?", "")
    base = base.split("<", 1)[0].strip()
    return base.split(".")[-1]


def row(property_name: str, data: dict[str, str]) -> dict[str, str]:
    required_raw = (data.get("required") or "false").strip().lower()
    return {
        "property": property_name,
        "description": data.get("description", ""),
        "values": data.get("values", ""),
        "default": data.get("default", ""),
        "required": required_raw in {"true", "yes", "1"},
    }


def minimal_target_snippet(target_kind: str, library: str) -> str:
    target_name = target_kind.lower()
    return "\n".join(
        [
            f"    {target_name} {{",
            f"        packageName = \"dev.openapi2kotlin.{target_name}\"",
            f"        library = {library}",
            "    }",
        ]
    )


def minimal_config_snippets(client_libraries: list[str], server_libraries: list[str]) -> dict[str, dict[str, str]]:
    return {
        "client": {
            library: minimal_target_snippet("Client", library)
            for library in client_libraries
        },
        "server": {
            library: minimal_target_snippet("Server", library)
            for library in server_libraries
        },
    }


def model_config_snippet(serialization: str) -> str:
    return f"""    model {{
        packageName = "dev.openapi2kotlin.model"
        serialization = {serialization}
        validation = None
        double2BigDecimal = false
        float2BigDecimal = false
        integer2Long = true
    }}"""


def target_config_snippet(target_kind: str, library: str, swagger: bool | None = None) -> str:
    lines = [
        f"    {target_kind.lower()} {{",
        f"        packageName = \"dev.openapi2kotlin.{target_kind.lower()}\"",
        f"        library = {library}",
    ]
    if swagger is not None:
        lines.append(f"        swagger = {str(swagger).lower()}")
    lines.extend(
        [
            "        basePathVar = \"basePath\"",
            "        methodNameSingularized = true",
            "        methodNamePluralized = true",
            "        methodNameFromOperationId = false",
            "    }",
        ]
    )
    return "\n".join(lines)


def full_config_snippet(target_kind: str, library: str, serialization: str, swagger: bool | None = None) -> str:
    return "\n".join(
        [
            "openapi2kotlin {",
            "    inputSpec = \"$projectDir/src/main/resources/openapi.yaml\"",
            "    outputDir = layout.buildDirectory.dir(\"generated/src/main/kotlin\").get().asFile.path",
            "    enabled = true",
            "",
            model_config_snippet(serialization),
            "",
            target_config_snippet(target_kind, library, swagger),
            "}",
        ]
    )


def model_serialization_for_client_library(library: str) -> str:
    return "Jackson" if library == "RestClient" else "KotlinX"


def model_serialization_for_server_library(library: str) -> str:
    return "Jackson" if library == "Spring" else "KotlinX"


def default_swagger_for_server_library(library: str) -> bool:
    return library == "Spring"


def config_snippets(client_libraries: list[str], server_libraries: list[str]) -> dict[str, dict[str, str]]:
    return {
        "client": {
            library: full_config_snippet(
                "Client",
                library,
                model_serialization_for_client_library(library),
            )
            for library in client_libraries
        },
        "server": {
            library: full_config_snippet(
                "Server",
                library,
                model_serialization_for_server_library(library),
                swagger=default_swagger_for_server_library(library),
            )
            for library in server_libraries
        },
    }


def generate(version: str) -> dict:
    text = EXT_PATH.read_text()

    root_props = parse_props(extract_class_body(text, "OpenApi2KotlinExtension"))
    model_props = parse_props(extract_class_body(text, "ModelConfigExtension"))
    client_props = parse_props(extract_class_body(text, "ClientExtension"))
    server_props = parse_props(extract_class_body(text, "ServerExtension"))

    enum_values_by_type = parse_all_enums(text)
    client_enum_values = enum_values_by_type.get("ClientLibrary", [])
    server_enum_values = enum_values_by_type.get("ServerLibrary", [])

    rows: list[dict[str, str]] = []

    def append_props(props: list[dict[str, str]], prefix: str = "") -> None:
        for p in props:
            enriched = dict(p)
            type_name = normalize_type_name(enriched.get("type", ""))
            if not enriched.get("values") and type_name in enum_values_by_type:
                enriched["values"] = ", ".join(enum_values_by_type[type_name])

            property_name = enriched["name"] if not prefix else f"{prefix}.{enriched['name']}"
            rows.append(row(property_name, enriched))

    append_props(root_props)
    append_props(model_props, "model")
    append_props(client_props, "client")
    append_props(server_props, "server")

    docs = {
        "version": version,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "clientLibraries": client_enum_values,
        "serverLibraries": server_enum_values,
        "defaultClientLibrary": "Ktor",
        "defaultServerLibrary": "Ktor",
        "configRows": rows,
        "snippets": minimal_config_snippets(
            client_libraries=client_enum_values,
            server_libraries=server_enum_values,
        ),
    }
    return docs


def table_escape(v: str) -> str:
    return v.replace("|", "\\|").replace("\n", "<br>")


def split_description_blocks(text: str) -> list[dict[str, str]]:
    normalized = text.strip()
    fenced_match = re.match(r"^(.*?)(e\.g\.|i\.e\.)\s*```([\s\S]+?)```$", normalized, re.I)
    if fenced_match:
        before = fenced_match.group(1).strip().rstrip(",;:")
        prefix = fenced_match.group(2).lower()
        code = fenced_match.group(3).strip()
        parts: list[dict[str, str]] = []
        if before:
            parts.append({"kind": "paragraph", "text": before})
        if code:
            parts.append({"kind": "example", "prefix": prefix, "text": code})
        return parts

    lines = text.strip().splitlines()
    parts: list[dict[str, str]] = []
    paragraph_lines: list[str] = []
    example_lines: list[str] = []
    example_prefix: str | None = None

    def flush_paragraph() -> None:
        nonlocal paragraph_lines
        value = " ".join(line.strip() for line in paragraph_lines).strip()
        if value:
            parts.append({"kind": "paragraph", "text": value})
        paragraph_lines = []

    def flush_example() -> None:
        nonlocal example_lines, example_prefix
        value = "\n".join(example_lines).strip()
        if example_prefix and value:
            parts.append({"kind": "example", "prefix": example_prefix, "text": value})
        example_lines = []
        example_prefix = None

    for raw_line in lines:
        line = raw_line.rstrip()
        marker = line.strip().lower()

        if marker in {"e.g.", "i.e."}:
            flush_paragraph()
            flush_example()
            example_prefix = marker
            continue

        if example_prefix is not None:
            if not line.strip():
                flush_example()
                continue
            if line.strip() == "```":
                continue
            example_lines.append(line)
            continue

        if not line.strip():
            flush_paragraph()
            continue

        paragraph_lines.append(line)

    flush_paragraph()
    flush_example()
    return parts


def format_readme_description(text: str) -> str:
    parts = split_description_blocks(text)
    if not parts:
        return "-"

    rendered: list[str] = []
    for part in parts:
        if part["kind"] == "paragraph":
            rendered.append(table_escape(part["text"]))
        else:
            formatted_lines: list[str] = []
            for line in part["text"].splitlines():
                leading_spaces = len(line) - len(line.lstrip(" "))
                indent = "&nbsp;" * leading_spaces
                content = html.escape(line.lstrip(" "))
                formatted_lines.append(f"{indent}<code>{content}</code>" if content else "")
            code_lines = "<br>".join(formatted_lines)
            rendered.append(f"{part['prefix']}<br>{code_lines}")
    return "<br><br>".join(rendered)


def update_readme(rows: list[dict[str, str]]) -> None:
    table_lines = [
        "| Property | Description | Values | Required | Default |",
        "|---|---|---|---|---|",
    ]
    for r in rows:
        table_lines.append(
            "| `{}` | {} | {} | {} | {} |".format(
                table_escape(r["property"]),
                format_readme_description(r["description"] or "-"),
                table_escape(r["values"] or "-"),
                "true" if r.get("required") else "false",
                table_escape(r["default"] or "-"),
            )
        )

    section = "### Configuration options\n\n" + "\n".join(table_lines) + "\n\n"

    text = README_PATH.read_text()
    pattern = re.compile(r"### Configuration options\n\n[\s\S]*?\n### Requirements\n", re.M)
    repl = section + "### Requirements\n"
    new_text, n = pattern.subn(repl, text, count=1)
    if n != 1:
        raise RuntimeError("Could not update README Configuration options section")
    README_PATH.write_text(new_text)


def write_site_docs(version: str, docs: dict) -> None:
    SITE_DOCS_DIR.mkdir(parents=True, exist_ok=True)
    (SITE_DOCS_DIR / f"{version}.json").write_text(json.dumps(docs, indent=2) + "\n")

    versions = sorted(
        [p.stem for p in SITE_DOCS_DIR.glob("*.json") if re.match(r"^\d+\.\d+\.\d+([-.].+)?$", p.stem)],
        key=lambda s: tuple(int(x) if x.isdigit() else x for x in re.split(r"[.-]", s)),
    )
    (SITE_DOCS_DIR / "versions.json").write_text(json.dumps({"versions": versions}, indent=2) + "\n")


def update_existing_site_doc_snippets() -> None:
    for path in sorted(SITE_DOCS_DIR.glob("*.json")):
        if not re.match(r"^\d+\.\d+\.\d+([-.].+)?$", path.stem):
            continue

        docs = json.loads(path.read_text())
        snippets = minimal_config_snippets(
            client_libraries=docs.get("clientLibraries", []),
            server_libraries=docs.get("serverLibraries", []),
        )
        if docs.get("snippets") == snippets:
            continue

        docs["snippets"] = snippets
        path.write_text(json.dumps(docs, indent=2) + "\n")


def load_all_site_docs() -> list[dict]:
    docs_by_version: list[dict] = []
    for path in sorted(SITE_DOCS_DIR.glob("*.json")):
        if not re.match(r"^\d+\.\d+\.\d+([-.].+)?$", path.stem):
            continue
        docs_by_version.append(json.loads(path.read_text()))
    return docs_by_version


def compare_versions(a: str, b: str) -> int:
    pa = a.split(".")
    pb = b.split(".")
    for xa, xb in zip(pa, pb):
        na = int(xa) if xa.isdigit() else xa
        nb = int(xb) if xb.isdigit() else xb
        if na == nb:
            continue
        return -1 if na < nb else 1
    if len(pa) == len(pb):
        return 0
    return -1 if len(pa) < len(pb) else 1


def sort_versions_desc(versions: list[str]) -> list[str]:
    return sorted(
        versions,
        key=lambda s: tuple(int(x) if x.isdigit() else x for x in re.split(r"[.-]", s)),
        reverse=True,
    )


def format_latest_doc_link(version: str, latest_stable_version: str) -> str:
    if version == latest_stable_version:
        return f"{SITE_BASE_URL}/"
    return f"{SITE_BASE_URL}/{version}"


def format_versioned_doc_link(version: str) -> str:
    return f"{SITE_BASE_URL}/{version}"


def llms_header(title: str, summary: str) -> list[str]:
    return [
        f"# {title}",
        "",
        f"> {summary}",
        "",
    ]


def format_multiline_value(value: str) -> str:
    normalized = value.strip()
    if not normalized:
        return "-"
    return normalized


def looks_like_code_block(value: str) -> bool:
    normalized = value.strip()
    return "```" in normalized or "\n" in normalized or "layout.buildDirectory" in normalized or "$projectDir" in normalized


def render_field(label: str, value: str) -> list[str]:
    normalized = format_multiline_value(value)
    if normalized == "-":
        return [f"- {label}: -"]

    if looks_like_code_block(normalized):
        stripped = normalized.replace("```", "").strip()
        return [
            f"- {label}:",
            "```kotlin",
            stripped,
            "```",
        ]

    return [f"- {label}: {normalized}"]


def render_config_rows(rows: list[dict[str, str]]) -> list[str]:
    lines = ["## Configuration Options", ""]
    for config_row in rows:
        lines.append(f"### `{config_row['property']}`")
        lines.append("")
        lines.append(f"- Required: {'true' if config_row.get('required') else 'false'}")
        lines.extend(render_field("Default", config_row.get("default", "")))
        lines.extend(render_field("Values", config_row.get("values", "")))
        lines.extend(render_field("Description", config_row.get("description", "")))
        lines.append("")
    return lines


def fenced(language: str, lines: list[str]) -> list[str]:
    return [
        f"```{language}",
        *lines,
        "```",
    ]


def demo_library_dir(version: str, target_kind: str, library: str) -> Path | None:
    slug = library.lower()
    path = ROOT / "demo" / version / "petstore3" / target_kind / f"openapi2kotlin-demo-{version}-petstore3-{target_kind}-{slug}"
    return path if path.exists() else None


def generated_file(demo_dir: Path, suffix: str) -> Path | None:
    matches = sorted((demo_dir / "build/generated/src/main/kotlin").glob(f"**/{suffix}"))
    return matches[0] if matches else None


def code_file_block(path: Path, language: str = "kotlin") -> list[str]:
    return [
        f"`{path.relative_to(ROOT)}`:",
        "",
        *fenced(language, path.read_text().splitlines()),
        "",
    ]


def code_excerpt_block(path: Path, title: str, lines: list[str], language: str = "kotlin") -> list[str]:
    return [
        f"`{path.relative_to(ROOT)}` {title}:",
        "",
        *fenced(language, lines),
        "",
    ]


def extract_first_matching_block(lines: list[str], contains: str, end_prefix: str | None = None) -> list[str]:
    for index, line in enumerate(lines):
        if contains not in line:
            continue

        start = index
        for previous in range(index - 1, -1, -1):
            if lines[previous].strip() == "/**":
                start = previous
                break
            if lines[previous].strip() == "":
                break

        if end_prefix is None:
            return lines[start:index + 1]

        end = index
        while end < len(lines) and not lines[end].startswith(end_prefix):
            end += 1
        if end < len(lines):
            end += 1
        return lines[start:end]
    return []


def extract_balanced_block(lines: list[str], contains: str) -> list[str]:
    for index, line in enumerate(lines):
        if contains not in line:
            continue

        start = index
        depth = 0
        seen_brace = False
        for end in range(index, len(lines)):
            depth += lines[end].count("{")
            if "{" in lines[end]:
                seen_brace = True
            depth -= lines[end].count("}")
            if seen_brace and depth == 0:
                return lines[start:end + 1]
    return []


def openapi_demo_excerpt(demo_dir: Path) -> list[str]:
    source = demo_dir / "src/main/resources/openapi.yaml"
    if not source.exists():
        return []

    lines = source.read_text().splitlines()
    route = extract_yaml_section(lines, "  /pet:", stop_indent=2)
    post = extract_yaml_section(route, "    post:", stop_indent=4)
    pet = extract_yaml_section(lines, "    Pet:", stop_indent=4)
    error = extract_yaml_section(lines, "    Error:", stop_indent=4)
    info = extract_yaml_section(lines, "info:", stop_indent=0)

    return [
        lines[0],
        *info,
        "paths:",
        "  /pet:",
        *strip_yaml_xml_blocks(post),
        "components:",
        "  schemas:",
        *strip_yaml_xml_blocks(pet),
        *strip_yaml_xml_blocks(error),
    ]


def extract_yaml_section(lines: list[str], header: str, stop_indent: int) -> list[str]:
    try:
        start = lines.index(header)
    except ValueError:
        return []

    section = [lines[start]]
    for line in lines[start + 1:]:
        if line.strip() and not line.startswith(" " * (stop_indent + 1)):
            break
        section.append(line)
    return section


def strip_yaml_xml_blocks(lines: list[str]) -> list[str]:
    stripped: list[str] = []
    skip_indent: int | None = None

    for line in lines:
        indent = len(line) - len(line.lstrip(" "))
        if skip_indent is not None:
            if line.strip() and indent > skip_indent:
                continue
            skip_indent = None

        if line.strip() in {"xml:", "application/xml:", "application/x-www-form-urlencoded:"}:
            skip_indent = indent
            continue

        stripped.append(line)

    return stripped


def render_how_it_works(docs: dict) -> list[str]:
    version = docs["version"]
    demo_dirs = [
        demo_library_dir(version, "client", library)
        for library in docs["clientLibraries"]
    ] + [
        demo_library_dir(version, "server", library)
        for library in docs["serverLibraries"]
    ]
    demo_dirs = [path for path in demo_dirs if path is not None]
    if not demo_dirs:
        return []

    openapi_lines = openapi_demo_excerpt(demo_dirs[0])
    if not openapi_lines:
        return []

    lines = [
        "## How It Works",
        "",
        f"Examples in this section are extracted from generated demo files for version {version}.",
        "",
        "Minimal OpenAPI excerpt based on the petstore3 demo `/pet` POST route; XML metadata is omitted:",
        "",
        *fenced("yaml", openapi_lines),
        "",
        "Generated sources land under `build/generated/src/main/kotlin`.",
        "",
    ]

    lines.extend(render_demo_models(demo_dirs[0]))
    lines.extend(render_demo_clients(version, docs["clientLibraries"]))
    lines.extend(render_demo_servers(version, docs["serverLibraries"]))
    return lines


def render_demo_models(demo_dir: Path) -> list[str]:
    lines = ["### Generated Models", ""]
    for suffix in ("model/Pet.kt", "model/Error.kt"):
        path = generated_file(demo_dir, suffix)
        if path is not None:
            lines.extend(code_file_block(path))
    return lines


def render_demo_clients(version: str, client_libraries: list[str]) -> list[str]:
    lines: list[str] = []
    for library in client_libraries:
        demo_dir = demo_library_dir(version, "client", library)
        if demo_dir is None:
            continue
        api_path = generated_file(demo_dir, "client/PetApi.kt")
        impl_path = generated_file(demo_dir, "client/PetApiImpl.kt")
        if api_path is None or impl_path is None:
            continue

        if not lines:
            lines.extend(["### Generated Clients", ""])
        lines.extend([f"#### {library} Client", ""])
        api_lines = api_path.read_text().splitlines()
        impl_lines = impl_path.read_text().splitlines()
        lines.extend(code_excerpt_block(api_path, "`createPet` excerpt", extract_first_matching_block(api_lines, "createPet(body: Pet)")))
        lines.extend(code_excerpt_block(impl_path, "`createPet` excerpt", extract_balanced_block(impl_lines, "createPet(body: Pet)")))
    return lines


def render_demo_servers(version: str, server_libraries: list[str]) -> list[str]:
    lines: list[str] = []
    for library in server_libraries:
        demo_dir = demo_library_dir(version, "server", library)
        if demo_dir is None:
            continue
        api_path = generated_file(demo_dir, "server/PetApi.kt")
        route_path = generated_file(demo_dir, "server/PetRoutes.kt")
        if api_path is None:
            continue

        if not lines:
            lines.extend(["### Generated Servers", ""])
        lines.extend([f"#### {library} Server", ""])
        api_lines = api_path.read_text().splitlines()
        lines.extend(code_excerpt_block(api_path, "`createPet` excerpt", extract_first_matching_block(api_lines, "createPet(")))

        if route_path is not None:
            route_lines = route_path.read_text().splitlines()
            route_excerpt = extract_balanced_block(route_lines, "\"/pet\" bind org.http4k.core.Method.POST") if library == "Http4k" else extract_balanced_block(route_lines, "post {")
            lines.extend(code_excerpt_block(route_path, "`POST /pet` route excerpt", route_excerpt))
    return lines


def render_snippets(docs: dict) -> list[str]:
    lines = ["## Configuration Snippets", ""]
    snippets_by_target = config_snippets(
        client_libraries=docs["clientLibraries"],
        server_libraries=docs["serverLibraries"],
    )
    for target_kind, snippets in (("Client", snippets_by_target["client"]), ("Server", snippets_by_target["server"])):
        lines.append(f"### {target_kind}")
        lines.append("")
        for library, snippet in snippets.items():
            lines.append(f"#### {library}")
            lines.append("")
            lines.append("```kotlin")
            lines.extend(snippet.splitlines())
            lines.append("```")
            lines.append("")
    return lines


def build_root_llms(latest_docs: dict, all_versions: list[str]) -> str:
    latest_version = latest_docs["version"]
    lines = llms_header(
        "Openapi2kotlin Documentation",
        "Openapi2kotlin generates Kotlin models, clients, and servers from OpenAPI specs for real applications.",
    )
    lines.extend(
        [
            f"Latest stable version: {latest_version}",
            "",
            "## Start Here",
            f"- Overview: {SITE_BASE_URL}/",
            f"- Installation: {SITE_BASE_URL}/#installation",
            f"- AI / LLMs: {SITE_BASE_URL}/#llms",
            f"- Under the Hood: {SITE_BASE_URL}/#under-the-hood",
            f"- API Reference: {SITE_BASE_URL}/#api-reference",
            "",
            "## Framework Targets",
            f"- Client libraries: {', '.join(latest_docs['clientLibraries'])}",
            f"- Server libraries: {', '.join(latest_docs['serverLibraries'])}",
            "",
            "## Versioned Documentation",
        ]
    )
    for version in all_versions:
        latest_or_versioned_url = format_latest_doc_link(version, latest_version)
        versioned_url = format_versioned_doc_link(version)
        lines.append(f"- {version}: {latest_or_versioned_url} ({versioned_url}/llms.txt)")
    lines.extend(["", *render_how_it_works(latest_docs), *render_config_rows(latest_docs["configRows"]), *render_snippets(latest_docs)])
    lines.extend(
        [
            "## External Links",
            "- GitHub: https://github.com/openapi2kotlin/openapi2kotlin",
            "- Maven Central: https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin",
            "- OpenAPI Specification: https://spec.openapis.org/oas/v3.2.0.html",
            "",
        ]
    )
    return "\n".join(lines)


def build_versioned_llms(docs: dict, latest_version: str) -> str:
    version = docs["version"]
    version_url = format_versioned_doc_link(version)
    lines = llms_header(
        f"Openapi2kotlin {version} Documentation",
        f"Version-pinned documentation for openapi2kotlin {version}. Use this file when you need links and structured data for this exact release.",
    )
    lines.extend(
        [
            f"Version: {version}",
            f"Latest stable version: {latest_version}",
            "",
            "## Start Here",
            f"- Versioned docs: {version_url}",
            f"- Installation: {version_url}#installation",
            f"- AI / LLMs: {version_url}#llms",
            f"- Under the Hood: {version_url}#under-the-hood",
            f"- API Reference: {version_url}#api-reference",
            "",
            "## Framework Targets",
            f"- Client libraries: {', '.join(docs['clientLibraries'])}",
            f"- Server libraries: {', '.join(docs['serverLibraries'])}",
            "",
            *render_how_it_works(docs),
            *render_config_rows(docs["configRows"]),
            *render_snippets(docs),
            "## External Links",
            "- GitHub: https://github.com/openapi2kotlin/openapi2kotlin",
            "- Maven Central: https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin",
            "",
        ]
    )
    return "\n".join(lines)


def write_site_llms(latest_stable_version: str) -> None:
    SITE_PUBLIC_DIR.mkdir(parents=True, exist_ok=True)
    all_docs = load_all_site_docs()
    docs_by_version = {docs["version"]: docs for docs in all_docs}

    if latest_stable_version not in docs_by_version:
        raise RuntimeError(f"Latest stable version {latest_stable_version} not found in site docs")

    all_versions = sort_versions_desc(list(docs_by_version))
    latest_docs = docs_by_version[latest_stable_version]

    (SITE_PUBLIC_DIR / "llms.txt").write_text(build_root_llms(latest_docs, all_versions) + "\n")

    for version, docs in docs_by_version.items():
        version_dir = SITE_PUBLIC_DIR / version
        version_dir.mkdir(parents=True, exist_ok=True)
        (version_dir / "llms.txt").write_text(build_versioned_llms(docs, latest_stable_version) + "\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("version", help="Docs version, e.g. 0.15.0")
    args = parser.parse_args()

    docs = generate(args.version)
    write_site_docs(args.version, docs)
    update_existing_site_doc_snippets()
    write_site_llms(args.version)
    update_readme(docs["configRows"])
    print(f"Generated docs for version {args.version}")


if __name__ == "__main__":
    main()
