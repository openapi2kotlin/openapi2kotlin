package dev.openapi2kotlin.application.core.openapi2kotlin.service

import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateServerPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateClientPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.GenerateModelPort
import dev.openapi2kotlin.application.core.openapi2kotlin.port.ParseSpecPort
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.client.prepareClientApis
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.prepareModels
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.server.prepareServerApis
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase


class OpenApi2KotlinService(
    private val parseSpecPort: ParseSpecPort,
    private val generateServerPort: GenerateServerPort,
    private val generateModelPort: GenerateModelPort,
    private val generateClientPort: GenerateClientPort
): OpenApi2KotlinUseCase {
    override fun openApi2kotlin(config: OpenApi2KotlinUseCase.Config) {
        val openApi = parseSpecPort.parseSpec(config.inputSpecPath)

        generateModelPort.generateModel(GenerateModelPort.Command(
            outputDirPath = config.outputDirPath,
            models = prepareModels( openApi.rawSchemas, config.model),
        ))

        if (config.server.enabled) {
            generateServerPort.generateServer(GenerateServerPort.Command(
                serverApis = prepareServerApis(openApi.rawPaths),
                serverPackageName = config.server.packageName,
                modelPackageName = config.model.packageName,
                outputDirPath = config.outputDirPath,
            ))
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
