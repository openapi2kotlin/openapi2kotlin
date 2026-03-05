#!/usr/bin/env python3
from __future__ import annotations

import argparse
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
    for raw in block.splitlines():
        line = raw.strip()
        if line.startswith("*"):
            line = line[1:].strip()
        m = re.match(r"^(description|default|values|required):\s*(.+)$", line)
        if m:
            out[m.group(1)] = m.group(2).strip()
    return out


def parse_props(class_body: str) -> list[dict[str, str]]:
    results: list[dict[str, str]] = []
    pattern = re.compile(r"/\*\*(.*?)\*/\s*var\s+(\w+)\s*:\s*([^=\n]+?)(?:\s*=\s*([^\n]+))?\n", re.S)
    for m in pattern.finditer(class_body):
        kdoc = parse_kdoc_meta(m.group(1))
        name = m.group(2)
        typ = m.group(3).strip()
        if "Extension" in typ:
            continue
        row = {
            "name": name,
            "type": typ,
            "description": kdoc.get("description", ""),
            "default": kdoc.get("default", ""),
            "values": kdoc.get("values", ""),
            "required": kdoc.get("required", "false"),
        }
        results.append(row)
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

    client_enum_values = parse_enum_values(text, "ClientLibrary")
    server_enum_values = parse_enum_values(text, "ServerLibrary")

    by_name_root = {p["name"]: p for p in root_props}
    by_name_model = {p["name"]: p for p in model_props}
    by_name_client = {p["name"]: p for p in client_props}
    by_name_server = {p["name"]: p for p in server_props}

    rows: list[dict[str, str]] = []

    for n in ["enabled", "inputSpec", "outputDir"]:
        if n in by_name_root:
            rows.append(row(n, by_name_root[n]))

    for n in ["packageName", "serialization", "validation", "double2BigDecimal", "float2BigDecimal", "integer2Long"]:
        if n in by_name_model:
            rows.append(row(f"model.{n}", by_name_model[n]))

    for n in ["packageName", "basePathVar"]:
        if n in by_name_client:
            rows.append(row(f"client.{n}", by_name_client[n]))
    if "library" in by_name_client:
        c = dict(by_name_client["library"])
        c["values"] = ", ".join(client_enum_values)
        rows.append(row("client.library", c))

    for n in ["packageName", "basePathVar"]:
        if n in by_name_server:
            rows.append(row(f"server.{n}", by_name_server[n]))
    if "library" in by_name_server:
        s = dict(by_name_server["library"])
        s["values"] = ", ".join(server_enum_values)
        rows.append(row("server.library", s))
    if "swagger" in by_name_server:
        rows.append(row("server.swagger", by_name_server["swagger"]))

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
    return v.replace("|", "\\|")


def update_readme(rows: list[dict[str, str]]) -> None:
    table_lines = [
        "| Property | Description | Values | Required | Default |",
        "|---|---|---|---|---|",
    ]
    for r in rows:
        table_lines.append(
            "| `{}` | {} | {} | {} | {} |".format(
                table_escape(r["property"]),
                table_escape(r["description"] or "-"),
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
