package dev.openapi2kotlin.application.core.openapi2kotlin.service

import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateClientPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateModelPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateServerPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.ParseSpecPort
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.client.prepareClientApis
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.prepareModels
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.prepareServerApis
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase


class OpenApi2KotlinService(
    private val parseSpecPort: ParseSpecPort,
    private val generateServerPort: GenerateServerPort,
    private val generateModelPort: GenerateModelPort,
    private val generateClientPort: GenerateClientPort,
) : OpenApi2KotlinUseCase {

    override fun openApi2kotlin(config: OpenApi2KotlinUseCase.Config) {
        val openApi = parseSpecPort.parseSpec(config.inputSpecPath)

        val models = prepareModels(
            schemas = openApi.rawSchemas,
            config = config.model,
        )

        generateModelPort.generateModel(
            GenerateModelPort.Command(
                outputDirPath = config.outputDirPath,
                models = models,
            )
        )

        if (config.server.enabled) {
            val serverApis = prepareServerApis(
                rawPaths = openApi.rawPaths,
                models = models,
                mappingCfg = config.model.mapping,
                cfg = config.server,
            )

            generateServerPort.generateServer(
                GenerateServerPort.Command(
                    serverApis = serverApis,
                    serverPackageName = config.server.packageName,
                    modelPackageName = config.model.packageName,
                    outputDirPath = config.outputDirPath,
                    models = models,
                )
            )
        }

        if (config.client.enabled) {
            generateClientPort.generateClient(GenerateClientPort.Command(
                clientApis = prepareClientApis(openApi.rawPaths),
                clientPackageName = config.client.packageName,
                modelPackageName = config.model.packageName,
                outputDirPath = config.outputDirPath,
            ))
        }
    }
}
