package dev.openapi2kotlin.application.core.openapi2kotlin.service

import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateApiPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateModelPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.ParseSpecPort
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.api.prepareApis
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.prepareModels
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

class OpenApi2KotlinService(
    private val parseSpecPort: ParseSpecPort,
    private val generateApiPort: GenerateApiPort,
    private val generateModelPort: GenerateModelPort,
) : OpenApi2KotlinUseCase {

    override fun openApi2kotlin(config: OpenApi2KotlinUseCase.Config) {
        val openApi = parseSpecPort.parseSpec(config.inputSpecPath)

        val models = prepareModels(
            schemas = openApi.rawSchemas,
            config = config,
        )

        generateModelPort.generateModel(
            GenerateModelPort.Command(
                outputDirPath = config.outputDirPath,
                models = models,
            )
        )

        val apiPackageName = config.api?.packageName ?: return

        val apis = prepareApis(
            rawPaths = openApi.rawPaths,
            models = models,
            config = config,
        )

        generateApiPort.generateApi(
            GenerateApiPort.Command(
                apis = apis,
                packageName = apiPackageName,
                modelPackageName = config.model.packageName,
                outputDirPath = config.outputDirPath,
                models = models,
            )
        )
    }
}
