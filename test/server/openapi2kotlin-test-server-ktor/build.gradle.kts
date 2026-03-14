import java.util.Properties

fun readRepoJvmVersion(): Int =
    generateSequence(projectDir) { it.parentFile }
        .map { it.resolve("gradle.properties") }
        .filter { it.isFile }
        .mapNotNull { propertiesFile ->
            Properties().apply {
                propertiesFile.inputStream().use(::load)
            }.getProperty("openapi2kotlin.jvm")?.toInt()
        }
        .firstOrNull()
        ?: error("Could not locate openapi2kotlin.jvm in repository gradle.properties from $projectDir")

val repoJvmVersion = readRepoJvmVersion()

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.openapi2kotlin)
}

kotlin {
    jvmToolchain(repoJvmVersion)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:${libs.versions.ktor.get()}"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jsr310)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.ktor.server.test.host)
}

openapi2kotlin {
    inputSpec = "$projectDir/src/main/resources/openapi.yaml"
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "e2e.server.ktor.generated.model"
        serialization = Jackson
    }

    server {
        packageName = "e2e.server.ktor.generated.server"
        library = Ktor
    }
}
