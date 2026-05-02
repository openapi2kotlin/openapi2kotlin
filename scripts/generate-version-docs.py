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
        "snippets": {
            "client": {
                "Ktor": """    client {\n        packageName = \"dev.openapi2kotlin.client\"\n        library = Ktor\n    }""",
                "Http4k": """    client {\n        packageName = \"dev.openapi2kotlin.client\"\n        library = Http4k\n    }""",
                "RestClient": """    client {\n        packageName = \"dev.openapi2kotlin.client\"\n        library = RestClient\n    }""",
            },
            "server": {
                "Ktor": """    server {\n        packageName = \"dev.openapi2kotlin.server\"\n        library = Ktor\n    }""",
                "Http4k": """    server {\n        packageName = \"dev.openapi2kotlin.server\"\n        library = Http4k\n    }""",
                "Spring": """    server {\n        packageName = \"dev.openapi2kotlin.server\"\n        library = Spring\n    }""",
            },
        },
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


def render_snippets(docs: dict) -> list[str]:
    lines = ["## Configuration Snippets", ""]
    for target_kind, snippets in (("Client", docs["snippets"]["client"]), ("Server", docs["snippets"]["server"])):
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
    lines.extend(["", *render_config_rows(latest_docs["configRows"]), *render_snippets(latest_docs)])
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
    write_site_llms(args.version)
    update_readme(docs["configRows"])
    print(f"Generated docs for version {args.version}")


if __name__ == "__main__":
    main()
