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

    // ----- Java toolchain + release for any project that has the Java plugin -----
    plugins.withType<JavaPlugin> {
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
        tasks.withType(JavaCompile::class.java).configureEach {
            options.release.set(24) // FIXME: Kotlin doesn't support 25 just yet
        }

        val artifactBase = "openapi2kotlin-${project.name}"

        // Nice jar module names
        tasks.withType<Jar>().configureEach {
            archiveBaseName.set(artifactBase)
        }

        // ---- enable publishing for all Java/Kotlin projects, which my gradle plugin internally uses, so I don't need to define tis one by one ----
        plugins.apply("maven-publish")
        extensions.configure(PublishingExtension::class.java) {
            publications {
                if (names.isEmpty() || !names.contains("mavenJava")) {
                    create<MavenPublication>("mavenJava") {
                        from(components["java"])
                    }
                }

                // Nice jar module names
                withType<MavenPublication>().configureEach {
                    artifactId = artifactBase
                }
            }
            repositories {
                mavenLocal()
            }
        }
    }

    // ----- Kotlin JVM toolchain + jvmTarget for projects using kotlin-jvm -----
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure(KotlinJvmProjectExtension::class.java) {
            jvmToolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
        tasks.withType(KotlinCompile::class.java).configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_24) // FIXME: Kotlin doesn't support 25 just yet
        }
    }
}
