<p align="center">
  <a href="https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin" target="_blank">
    <img src="https://img.shields.io/maven-central/v/dev.openapi2kotlin/openapi2kotlin" alt="Maven Central">
  </a>
  <a href="https://openapi2kotlin.dev/" target="_blank">
    <img src="https://img.shields.io/badge/website-openapi2kotlin.dev-0b0b0b" alt="Website">
  </a>
  <a href="https://www.apache.org/licenses/LICENSE-2.0" target="_blank">
    <img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License">
  </a>
</p>

<br/>

<p align="center">
  <img src="site/public/logo.svg" alt="openapi2kotlin logo" width="80">
</p>

<p align="center">
  <img src="site/public/hero.svg" alt="hero logo" width="1200">
</p>

<br/>

<h4 align="center">
  OpenAPI 👉 Kotlin |
  <a href="https://openapi2kotlin.dev/">openapi2kotlin.dev</a>
</h4>

<br/>
<h1 >openapi2kotlin</h1>

Gradle plugin for generating Kotlin sources from an OpenAPI specification, engineered to handle complex polymorphism including `oneOf`, `allOf` and `discriminator`.

<br/>

## Installation & Usage

The plugin is published to [Maven Central](https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin).

### `libs.versions.toml`

```toml
[versions]
openapi2kotlin = "0.16.0"

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
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "dev.openapi2kotlin.model"
    }

    client {
        packageName = "dev.openapi2kotlin.client"
        library = Ktor
    }
}
```

## API Reference

For the complete, versioned documentation, see [openapi2kotlin.dev API Reference](https://openapi2kotlin.dev/#api-reference).

### Configuration options

| Property | Description | Values | Required | Default |
|---|---|---|---|---|
| `inputSpec` | Path to OpenAPI YAML or JSON specification, e.g. "$projectDir/src/main/resources/openapi.yaml". | - | true | - |
| `outputDir` | Root directory for generated Kotlin sources, e.g. layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path. | - | true | - |
| `enabled` | Enables or disables code generation for the current Gradle run. | true, false | false | true |
| `model.packageName` | Package name for generated model classes. | - | false | "dev.openapi2kotlin.model" |
| `model.serialization` | Serialization annotation family for generated model classes. | KotlinX, Jackson | false | Ktor -> KotlinX, Server Spring -> Jackson, Client RestClient -> Jackson |
| `model.validation` | Validation annotations namespace used in generated models. | None, Jakarta, JavaX | false | None |
| `model.double2BigDecimal` | Maps OpenAPI number/double to BigDecimal instead of Double. | true, false | false | false |
| `model.float2BigDecimal` | Maps OpenAPI number/float to BigDecimal instead of Float. | true, false | false | false |
| `model.integer2Long` | Maps OpenAPI integer to Long instead of Int. | true, false | false | true |
| `client.packageName` | Base package for generated API classes. | - | false | "dev.openapi2kotlin.client" |
| `client.library` | Target HTTP client library used by generated client API. | Ktor, RestClient | true | - |
| `client.basePathVar` | Uses the first OpenAPI server variable matching `basePathVar` when it has a default value<br><br>e.g.<br><code>servers:</code><br>&nbsp;&nbsp;<code>- url: &#x27;/{basePath}&#x27;</code><br>&nbsp;&nbsp;&nbsp;&nbsp;<code>variables:</code><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>basePath:</code><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>default: &#x27;v5/&#x27;</code> | - | false | "basePath" |
| `client.methodNameSingularized` | Singularizes method names for single-resource endpoints, e.g. `retrieveQuote`. | true, false | false | true |
| `client.methodNamePluralized` | Pluralizes method names for collection endpoints, e.g. `listQuotes`. | true, false | false | true |
| `client.methodNameFromOperationId` | Derives method names from OpenAPI `operationId` instead of URL path. | true, false | false | false |
| `server.packageName` | Base package for generated API classes. | - | false | "dev.openapi2kotlin.server" |
| `server.library` | Target server framework used by generated server API. | Ktor, Spring | true | - |
| `server.swagger` | Enables generated Swagger/OpenAPI annotations. | true, false | false | Ktor -> false, Spring -> true |
| `server.basePathVar` | Uses the first OpenAPI server variable matching `basePathVar` when it has a default value<br><br>e.g.<br><code>servers:</code><br>&nbsp;&nbsp;<code>- url: &#x27;/{basePath}&#x27;</code><br>&nbsp;&nbsp;&nbsp;&nbsp;<code>variables:</code><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>basePath:</code><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>default: &#x27;v5/&#x27;</code> | - | false | "basePath" |
| `server.methodNameSingularized` | Singularizes method names for single-resource endpoints, e.g. `retrieveQuote`. | true, false | false | true |
| `server.methodNamePluralized` | Pluralizes method names for collection endpoints, e.g. `listQuotes`. | true, false | false | true |
| `server.methodNameFromOperationId` | Derives method names from OpenAPI `operationId` instead of URL path. | true, false | false | false |

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

## Links

- Website: https://openapi2kotlin.dev/
- Maven Central: https://central.sonatype.com/artifact/dev.openapi2kotlin/openapi2kotlin

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
