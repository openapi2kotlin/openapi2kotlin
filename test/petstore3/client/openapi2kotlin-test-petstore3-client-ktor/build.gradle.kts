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
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.openapi2kotlin)
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
    implementation(libs.bundles.ktor)
    testImplementation(libs.bundles.test)
}

openapi2kotlin {
    inputSpec = "$projectDir/../../openapi.yaml"
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "e2e.petstore3.client.ktor.generated.model"
        serialization = KotlinX
    }

    client {
        packageName = "e2e.petstore3.client.ktor.generated.client"
        library = Ktor
    }
}
