import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi2kotlin)
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(repoJvmVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.bundles.spring)
    testImplementation(libs.bundles.test)
}

openapi2kotlin {
    inputSpec = "$projectDir/../../openapi.yaml"
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "e2e.server.spring.generated.model"
    }

    server {
        packageName = "e2e.server.spring.generated.server"
        library = Spring
    }
}
