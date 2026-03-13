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

sourceSets {
    main {
        kotlin.srcDir(layout.buildDirectory.dir("generated/src/main/kotlin"))
    }
}

tasks.named("compileKotlin") {
    dependsOn("openapi2kotlin")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:${libs.versions.ktor.get()}"))
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jsr310)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.wiremock)
}

openapi2kotlin {
    inputSpec = "$projectDir/src/main/resources/openapi.yaml"
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "e2e.client.ktor.generated.model"
        serialization = Jackson
    }

    client {
        packageName = "e2e.client.ktor.generated.client"
        library = Ktor
    }
}
