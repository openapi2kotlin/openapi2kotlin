import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

val javaVersion = providers.gradleProperty("openapi2kotlin.jvm").get().toInt()
val kotlinJvmTarget = JvmTarget.fromTarget(javaVersion.toString())
val detektJvmTarget = minOf(javaVersion, 22)

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    idea
}

subprojects {
    apply(plugin = "idea")

    repositories {
        mavenCentral()
    }

    plugins.withType<JavaPlugin> {
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(javaVersion))
            }
        }
        tasks.withType(JavaCompile::class.java).configureEach {
            options.release.set(javaVersion)
        }

        /**
         * Consistent module JAR naming for local dev artifacts.
         * Publication coordinates for the plugin are configured in :gradle-plugin only.
         */
        val artifactBase = "openapi2kotlin-${project.name}"
        tasks.withType<Jar>().configureEach {
            archiveBaseName.set(artifactBase)
        }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "io.gitlab.arturbosch.detekt")

        extensions.configure(KotlinJvmProjectExtension::class.java) {
            jvmToolchain {
                languageVersion.set(JavaLanguageVersion.of(javaVersion))
            }
        }
        tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions.jvmTarget.set(kotlinJvmTarget)
        }

        extensions.configure(KtlintExtension::class.java) {
            filter {
                exclude("**/build/**")
                exclude("**/generated/**")
            }
        }

        extensions.configure(DetektExtension::class.java) {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(rootProject.file("detekt.yml"))
        }

        tasks.withType(Detekt::class.java).configureEach {
            setSource(files(project.projectDir))
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/generated/**")
            jvmTarget = detektJvmTarget.toString()
        }
    }
}

/**
 * CI release versioning:
 * If the build runs on a Git tag "vX.Y.Z", publish as "X.Y.Z".
 *
 * Local default remains whatever is in gradle.properties (e.g. 999.999.999-SNAPSHOT).
 */
val gitTag = System.getenv("GITHUB_REF_NAME")?.trim().orEmpty()
val tagVersion =
    gitTag
        .takeIf { it.startsWith("v") && it.length > 1 }
        ?.removePrefix("v")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

if (tagVersion != null) {
    allprojects {
        version = tagVersion
    }
}

fun standaloneProjects(rootDirName: String): List<String> =
    fileTree(rootDir.resolve(rootDirName)) {
        include("**/settings.gradle.kts")
    }.files
        .map { it.parentFile.relativeTo(rootDir).invariantSeparatorsPath }
        .sorted()

val standaloneDemoProjects = standaloneProjects("demo")
val standaloneTestProjects = standaloneProjects("test")

fun String.toStandaloneTaskSuffix(): String =
    replace(Regex("[^A-Za-z0-9]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString("") { segment -> segment.replaceFirstChar(Char::uppercaseChar) }

val gradleCommand = if (OperatingSystem.current().isWindows) "gradlew.bat" else "./gradlew"

val cleanBuildStandaloneDemos =
    standaloneDemoProjects.map { path ->
        tasks.register<Exec>("cleanBuild${path.toStandaloneTaskSuffix()}") {
            group = "verification"
            description = "Runs clean build for standalone project $path."
            workingDir = rootDir
            commandLine(gradleCommand, "--no-daemon", "-p", path, "clean", "build")
        }
    }

val cleanBuildStandaloneTests =
    standaloneTestProjects.map { path ->
        tasks.register<Exec>("cleanBuild${path.toStandaloneTaskSuffix()}") {
            group = "verification"
            description = "Runs clean build for standalone project $path."
            workingDir = rootDir
            commandLine(gradleCommand, "--no-daemon", "-p", path, "clean", "build")
        }
    }

val lintStandaloneProjects =
    (standaloneDemoProjects + standaloneTestProjects).map { path ->
        tasks.register<Exec>("lint${path.toStandaloneTaskSuffix()}") {
            group = "verification"
            description = "Runs ktlintCheck and detekt for standalone project $path."
            workingDir = rootDir
            commandLine(gradleCommand, "--no-daemon", "-p", path, "ktlintCheck", "detekt")
        }
    }

val formatStandaloneProjects =
    (standaloneDemoProjects + standaloneTestProjects).map { path ->
        tasks.register<Exec>("format${path.toStandaloneTaskSuffix()}") {
            group = "formatting"
            description = "Runs ktlintFormat for standalone project $path."
            workingDir = rootDir
            commandLine(gradleCommand, "--no-daemon", "-p", path, "ktlintFormat")
        }
    }

tasks.named("build") {
    mustRunAfter(tasks.named("clean"))
}

tasks.register("cleanBuildStandaloneDemos") {
    group = "verification"
    description = "Runs clean build for all standalone demo projects."
    dependsOn(cleanBuildStandaloneDemos)
}

tasks.register("cleanBuildStandaloneTests") {
    group = "verification"
    description = "Runs clean build for all standalone test projects."
    dependsOn(cleanBuildStandaloneTests)
}

tasks.register("cleanBuildStandaloneProjects") {
    group = "verification"
    description = "Runs clean build for all standalone demo and test projects."
    dependsOn("cleanBuildStandaloneDemos", "cleanBuildStandaloneTests")
}

tasks.register("lintStandaloneProjects") {
    group = "verification"
    description = "Runs ktlintCheck and detekt for all standalone demo and test projects."
    dependsOn(lintStandaloneProjects)
}

tasks.register("formatStandaloneProjects") {
    group = "formatting"
    description = "Runs ktlintFormat for all standalone demo and test projects."
    dependsOn(formatStandaloneProjects)
}

tasks.register("ktlintFormat") {
    group = "formatting"
    description = "Runs ktlintFormat for all Kotlin subprojects and standalone demo/test projects."
    dependsOn(
        subprojects.mapNotNull { project ->
            project.plugins.findPlugin("org.jetbrains.kotlin.jvm")?.let {
                project.tasks.named("ktlintFormat")
            }
        },
        tasks.named("formatStandaloneProjects"),
    )
}

tasks.register("preCommit") {
    group = "verification"
    description = "Runs repo linting for root and standalone projects."
    dependsOn(
        subprojects
            .mapNotNull { project ->
                project.plugins.findPlugin("org.jetbrains.kotlin.jvm")?.let {
                    listOf(
                        project.tasks.named("ktlintCheck"),
                        project.tasks.named("detekt"),
                    )
                }
            }.flatten(),
        tasks.named("lintStandaloneProjects"),
    )
}

tasks.register("prePush") {
    group = "verification"
    description = "Runs the full root and standalone verification gate."
    dependsOn("cleanBuildAll")
}

tasks.register("cleanBuildAll") {
    group = "verification"
    description = "Runs a clean build for the root project and all standalone demo and test projects."
    dependsOn("clean", "build", "cleanBuildStandaloneProjects")
}

tasks.register("all") {
    group = "verification"
    description = "Runs all standalone demo and test clean builds. Intended to be used with `clean build all`."
    dependsOn("cleanBuildStandaloneProjects")
}

tasks.register<Copy>("installGitHooks") {
    group = "build setup"
    description = "Installs repository git hooks into .git/hooks."
    from(rootProject.file(".githooks"))
    into(rootProject.file(".git/hooks"))
    doLast {
        rootProject
            .file(".git/hooks")
            .listFiles()
            ?.filter { it.isFile }
            ?.forEach { hookFile ->
                hookFile.setExecutable(true)
            }
    }
}
