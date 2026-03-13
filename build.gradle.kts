import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.internal.os.OperatingSystem

val javaVersion = providers.gradleProperty("openapi2kotlin.jvm").get().toInt()
val kotlinJvmTarget = JvmTarget.fromTarget(javaVersion.toString())

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
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
        extensions.configure(KotlinJvmProjectExtension::class.java) {
            jvmToolchain {
                languageVersion.set(JavaLanguageVersion.of(javaVersion))
            }
        }
        tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions.jvmTarget.set(kotlinJvmTarget)
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
val tagVersion = gitTag
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
    }
        .files
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

val cleanBuildStandaloneDemos = standaloneDemoProjects.map { path ->
    tasks.register<Exec>("cleanBuild${path.toStandaloneTaskSuffix()}") {
        group = "verification"
        description = "Runs clean build for standalone project $path."
        workingDir = rootDir
        commandLine(gradleCommand, "--no-daemon", "-p", path, "clean", "build")
    }
}

val cleanBuildStandaloneTests = standaloneTestProjects.map { path ->
    tasks.register<Exec>("cleanBuild${path.toStandaloneTaskSuffix()}") {
        group = "verification"
        description = "Runs clean build for standalone project $path."
        workingDir = rootDir
        commandLine(gradleCommand, "--no-daemon", "-p", path, "clean", "build")
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
