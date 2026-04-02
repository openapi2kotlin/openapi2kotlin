import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.tasks.SourceTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import java.util.Properties

fun readRepoRoot(): File =
    generateSequence(projectDir) { it.parentFile }
        .firstOrNull { it.resolve("AGENTS.md").isFile }
        ?: error("Could not locate repository root from $projectDir")

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

fun readRepoCoordinate(moduleName: String): String =
    generateSequence(projectDir) { it.parentFile }
        .map { it.resolve("gradle.properties") }
        .filter { it.isFile }
        .mapNotNull { propertiesFile ->
            Properties().apply {
                propertiesFile.inputStream().use(::load)
            }.run {
                val group = getProperty("group")
                val version = getProperty("version")
                if (group != null && version != null) {
                    "$group:$moduleName:$version"
                } else {
                    null
                }
            }
        }.firstOrNull()
        ?: error("Could not locate group/version in repository gradle.properties from $projectDir")

val repoRoot = readRepoRoot()
val repoJvmVersion = readRepoJvmVersion()
val detektJvmTarget = minOf(repoJvmVersion, 22)
val detektToolsCoordinate = readRepoCoordinate("detekt-tools")

plugins {
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi2kotlin)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(repoJvmVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

configure<KtlintExtension> {
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}

configure<DetektExtension> {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(repoRoot.resolve("detekt.yml"))
}

tasks
    .withType<SourceTask>()
    .matching { it.name.contains("ktlint", ignoreCase = true) }
    .configureEach {
        exclude("**/build/**")
        exclude("**/generated/**")
    }

tasks.withType<Detekt>().configureEach {
    setSource(files(projectDir))
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**", "**/generated/**")
    jvmTarget = detektJvmTarget.toString()
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    detektPlugins(detektToolsCoordinate)
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

tasks.matching { it.name == "runKtlintFormatOverMainSourceSet" }.configureEach {
    dependsOn("openapi2kotlin")
}

tasks.matching { it.name == "runKtlintCheckOverMainSourceSet" }.configureEach {
    dependsOn("runKtlintFormatOverMainSourceSet")
}

tasks.withType<KtLintCheckTask>().configureEach {
    setSource(files("src/main/kotlin", "src/test/kotlin", "build.gradle.kts", "settings.gradle.kts"))
}

tasks.withType<KtLintFormatTask>().configureEach {
    setSource(files("src/main/kotlin", "src/test/kotlin", "build.gradle.kts", "settings.gradle.kts"))
}
