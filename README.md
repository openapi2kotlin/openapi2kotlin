# openapi2kotlin

[![Maven Central](https://img.shields.io/maven-central/v/dev.openapi2kotlin/openapi2kotlin)](https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin)
[![Website](https://img.shields.io/badge/website-openapi2kotlin.dev-0b0b0b)](https://openapi2kotlin.dev/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

#### OpenAPI 👉 Kotlin | [openapi2kotlin.dev](https://openapi2kotlin.dev/)

Gradle plugin for generating Kotlin sources from an OpenAPI specification, engineered to handle complex polymorphism including `anyOf` and `allOf`.

---

## Installation & Usage

The plugin is published to [Maven Central](https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin).

### `libs.versions.toml`

```toml
[versions]
openapi2kotlin = "0.10.0"

[plugins]
openapi2kotlin = { id = "dev.openapi2kotlin", version.ref = "openapi2kotlin" }
```

### `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.openapi2kotlin)
}

openapi2kotlin {
    inputSpec = "$projectDir/src/main/resources/openapi.yaml"
    outputDir = layout.buildDirectory
        .dir("generated/src/main/kotlin")
        .get()
        .asFile
        .path

    model {
        packageName = "dev.openapi2kotlin.model"
    }
}
```

### Configuration options

| Property                      | Description | Example |
|-------------------------------|-------------|---------|
| `inputSpec = `                | Path to OpenAPI YAML or JSON specification |  "$projectDir/src/main/resources/openapi.yaml"       |
| `outputDir = `                | Root directory for generated Kotlin sources |  layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path       |
| `model { packageName = }`     | Package name for generated model classes |     "dev.openapi2kotlin.model"    |

### Requirements

- Gradle 8+
- Kotlin JVM project
- OpenAPI 3.x specification (YAML or JSON)
- Java 21+

---

## Generated sources & IDE integration

The plugin **automatically registers the configured `outputDir` as a Kotlin sources root**.

This means:
- Generated code is picked up by Gradle without extra configuration
- IntelliJ IDEA / Android Studio indexes the generated sources automatically
- No manual `sourceSets` configuration is required

---

## Maven Central

Artifact coordinates:

```
groupId: dev.openapi2kotlin
artifactId: openapi2kotlin
```

https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin

---

## Links

- Website: https://openapi2kotlin.dev/
- Maven Central: https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.