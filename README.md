<p align="center">
  <a href="https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin">
    <img src="https://img.shields.io/maven-central/v/dev.openapi2kotlin/openapi2kotlin" alt="Maven Central">
  </a>
  <a href="https://openapi2kotlin.dev/">
    <img src="https://img.shields.io/badge/website-openapi2kotlin.dev-0b0b0b" alt="Website">
  </a>
  <a href="https://www.apache.org/licenses/LICENSE-2.0">
    <img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License">
  </a>
</p>

<br/>

<p align="center">
  <img src="site/public/logo.svg" alt="openapi2kotlin logo" width="160">
</p>

<h3 align="center" style="font-size: 36px">openapi2kotlin</h3>

<h4 align="center">
  OpenAPI 👉 Kotlin |
  <a href="https://openapi2kotlin.dev/">openapi2kotlin.dev</a>
</h4>

<br/>

Gradle plugin for generating Kotlin sources from an OpenAPI specification, engineered to handle complex polymorphism including `anyOf` and `allOf`.

<br/>

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