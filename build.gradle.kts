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
     * Toolchain standardization for all Java-bearing projects.
     * Kotlin compilation is configured separately below.
     */
    plugins.withType<JavaPlugin> {
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
        tasks.withType(JavaCompile::class.java).configureEach {
            options.release.set(24)  // FIXME: Kotlin doesn't support 25 just yet
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

    /**
     * Kotlin toolchain + target for all Kotlin JVM subprojects.
     */
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure(KotlinJvmProjectExtension::class.java) {
            jvmToolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
        tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_24)
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
