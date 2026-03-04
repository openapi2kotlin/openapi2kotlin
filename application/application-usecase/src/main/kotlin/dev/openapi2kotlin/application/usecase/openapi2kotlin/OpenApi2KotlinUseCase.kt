package dev.openapi2kotlin.application.usecase.openapi2kotlin

import java.nio.file.Path

fun interface OpenApi2KotlinUseCase {
    fun openApi2kotlin(config: Config)

    data class Config(
        val inputSpecPath: Path,
        val outputDirPath: Path,
        val model: ModelConfig,
        val api: ApiConfig? = null,
    )

    data class ModelConfig(
        val packageName: String,
        val serialization: Serialization? = null,
        val validation: Validation? = null,
        val double2BigDecimal: Boolean,
        val float2BigDecimal: Boolean,
        val integer2Long: Boolean,
        val jacksonJsonPropertyMapping: Boolean = true,
        val jacksonDefaultDiscriminatorValue: Boolean = true,
        val jacksonStrictDiscriminatorSerialization: Boolean = true,
        val jacksonJsonValue: Boolean = true,
        val jacksonJsonCreator: Boolean = true,
        val kotlinxSerializable: Boolean = true,
    ) {
        enum class Serialization {
            JACKSON,
            KOTLINX,
        }

        enum class Validation(
            val value: String,
        ) {
            JAKARTA("jakarta"),
            JAVAX("javax");

            override fun toString(): String = value
        }
    }

    sealed interface ApiConfig {
        val packageName: String
        val basePathVar: String

        sealed interface Client : ApiConfig

        data class ClientKtor(
            override val packageName: String,
            override val basePathVar: String,
        ) : Client

        data class ClientRestClient(
            override val packageName: String,
            override val basePathVar: String,
        ) : Client

        sealed interface Server : ApiConfig {
            val swagger: Boolean
        }

        data class ServerKtor(
            override val packageName: String,
            override val basePathVar: String,
            override val swagger: Boolean,
        ) : Server

        data class ServerSpring(
            override val packageName: String,
            override val basePathVar: String,
            override val swagger: Boolean,
        ) : Server
    }
}
