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
                "RestClient": """    client {\n        packageName = \"dev.openapi2kotlin.client\"\n        library = RestClient\n    }""",
            },
            "server": {
                "Ktor": """    server {\n        packageName = \"dev.openapi2kotlin.server\"\n        library = Ktor\n    }""",
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


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("version", help="Docs version, e.g. 0.15.0")
    args = parser.parse_args()

    docs = generate(args.version)
    write_site_docs(args.version, docs)
    update_readme(docs["configRows"])
    print(f"Generated docs for version {args.version}")


if __name__ == "__main__":
    main()
