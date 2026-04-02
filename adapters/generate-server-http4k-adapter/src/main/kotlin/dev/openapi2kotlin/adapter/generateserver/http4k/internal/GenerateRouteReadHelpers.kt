package dev.openapi2kotlin.adapter.generateserver.http4k.internal

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiEndpointDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.api.ApiParamDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.raw.RawPathDO

private val RESPONSE_T = ClassName("org.http4k.core", "Response")
private val STATUS_T = ClassName("org.http4k.core", "Status")

internal fun httpMethodRef(ep: ApiEndpointDO): String =
    when (ep.rawOperation.httpMethod) {
        RawPathDO.HttpMethodDO.GET -> "GET"
        RawPathDO.HttpMethodDO.POST -> "POST"
        RawPathDO.HttpMethodDO.PUT -> "PUT"
        RawPathDO.HttpMethodDO.PATCH -> "PATCH"
        RawPathDO.HttpMethodDO.DELETE -> "DELETE"
    }

internal fun joinPaths(
    basePath: String,
    endpointPath: String,
): String {
    val base = basePath.trim().trim('/').takeIf { it.isNotBlank() }
    val path = endpointPath.trim().trim('/').takeIf { it.isNotBlank() }
    return when {
        base == null && path == null -> "/"
        base == null -> "/$path"
        path == null -> "/$base"
        else -> "/$base/$path"
    }
}

internal fun pathReadExpr(param: ApiParamDO): CodeBlock {
    val key = param.rawParam.name
    return when ((param.type as? TrivialTypeDO)?.kind) {
        TrivialTypeDO.Kind.STRING, null -> {
            CodeBlock.of(
                "request.path(%S) ?: return@to %T(%T.BAD_REQUEST)",
                key,
                RESPONSE_T,
                STATUS_T,
            )
        }

        TrivialTypeDO.Kind.LONG -> {
            CodeBlock.of(
                "request.path(%S)?.toLongOrNull() ?: return@to %T(%T.BAD_REQUEST)",
                key,
                RESPONSE_T,
                STATUS_T,
            )
        }

        TrivialTypeDO.Kind.INT -> {
            CodeBlock.of(
                "request.path(%S)?.toIntOrNull() ?: return@to %T(%T.BAD_REQUEST)",
                key,
                RESPONSE_T,
                STATUS_T,
            )
        }

        TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE -> {
            CodeBlock.of(
                "request.path(%S)?.toDoubleOrNull() ?: return@to %T(%T.BAD_REQUEST)",
                key,
                RESPONSE_T,
                STATUS_T,
            )
        }

        TrivialTypeDO.Kind.BOOLEAN -> {
            CodeBlock.of(
                "request.path(%S)?.toBooleanStrictOrNull() ?: return@to %T(%T.BAD_REQUEST)",
                key,
                RESPONSE_T,
                STATUS_T,
            )
        }

        else -> {
            CodeBlock.of(
                "request.path(%S) ?: return@to %T(%T.BAD_REQUEST)",
                key,
                RESPONSE_T,
                STATUS_T,
            )
        }
    }
}

internal fun queryReadExpr(param: ApiParamDO): CodeBlock {
    val key = param.rawParam.name
    return when (param.type) {
        is ListTypeDO -> {
            val kind = ((param.type as ListTypeDO).elementType as? TrivialTypeDO)?.kind
            when (kind) {
                TrivialTypeDO.Kind.LONG -> {
                    CodeBlock.of(
                        "request.queries(%S).mapNotNull(String::toLongOrNull).takeIf { it.isNotEmpty() }",
                        key,
                    )
                }

                TrivialTypeDO.Kind.INT -> {
                    CodeBlock.of(
                        "request.queries(%S).mapNotNull(String::toIntOrNull).takeIf { it.isNotEmpty() }",
                        key,
                    )
                }

                TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE -> {
                    CodeBlock.of(
                        "request.queries(%S).mapNotNull(String::toDoubleOrNull).takeIf { it.isNotEmpty() }",
                        key,
                    )
                }

                TrivialTypeDO.Kind.BOOLEAN -> {
                    CodeBlock.of(
                        "request.queries(%S).mapNotNull(String::toBooleanStrictOrNull).takeIf { it.isNotEmpty() }",
                        key,
                    )
                }

                else -> {
                    CodeBlock.of("request.queries(%S).filterNotNull().takeIf { it.isNotEmpty() }", key)
                }
            }
        }

        else -> {
            when ((param.type as? TrivialTypeDO)?.kind) {
                TrivialTypeDO.Kind.STRING, null -> {
                    CodeBlock.of("request.query(%S)", key)
                }

                TrivialTypeDO.Kind.LONG -> {
                    CodeBlock.of("request.query(%S)?.toLongOrNull()", key)
                }

                TrivialTypeDO.Kind.INT -> {
                    CodeBlock.of("request.query(%S)?.toIntOrNull()", key)
                }

                TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE -> {
                    CodeBlock.of("request.query(%S)?.toDoubleOrNull()", key)
                }

                TrivialTypeDO.Kind.BOOLEAN -> {
                    CodeBlock.of("request.query(%S)?.toBooleanStrictOrNull()", key)
                }

                else -> {
                    CodeBlock.of("request.query(%S)", key)
                }
            }
        }
    }
}

internal fun headerReadExpr(param: ApiParamDO): CodeBlock {
    val key = param.rawParam.name
    return when ((param.type as? TrivialTypeDO)?.kind) {
        TrivialTypeDO.Kind.STRING, null -> {
            CodeBlock.of("request.header(%S)", key)
        }

        TrivialTypeDO.Kind.LONG -> {
            CodeBlock.of("request.header(%S)?.toLongOrNull()", key)
        }

        TrivialTypeDO.Kind.INT -> {
            CodeBlock.of("request.header(%S)?.toIntOrNull()", key)
        }

        TrivialTypeDO.Kind.FLOAT, TrivialTypeDO.Kind.DOUBLE -> {
            CodeBlock.of("request.header(%S)?.toDoubleOrNull()", key)
        }

        TrivialTypeDO.Kind.BOOLEAN -> {
            CodeBlock.of("request.header(%S)?.toBooleanStrictOrNull()", key)
        }

        else -> {
            CodeBlock.of("request.header(%S)", key)
        }
    }
}
