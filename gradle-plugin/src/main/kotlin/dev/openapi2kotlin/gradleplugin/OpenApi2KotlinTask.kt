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

        val apiConfig = resolveApiConfig(ext)
        val config = OpenApi2KotlinUseCase.Config(
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
                val library = server.library ?: OpenApi2KotlinExtension.ServerLibrary.Ktor

                val swagger = when (library) {
                    OpenApi2KotlinExtension.ServerLibrary.Ktor ->
                        server.swagger ?: false

                    OpenApi2KotlinExtension.ServerLibrary.Spring ->
                        server.swagger ?: true
                }

                when (library) {
                    OpenApi2KotlinExtension.ServerLibrary.Ktor -> OpenApi2KotlinUseCase.ApiConfig.ServerKtor(
                        packageName = server.packageName,
                        basePathVar = server.basePathVar.trim().takeIf { it.isNotBlank() }
                            ?: OpenApi2KotlinExtension.DEFAULT_BASE_PATH_VAR,
                        swagger = swagger,
                    )

                    OpenApi2KotlinExtension.ServerLibrary.Spring -> OpenApi2KotlinUseCase.ApiConfig.ServerSpring(
                        packageName = server.packageName,
                        basePathVar = server.basePathVar.trim().takeIf { it.isNotBlank() }
                            ?: OpenApi2KotlinExtension.DEFAULT_BASE_PATH_VAR,
                        swagger = swagger,
                    )
                }
            }

            client != null -> {
                val library = client.library ?: OpenApi2KotlinExtension.ClientLibrary.Ktor

                when (library) {
                    OpenApi2KotlinExtension.ClientLibrary.Ktor -> OpenApi2KotlinUseCase.ApiConfig.ClientKtor(
                        packageName = client.packageName,
                        basePathVar = client.basePathVar.trim().takeIf { it.isNotBlank() }
                            ?: OpenApi2KotlinExtension.DEFAULT_BASE_PATH_VAR,
                    )

                    OpenApi2KotlinExtension.ClientLibrary.RestClient -> OpenApi2KotlinUseCase.ApiConfig.ClientRestClient(
                        packageName = client.packageName,
                        basePathVar = client.basePathVar.trim().takeIf { it.isNotBlank() }
                            ?: OpenApi2KotlinExtension.DEFAULT_BASE_PATH_VAR,
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
