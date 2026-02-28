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

        if (!ext.enabled) return

        val defaultConfig = OpenApi2KotlinUseCase.Config(
            inputSpecPath = resolveInputSpecPath(ext),
            outputDirPath = resolveOutputSpecPath(ext),
        )

        val apiConfig = resolveApiConfig(ext)
        val serverMode = apiConfig is OpenApi2KotlinUseCase.ApiConfig.Server

        // Conditional default for validation annotations:
        //  - explicit user setting wins
        //  - otherwise: enabled when generating server, disabled otherwise
        val effectiveValidationAnnotationsEnabled: Boolean =
            ext.model.annotations.validations.enabled ?: serverMode

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
                            OpenApi2KotlinUseCase.ModelConfig.ModelAnnotationsConfig.ValidationAnnotationsConfig
                                .ValidationAnnotationsNamespace.fromValue(it)
                        }
                        ?: defaultConfig.model.annotations.validations.namespace,
                ),
                kotlinx = defaultConfig.model.annotations.kotlinx.copy(
                    enabled = ext.model.annotations.kotlinx.enabled
                        ?: defaultConfig.model.annotations.kotlinx.enabled,
                    serializable = ext.model.annotations.kotlinx.serializable
                        ?: defaultConfig.model.annotations.kotlinx.serializable,
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

        val config = defaultConfig.copy(
            model = modelConfig,
            api = apiConfig,
        )

        clearOutput(config)

        OpenApi2KotlinApp.openApi2kotlin(config)
    }

    private fun resolveApiConfig(ext: OpenApi2KotlinExtension): OpenApi2KotlinUseCase.ApiConfig? {
        val server = ext.server
        val client = ext.client

        if (server != null && client != null) {
            throw GradleException(
                "openapi2kotlin: client{} and server{} cannot coexist.\n" +
                        "This generator is intentionally single-target.\n" +
                        "\n" +
                        "If you feel the need to generate both in one run,\n" +
                        "the issue is likely architectural rather than configurational.\n" +
                        "\n" +
                        "Choose exactly one:\n\n" +
                        "openapi2kotlin {\n" +
                        "    client { ... }\n" +
                        "}\n" +
                        "\n" +
                        "or:\n\n" +
                        "openapi2kotlin {\n" +
                        "    server { ... }\n" +
                        "}"
            )
        }

        return when {
            server != null -> {
                val library = server.library ?: throw GradleException(
                    "openapi2kotlin: server.library is required, e.g.\n" +
                        "openapi2kotlin {\n" +
                        "    server {\n" +
                        "        library = Spring\n" +
                        "    }\n" +
                        "}"
                )

                val defaults = when (library) {
                    OpenApi2KotlinExtension.ServerLibrary.Ktor -> OpenApi2KotlinUseCase.ApiConfig.ServerKtor()
                    OpenApi2KotlinExtension.ServerLibrary.Spring -> OpenApi2KotlinUseCase.ApiConfig.ServerSpring()
                }

                val basePathVar =
                    server.basePathVar
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: defaults.basePathVar

                val effectiveSwaggerEnabled =
                    server.swagger.enabled ?: defaults.swagger.enabled

                when (library) {
                    OpenApi2KotlinExtension.ServerLibrary.Ktor -> OpenApi2KotlinUseCase.ApiConfig.ServerKtor(
                        packageName = server.packageName ?: defaults.packageName,
                        basePathVar = basePathVar,
                        swagger = defaults.swagger.copy(enabled = effectiveSwaggerEnabled),
                    )
                    OpenApi2KotlinExtension.ServerLibrary.Spring -> OpenApi2KotlinUseCase.ApiConfig.ServerSpring(
                        packageName = server.packageName ?: defaults.packageName,
                        basePathVar = basePathVar,
                        swagger = defaults.swagger.copy(enabled = effectiveSwaggerEnabled),
                    )
                }
            }

            client != null -> {
                val library = client.library ?: throw GradleException(
                    "openapi2kotlin: client.library is required, e.g.\n" +
                        "openapi2kotlin {\n" +
                        "    client {\n" +
                        "        library = Ktor\n" +
                        "    }\n" +
                        "}"
                )

                val defaults = when (library) {
                    OpenApi2KotlinExtension.ClientLibrary.Ktor -> OpenApi2KotlinUseCase.ApiConfig.ClientKtor()
                    OpenApi2KotlinExtension.ClientLibrary.RestClient -> OpenApi2KotlinUseCase.ApiConfig.ClientRestClient()
                }

                val basePathVar =
                    client.basePathVar
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: defaults.basePathVar

                when (library) {
                    OpenApi2KotlinExtension.ClientLibrary.Ktor -> OpenApi2KotlinUseCase.ApiConfig.ClientKtor(
                        packageName = client.packageName ?: defaults.packageName,
                        basePathVar = basePathVar,
                    )
                    OpenApi2KotlinExtension.ClientLibrary.RestClient -> OpenApi2KotlinUseCase.ApiConfig.ClientRestClient(
                        packageName = client.packageName ?: defaults.packageName,
                        basePathVar = basePathVar,
                    )
                }
            }

            else -> null
        }
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

        val apiPackageName = config.api?.packageName
        if (apiPackageName != null) {
            clearPackageDir(outputDirPath, apiPackageName)
        }
    }

    private fun clearPackageDir(outputDirPath: Path, packageName: String) {
        val packageDir = outputDirPath.resolve(packageName.replace('.', '/'))
        if (Files.exists(packageDir)) {
            packageDir.toFile().deleteRecursively()
        }
    }
}
