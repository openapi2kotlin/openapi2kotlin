import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val javaVersion = 25
val kotlinJvmTarget = JvmTarget.fromTarget(javaVersion.toString())

plugins {
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
