import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.attributes.Bundling
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

val javaVersion = providers.gradleProperty("openapi2kotlin.jvm").get().toInt()
val kotlinJvmTarget = JvmTarget.fromTarget(javaVersion.toString())
val detektJvmTarget = minOf(javaVersion, 22)
val ktlintCliVersion = "1.0.1"

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    idea
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
    config.setFrom(rootProject.file("detekt.yml"))
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

        if (project.path != ":tools:detekt-tools") {
            dependencies.add("detektPlugins", project(":tools:detekt-tools"))
        }

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

        tasks.matching { task ->
            task.name.startsWith("runKtlint") &&
                task.name != "loadKtlintReporters"
        }.configureEach {
            dependsOn("loadKtlintReporters")
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

val demoKtlintCli by configurations.creating
val demoDetektCli by configurations.creating
val demoDetektPlugins by configurations.creating

demoKtlintCli.attributes {
    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named("shadowed"))
}

dependencies {
    demoKtlintCli("com.pinterest.ktlint:ktlint-cli:$ktlintCliVersion")
    demoDetektCli("io.gitlab.arturbosch.detekt:detekt-cli:${libs.versions.detekt.get()}")
    demoDetektPlugins(project(":tools:detekt-tools"))
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

fun standaloneLintSources(path: String): List<File> =
    listOf(
        rootDir.resolve(path).resolve("src/main/kotlin"),
        rootDir.resolve(path).resolve("src/test/kotlin"),
        rootDir.resolve(path).resolve("build.gradle.kts"),
        rootDir.resolve(path).resolve("settings.gradle.kts"),
    ).filter(File::exists)

fun repoLintSources(): List<File> =
    buildList {
        add(rootDir.resolve("build.gradle.kts"))
        add(rootDir.resolve("settings.gradle.kts"))
        subprojects.forEach { project ->
            add(project.projectDir.resolve("src/main/kotlin"))
            add(project.projectDir.resolve("src/test/kotlin"))
            add(project.projectDir.resolve("build.gradle.kts"))
        }
    }.filter(File::exists)

fun repoDetektInputs(): List<File> =
    buildList {
        add(rootDir.resolve("build.gradle.kts"))
        add(rootDir.resolve("settings.gradle.kts"))
        subprojects.forEach { project ->
            add(project.projectDir)
        }
    }.filter(File::exists)

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

val lintStandaloneDemoProjects =
    standaloneDemoProjects.flatMap { path ->
        val suffix = path.toStandaloneTaskSuffix()
        val ktlintTask =
            tasks.register<JavaExec>("ktlint$suffix") {
                group = "verification"
                description = "Runs ktlintCheck for standalone demo project $path."
                classpath = demoKtlintCli
                mainClass.set("com.pinterest.ktlint.Main")
                args("--editorconfig=${rootDir.resolve(".editorconfig").invariantSeparatorsPath}")
                args(standaloneLintSources(path).map(File::invariantSeparatorsPath))
            }
        val detektTask =
            tasks.register<JavaExec>("detekt$suffix") {
                group = "verification"
                description = "Runs detekt for standalone demo project $path."
                classpath = demoDetektCli + demoDetektPlugins
                mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
                args(
                    "--build-upon-default-config",
                    "--config",
                    rootDir.resolve("detekt.yml").invariantSeparatorsPath,
                    "--input",
                    rootDir.resolve(path).invariantSeparatorsPath,
                    "--includes",
                    "**/*.kt,**/*.kts",
                    "--excludes",
                    "**/build/**,**/generated/**",
                    "--jvm-target",
                    detektJvmTarget.toString(),
                )
            }
        val lintTask =
            tasks.register("lint$suffix") {
                group = "verification"
                description = "Runs ktlintCheck and detekt for standalone demo project $path."
                dependsOn(ktlintTask, detektTask)
            }

        listOf(ktlintTask, detektTask, lintTask)
    }

val lintStandaloneTestProjects =
    standaloneTestProjects.flatMap { path ->
        val suffix = path.toStandaloneTaskSuffix()
        val ktlintTask =
            tasks.register<JavaExec>("ktlint$suffix") {
                group = "verification"
                description = "Runs ktlintCheck for standalone test project $path."
                classpath = demoKtlintCli
                mainClass.set("com.pinterest.ktlint.Main")
                args("--editorconfig=${rootDir.resolve(".editorconfig").invariantSeparatorsPath}")
                args(standaloneLintSources(path).map(File::invariantSeparatorsPath))
            }
        val detektTask =
            tasks.register<JavaExec>("detekt$suffix") {
                group = "verification"
                description = "Runs detekt for standalone test project $path."
                classpath = demoDetektCli + demoDetektPlugins
                mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
                args(
                    "--build-upon-default-config",
                    "--config",
                    rootDir.resolve("detekt.yml").invariantSeparatorsPath,
                    "--input",
                    rootDir.resolve(path).invariantSeparatorsPath,
                    "--includes",
                    "**/*.kt,**/*.kts",
                    "--excludes",
                    "**/build/**,**/generated/**",
                    "--jvm-target",
                    detektJvmTarget.toString(),
                )
            }
        val lintTask =
            tasks.register("lint$suffix") {
                group = "verification"
                description = "Runs ktlintCheck and detekt for standalone test project $path."
                dependsOn(ktlintTask, detektTask)
            }

        listOf(ktlintTask, detektTask, lintTask)
    }

val formatStandaloneDemoProjects =
    standaloneDemoProjects.map { path ->
        val suffix = path.toStandaloneTaskSuffix()
        tasks.register<JavaExec>("format$suffix") {
            group = "formatting"
            description = "Runs ktlintFormat for standalone demo project $path."
            classpath = demoKtlintCli
            mainClass.set("com.pinterest.ktlint.Main")
            args(
                "--editorconfig=${rootDir.resolve(".editorconfig").invariantSeparatorsPath}",
                "--format",
            )
            args(standaloneLintSources(path).map(File::invariantSeparatorsPath))
        }
    }

val formatStandaloneTestProjects =
    standaloneTestProjects.map { path ->
        tasks.register<Exec>("format${path.toStandaloneTaskSuffix()}") {
            group = "formatting"
            description = "Runs ktlintFormat for standalone project $path."
            workingDir = rootDir
            commandLine(gradleCommand, "--no-daemon", "-p", path, "ktlintFormat")
        }
    }

val ktlintRepoSources =
    tasks.register<JavaExec>("ktlintRepoSources") {
        group = "verification"
        description = "Runs ktlintCheck once over root repo Kotlin sources and Gradle scripts."
        classpath = demoKtlintCli
        mainClass.set("com.pinterest.ktlint.Main")
        args("--editorconfig=${rootDir.resolve(".editorconfig").invariantSeparatorsPath}")
        args(repoLintSources().map(File::invariantSeparatorsPath))
    }

val detektRepoSources =
    tasks.register<JavaExec>("detektRepoSources") {
        group = "verification"
        description = "Runs detekt once over root repo Kotlin sources and Gradle scripts."
        classpath = demoDetektCli + demoDetektPlugins
        mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
        args(
            "--build-upon-default-config",
            "--config",
            rootDir.resolve("detekt.yml").invariantSeparatorsPath,
            "--input",
            repoDetektInputs().joinToString(",") { it.invariantSeparatorsPath },
            "--includes",
            "**/*.kt,**/*.kts",
            "--excludes",
            "**/build/**,**/generated/**",
            "--jvm-target",
            detektJvmTarget.toString(),
        )
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
    dependsOn(lintStandaloneDemoProjects, lintStandaloneTestProjects)
}

tasks.register("formatStandaloneProjects") {
    group = "formatting"
    description = "Runs ktlintFormat for all standalone demo and test projects."
    dependsOn(formatStandaloneDemoProjects, formatStandaloneTestProjects)
}

tasks.named("ktlintFormat") {
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
        ktlintRepoSources,
        detektRepoSources,
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
