package dev.openapi2kotlin.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class OpenApi2KotlinPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // extension: openapi2kotlin { ... }
        val ext = project.extensions.create<OpenApi2KotlinExtension>("openapi2kotlin")

        // task: openapi2kotlin
        val task = project.tasks.register<OpenApi2KotlinTask>("openapi2kotlin") {
            group = "openapi2kotlin"
            description = "Generate Kotlin sources from the configured OpenAPI spec using openapi2kotlin."
        }

        // Ensure openapi2kotlin runs before compileKotlin
        project.tasks
            .matching { it.name == "compileKotlin" }
            .configureEach {
                dependsOn(task)
            }

        // Optionally register outputDir as a source root
        project.afterEvaluate {
            val sourceSets = extensions.findByType(SourceSetContainer::class.java) ?: return@afterEvaluate
            val main = sourceSets.getByName("main")

            if (ext.srcDirEnabled) {
                main.java.srcDir(ext.outputDir!!)
            }
        }
    }
}
