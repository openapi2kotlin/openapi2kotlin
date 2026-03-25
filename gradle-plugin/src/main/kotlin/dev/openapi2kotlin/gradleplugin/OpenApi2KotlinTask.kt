package dev.openapi2kotlin.gradleplugin

import dev.openapi2kotlin.OpenApi2KotlinApp
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.Path

@DisableCachingByDefault(because = "OpenAPI generation is not yet declared with stable incremental inputs and outputs.")
abstract class OpenApi2KotlinTask : DefaultTask() {
    @TaskAction
    fun generate() {
        val ext = project.extensions.getByType(OpenApi2KotlinExtension::class.java)

        if (!ext.enabled) return

        val apiConfig = resolveApiConfig(ext)
        val config =
            OpenApi2KotlinUseCase.Config(
                inputSpecPath = resolveInputSpecPath(ext),
                outputDirPath = resolveOutputSpecPath(ext),
                model = ext.toUseCaseModelConfig(apiConfig = apiConfig),
                api = apiConfig,
            )

        clearOutput(config)

        OpenApi2KotlinApp.openApi2kotlin(config)
    }

    private fun resolveApiConfig(ext: OpenApi2KotlinExtension): OpenApi2KotlinUseCase.ApiConfig? {
        val server = ext.server
        val client = ext.client
        ensureSingleTarget(server = server, client = client)

        return when {
            server != null -> resolveServerApiConfig(server)
            client != null -> resolveClientApiConfig(client)
            else -> null
        }
    }

    private fun resolveServerApiConfig(
        server: OpenApi2KotlinExtension.ServerExtension,
    ): OpenApi2KotlinUseCase.ApiConfig {
        val library = server.library ?: throw missingServerLibrary()
        val basePathVar = server.basePathVar.trim()
        val swagger = server.effectiveSwagger(library)

        return when (library) {
            OpenApi2KotlinExtension.ServerLibrary.Ktor -> {
                OpenApi2KotlinUseCase.ApiConfig.ServerKtor(
                    packageName = server.packageName,
                    basePathVar = basePathVar,
                    methodNameSingularized = server.methodNameSingularized,
                    methodNamePluralized = server.methodNamePluralized,
                    methodNameFromOperationId = server.methodNameFromOperationId,
                    swagger = swagger,
                )
            }

            OpenApi2KotlinExtension.ServerLibrary.Http4k -> {
                OpenApi2KotlinUseCase.ApiConfig.ServerHttp4k(
                    packageName = server.packageName,
                    basePathVar = basePathVar,
                    methodNameSingularized = server.methodNameSingularized,
                    methodNamePluralized = server.methodNamePluralized,
                    methodNameFromOperationId = server.methodNameFromOperationId,
                    swagger = swagger,
                )
            }

            OpenApi2KotlinExtension.ServerLibrary.Spring -> {
                OpenApi2KotlinUseCase.ApiConfig.ServerSpring(
                    packageName = server.packageName,
                    basePathVar = basePathVar,
                    methodNameSingularized = server.methodNameSingularized,
                    methodNamePluralized = server.methodNamePluralized,
                    methodNameFromOperationId = server.methodNameFromOperationId,
                    swagger = swagger,
                )
            }
        }
    }

    private fun resolveClientApiConfig(
        client: OpenApi2KotlinExtension.ClientExtension,
    ): OpenApi2KotlinUseCase.ApiConfig {
        val library = client.library ?: throw missingClientLibrary()
        val basePathVar = client.basePathVar.trim()

        return when (library) {
            OpenApi2KotlinExtension.ClientLibrary.Ktor -> {
                OpenApi2KotlinUseCase.ApiConfig.ClientKtor(
                    packageName = client.packageName,
                    basePathVar = basePathVar,
                    methodNameSingularized = client.methodNameSingularized,
                    methodNamePluralized = client.methodNamePluralized,
                    methodNameFromOperationId = client.methodNameFromOperationId,
                )
            }

            OpenApi2KotlinExtension.ClientLibrary.Http4k -> {
                OpenApi2KotlinUseCase.ApiConfig.ClientHttp4k(
                    packageName = client.packageName,
                    basePathVar = basePathVar,
                    methodNameSingularized = client.methodNameSingularized,
                    methodNamePluralized = client.methodNamePluralized,
                    methodNameFromOperationId = client.methodNameFromOperationId,
                )
            }

            OpenApi2KotlinExtension.ClientLibrary.RestClient -> {
                OpenApi2KotlinUseCase.ApiConfig.ClientRestClient(
                    packageName = client.packageName,
                    basePathVar = basePathVar,
                    methodNameSingularized = client.methodNameSingularized,
                    methodNamePluralized = client.methodNamePluralized,
                    methodNameFromOperationId = client.methodNameFromOperationId,
                )
            }
        }
    }

    private fun resolveInputSpecPath(ext: OpenApi2KotlinExtension): Path {
        val inputSpecStr =
            ext.inputSpec
                ?.takeIf { it.isNotBlank() }
                ?: throw GradleException(
                    "openapi2kotlin: inputSpec is required, e.g.\n" +
                        "openapi2kotlin {\n" +
                        "    inputSpec = \"${'$'}projectDir/src/main/resources/openapi.yaml\"\n" +
                        "}",
                )

        val inputSpecFile = project.file(inputSpecStr)
        if (!inputSpecFile.exists()) {
            throw GradleException("openapi2kotlin: inputSpec does not exist: ${inputSpecFile.absolutePath}")
        }
        return inputSpecFile.toPath()
    }

    private fun resolveOutputSpecPath(ext: OpenApi2KotlinExtension): Path {
        val outputDirString =
            ext.outputDir
                ?.takeIf { it.isNotBlank() }
                ?: throw GradleException(
                    "openapi2kotlin: outputDir is required, e.g.\n" +
                        "openapi2kotlin {\n" +
                        "    outputDir = " +
                        "\"layout.buildDirectory.dir(\\\"generated/openapi/src/main/kotlin\\\").get().asFile.path\"\n" +
                        "}",
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

    private fun clearPackageDir(
        outputDirPath: Path,
        packageName: String,
    ) {
        val packageDir = outputDirPath.resolve(packageName.replace('.', '/'))
        if (Files.exists(packageDir)) {
            packageDir.toFile().deleteRecursively()
        }
    }
}

private fun ensureSingleTarget(
    server: OpenApi2KotlinExtension.ServerExtension?,
    client: OpenApi2KotlinExtension.ClientExtension?,
) {
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
                "}",
        )
    }
}

private fun OpenApi2KotlinExtension.ServerExtension.effectiveSwagger(
    library: OpenApi2KotlinExtension.ServerLibrary,
): Boolean =
    when (library) {
        OpenApi2KotlinExtension.ServerLibrary.Ktor -> swagger ?: false
        OpenApi2KotlinExtension.ServerLibrary.Http4k -> swagger ?: false
        OpenApi2KotlinExtension.ServerLibrary.Spring -> swagger ?: true
    }

private fun missingServerLibrary(): GradleException =
    GradleException(
        "openapi2kotlin: server.library is required, e.g.\n" +
            "openapi2kotlin {\n" +
            "    server {\n" +
            "        library = Ktor\n" +
            "    }\n" +
            "}",
    )

private fun missingClientLibrary(): GradleException =
    GradleException(
        "openapi2kotlin: client.library is required, e.g.\n" +
            "openapi2kotlin {\n" +
            "    client {\n" +
            "        library = Ktor\n" +
            "    }\n" +
            "}",
    )
