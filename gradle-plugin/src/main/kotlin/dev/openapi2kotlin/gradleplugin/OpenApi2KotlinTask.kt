package dev.openapi2kotlin.gradleplugin

import dev.openapi2kotlin.OpenApi2KotlinApp
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path

abstract class OpenApi2KotlinTask(
) : DefaultTask() {

    @TaskAction
    fun generate() {
        val ext = project.extensions.getByType(OpenApi2KotlinExtension::class.java)

        val defaultConfig = OpenApi2KotlinUseCase.Config(
            inputSpecPath = resolveInputSpecPath(ext),
            outputDirPath = resolveOutputSpecPath(ext),
        )

        val modelConfig = defaultConfig.model.copy(
            packageName = ext.model.packageName ?: defaultConfig.model.packageName,
        )

        val clientConfig = defaultConfig.client.copy(
            enabled     = ext.client.enabled     ?: defaultConfig.client.enabled,
            packageName = ext.client.packageName ?: defaultConfig.client.packageName,
        )

        val serverConfig = defaultConfig.server.copy(
            enabled     = ext.server.enabled     ?: defaultConfig.server.enabled,
            packageName = ext.server.packageName ?: defaultConfig.server.packageName,
            framework   = ext.server.framework?.let { OpenApi2KotlinUseCase.ServerConfig.Framework.fromValue(it) } ?: defaultConfig.server.framework,
        )

        val config = defaultConfig.copy(
            model = modelConfig,
            client = clientConfig,
            server = serverConfig,
        )

        clearOutput(config)

        OpenApi2KotlinApp.openApi2kotlin(config)
    }

    private fun resolveInputSpecPath(ext: OpenApi2KotlinExtension): Path {
        val inputSpecStr = ext.inputSpec
            ?.takeIf { it.isNotBlank() }
            ?: throw GradleException(
                "openapi2kotlin: inputSpec is required, e.g.\n" +
                        "openapi2kotlin {\n" +
                        "    inputSpec = \"${'$'}projectDir/src/main/resources/openapi.yaml\"\n" +
                        "}"
            )
        val inputSpecFile = project.file(inputSpecStr)
        if (!inputSpecFile.exists()) {
            throw GradleException("openapi2kotlin: inputSpec does not exist: ${inputSpecFile.absolutePath}")
        }
        return inputSpecFile.toPath()
    }

    private fun resolveOutputSpecPath(ext: OpenApi2KotlinExtension): Path {
        val outputDirString = ext.outputDir
            ?.takeIf { it.isNotBlank() }
            ?: throw GradleException(
                "openapi2kotlin: outputDir is required, e.g.\n" +
                        "openapi2kotlin {\n" +
                        "    outputDir = \"layout.buildDirectory.dir(\"generated/openapi/src/main/kotlin\").get().asFile.path\"\n" +
                        "}"
            )

        val outputDirFile = project.file(outputDirString)
        val outputDirPath: Path = outputDirFile.toPath()

        if (!Files.exists(outputDirPath)) {
            Files.createDirectories(outputDirPath)
        }

        return outputDirPath
    }

    private fun clearOutput(config: OpenApi2KotlinUseCase.Config) {
        val outputDirPath = config.outputDirPath

        if (!Files.exists(outputDirPath)) {
            return
        }

        clearPackageDir(outputDirPath, config.model.packageName)

        if (config.client.enabled) {
            clearPackageDir(outputDirPath, config.client.packageName)
        }
        if (config.server.enabled) {
            clearPackageDir(outputDirPath, config.server.packageName)
        }
    }

    private fun clearPackageDir(outputDirPath: Path, packageName: String) {
        val packageDir = outputDirPath.resolve(packageName.replace('.', '/'))
        if (Files.exists(packageDir)) {
            packageDir.toFile().deleteRecursively()
        }
    }
}