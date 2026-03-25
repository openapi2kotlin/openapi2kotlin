import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

fun readRepoRoot(): File =
    generateSequence(projectDir) { it.parentFile }
        .firstOrNull { it.resolve("AGENTS.md").isFile }
        ?: error("Could not locate repository root from $projectDir")

val repoRoot = readRepoRoot()
val repoJvmVersion = 25
val detektJvmTarget = minOf(repoJvmVersion, 22)

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi2kotlin)
}

kotlin {
    jvmToolchain(25)
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

tasks.withType<org.gradle.api.tasks.SourceTask>().configureEach {
    if (name.contains("ktlint", ignoreCase = true)) {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}

tasks.withType<KtLintCheckTask>().configureEach {
    setSource(files("src/main/kotlin", "src/test/kotlin", "build.gradle.kts", "settings.gradle.kts"))
}
tasks.withType<KtLintFormatTask>().configureEach {
    setSource(files("src/main/kotlin", "src/test/kotlin", "build.gradle.kts", "settings.gradle.kts"))
}
tasks.withType<Detekt>().configureEach {
    setSource(files(projectDir))
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**", "**/generated/**")
    jvmTarget = detektJvmTarget.toString()
}

dependencies {
    implementation(libs.bundles.restclient)
    testImplementation(libs.bundles.test)
}

tasks.test {
    useJUnitPlatform()
}

openapi2kotlin {
    inputSpec = "$projectDir/src/main/resources/openapi.yaml"
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "dev.openapi2kotlin.demo.model"
    }

    client {
        packageName = "dev.openapi2kotlin.demo.client"
        library = RestClient
    }
}

tasks.matching { it.name == "runKtlintFormatOverMainSourceSet" }.configureEach {
    dependsOn("openapi2kotlin")
}

tasks.matching { it.name == "runKtlintCheckOverMainSourceSet" }.configureEach {
    dependsOn("runKtlintFormatOverMainSourceSet")
}
