package dev.openapi2kotlin.gradleplugin

import dev.openapi2kotlin.OpenApi2KotlinApp
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path

abstract class OpenApi2KotlinTask : DefaultTask() {

    @TaskAction
    fun generate() {
        val ext = project.extensions.getByType(OpenApi2KotlinExtension::class.java)

        val defaultConfig = OpenApi2KotlinUseCase.Config(
            inputSpecPath = resolveInputSpecPath(ext),
            outputDirPath = resolveOutputSpecPath(ext),
        )

        // Resolve server config first so we can apply conditional defaults to model annotations.
        val serverEnabled = ext.server.enabled ?: defaultConfig.server.enabled

        val serverConfig = defaultConfig.server.copy(
            enabled = serverEnabled,
            packageName = ext.server.packageName ?: defaultConfig.server.packageName,
            framework = ext.server.framework
                ?.let { OpenApi2KotlinUseCase.ServerConfig.Framework.fromValue(it) }
                ?: defaultConfig.server.framework,
        )

        // Conditional default for validation annotations:
        //  - explicit user setting wins
        //  - otherwise: enabled when generating server, disabled otherwise
        val effectiveValidationAnnotationsEnabled: Boolean =
            ext.model.annotations.validations.enabled ?: serverConfig.enabled

        // Conditional default for swagger annotations:
        //  - explicit user setting wins
        //  - otherwise: enabled when generating server, disabled otherwise
        val effectiveSwaggerAnnotationsEnabled: Boolean =
            ext.model.annotations.swagger.enabled ?: serverConfig.enabled

        val modelConfig = defaultConfig.model.copy(
            packageName = ext.model.packageName ?: defaultConfig.model.packageName,
            annotations = defaultConfig.model.annotations.copy(
                jackson = defaultConfig.model.annotations.jackson.copy(
                    enabled = ext.model.annotations.jackson.enabled
                        ?: defaultConfig.model.annotations.jackson.enabled,
                    jsonPropertyMapping = ext.model.annotations.jackson.jsonPropertyMapping
                        ?: defaultConfig.model.annotations.jackson.jsonPropertyMapping,
                    defaultDiscriminatorValue = ext.model.annotations.jackson.defaultDiscriminatorValue
                        ?: defaultConfig.model.annotations.jackson.defaultDiscriminatorValue,
                    strictDiscriminatorSerialization = ext.model.annotations.jackson.strictDiscriminatorSerialization
                        ?: defaultConfig.model.annotations.jackson.strictDiscriminatorSerialization,
                    jsonValue = ext.model.annotations.jackson.jsonValue
                        ?: defaultConfig.model.annotations.jackson.jsonValue,
                    jsonCreator = ext.model.annotations.jackson.jsonCreator
                        ?: defaultConfig.model.annotations.jackson.jsonCreator,
                ),
                validations = defaultConfig.model.annotations.validations.copy(
                    enabled = effectiveValidationAnnotationsEnabled,
                    namespace = ext.model.annotations.validations.namespace
                        ?.let {
                            OpenApi2KotlinUseCase.ModelConfig.AnnotationsConfig.ValidationAnnotationsConfig
                                .ValidationAnnotationsNamespace.fromValue(it)
                        }
                        ?: defaultConfig.model.annotations.validations.namespace,
                ),
                swagger = defaultConfig.model.annotations.swagger.copy(
                    enabled = effectiveSwaggerAnnotationsEnabled,
                ),
            ),
            mapping = defaultConfig.model.mapping.copy(
                double2BigDecimal = ext.model.mapping.double2BigDecimal
                    ?: defaultConfig.model.mapping.double2BigDecimal,
                float2BigDecimal = ext.model.mapping.float2BigDecimal
                    ?: defaultConfig.model.mapping.float2BigDecimal,
                integer2Long = ext.model.mapping.integer2Long
                    ?: defaultConfig.model.mapping.integer2Long,
            ),
        )

        val clientConfig = defaultConfig.client.copy(
            enabled = ext.client.enabled ?: defaultConfig.client.enabled,
            packageName = ext.client.packageName ?: defaultConfig.client.packageName,
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
                        "    outputDir = \"layout.buildDirectory.dir(\\\"generated/openapi/src/main/kotlin\\\").get().asFile.path\"\n" +
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

        if (!Files.exists(outputDirPath)) return

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