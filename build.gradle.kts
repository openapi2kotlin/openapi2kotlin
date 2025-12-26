import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    idea
}

subprojects {
    apply(plugin = "idea")

    repositories {
        mavenCentral()
    }

    /**
     * IMPORTANT:
     * Gradle plugins run inside the Gradle JVM.
     * CI/CD platforms (e.g., Railway) often run Gradle on Java 21.
     *
     * Therefore, the Gradle plugin module MUST be compiled to Java 21 bytecode.
     */
    plugins.withType<JavaPlugin> {
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
        tasks.withType(JavaCompile::class.java).configureEach {
            options.release.set(21)
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
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
        tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
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
