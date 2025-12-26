import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`

    // Fat-jar creation (used only for implementation artifact)
    alias(libs.plugins.gradleup.shadow)

    // Maven Central publishing, signing, metadata
    alias(libs.plugins.vanniktech.publish)
}

/**
 * Published coordinates:
 *
 *   dev.openapi2kotlin:openapi2kotlin:<version>
 *
 * Plugin marker remains:
 *
 *   dev.openapi2kotlin:dev.openapi2kotlin.gradle.plugin:<version>
 */
base {
    archivesName.set("openapi2kotlin")
}

/**
 * Configuration used ONLY to embed internal implementation modules
 * into the shaded plugin JAR.
 *
 * This configuration MUST NOT leak into the published POM.
 */
val embed by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true

    // Pull in anything the plugin module itself needs at runtime (Gradle/Kotlin DSL, etc.)
    extendsFrom(configurations.getByName("runtimeClasspath"))
}

dependencies {
    /**
     * Compile against configuration, but do not publish it as a dependency.
     * This keeps the published POM free of dev.openapi2kotlin:configuration.
     */
    compileOnly(project(":configuration"))

    /**
     * Resolve configuration (and transitives) only for embedding into the fat jar.
     */
    embed(project(":configuration"))
}

/**
 * Gradle plugin definition.
 *
 * Marker artifact is generated automatically by java-gradle-plugin
 * and MUST NOT be shaded or modified.
 */
gradlePlugin {
    plugins {
        create("openApi2KotlinPlugin") {
            id = "dev.openapi2kotlin"
            implementationClass = "dev.openapi2kotlin.gradleplugin.OpenApi2KotlinPlugin"
            displayName = "openapi2kotlin"
            description = "OpenAPI → Kotlin generator."
        }
    }
}

/**
 * Build the fat jar with Shadow as an intermediate artifact.
 *
 * IMPORTANT:
 * - shadowJar must NOT produce the same filename as jar, otherwise jar will try to unzip a file
 *   that is being produced/overwritten and fails with "Cannot expand ZIP".
 */
tasks.named<ShadowJar>("shadowJar") {
    // Intermediate file: openapi2kotlin-<ver>-shadow.jar
    archiveClassifier.set("shadow")

    // Ensure module naming is stable and does not inherit root's "openapi2kotlin-${project.name}"
    archiveBaseName.set("openapi2kotlin")

    configurations = listOf(embed)
    mergeServiceFiles()
}

/**
 * Primary published jar:
 * - openapi2kotlin-<ver>.jar
 *
 * We repackage the shadow output into the main jar so Gradle metadata stays consistent
 * and consumers get a single runtime jar.
 */
tasks.named<Jar>("jar") {
    // Ensure module naming is stable and does not inherit root's "openapi2kotlin-${project.name}"
    archiveBaseName.set("openapi2kotlin")

    dependsOn(tasks.named("shadowJar"))

    val shadowJarFile = tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile }

    from(zipTree(shadowJarFile))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/**
 * Central requires a -sources.jar.
 */
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

/**
 * Central requires a -javadoc.jar.
 */
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(layout.projectDirectory.file("README.md"))
}

/**
 * kotlin-dsl + publishing plugins may introduce an `emptySourcesJar` fallback.
 * If it gets attached alongside a real sources jar, publishing fails with:
 * "multiple artifacts with the identical extension and classifier ('jar', 'sources')".
 */
tasks.matching { it.name == "emptySourcesJar" }.configureEach {
    enabled = false
}

/**
 * Maven Central validates ALL publications, including the Gradle plugin marker publication.
 * Therefore, POM metadata must be present on every MavenPublication.
 */
publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("openapi2kotlin")
            description.set("OpenAPI → Kotlin generator.")
            url.set("https://openapi2kotlin.dev")

            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("martinbarnas")
                    name.set("Martin Barnas")
                    url.set("https://github.com/m-barnas")
                }
            }

            scm {
                url.set("https://github.com/openapi2kotlin/openapi2kotlin")
                connection.set("scm:git:https://github.com/openapi2kotlin/openapi2kotlin.git")
                developerConnection.set("scm:git:https://github.com/openapi2kotlin/openapi2kotlin.git")
            }
        }
    }
}

/**
 * Ensure the implementation publication:
 * - publishes ONLY the main jar + sources + javadoc (no -shadow.jar),
 * - does not publish internal dependencies,
 * - keeps marker publications intact.
 */
afterEvaluate {
    publishing {
        publications.withType<MavenPublication>().configureEach {
            // Marker publications contain "PluginMarker" in name; leave their artifacts intact.
            if (name.contains("PluginMarker", ignoreCase = true)) return@configureEach

            // Implementation publication: enforce artifactId.
            artifactId = "openapi2kotlin"

            // Remove any "sources" artifacts already attached by other plugins.
            // This must happen after evaluation, otherwise late wiring re-adds duplicates.
            artifacts
                .filter { it.extension == "jar" && it.classifier == "sources" }
                .toList()
                .forEach { artifacts.remove(it) }

            // Remove any "javadoc" artifacts already attached by other plugins (defensive).
            artifacts
                .filter { it.extension == "jar" && it.classifier == "javadoc" }
                .toList()
                .forEach { artifacts.remove(it) }

            // Remove any shadow artifacts; shadowJar is an internal intermediate only.
            artifacts
                .filter { it.extension == "jar" && it.classifier == "shadow" }
                .toList()
                .forEach { artifacts.remove(it) }

            // Attach exactly one sources jar and one javadoc jar.
            artifact(tasks.named<Jar>("sourcesJar"))
            artifact(javadocJar)

            // Keep POM clean (no leaked internal module deps).
            pom.withXml {
                val root = asNode()
                root.children()
                    .filterIsInstance<groovy.util.Node>()
                    .filter { it.name().toString() == "dependencies" }
                    .forEach { root.remove(it) }
            }
        }
    }
}

mavenPublishing {
    // We provide our own sourcesJar/javadocJar tasks to avoid collisions with kotlin-dsl.
    configureBasedOnAppliedPlugins(
        sourcesJar = false,
        javadocJar = false
    )
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}

/**
 * Gradle Module Metadata (.module) is not required by Maven Central.
 *
 * We intentionally replace the artifact set published from the `java` component (fat-jar repack),
 * which makes Gradle's module-metadata validation fail (it expects the original component artifacts).
 *
 * Disabling module metadata keeps publishing stable and Maven Central-compliant:
 * POM + main jar + sources jar + javadoc jar (+ signatures).
 */
tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}