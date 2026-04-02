package dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.model.helpers

import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.FieldTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ListTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.ModelShapeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.RefTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.domain.model.TrivialTypeDO
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.renderDefault
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.toFinalType
import dev.openapi2kotlin.application.core.openapi2kotlin.service.internal.common.withNullability
import dev.openapi2kotlin.application.usecase.openapi2kotlin.OpenApi2KotlinUseCase

internal fun List<ModelDO>.handleFields(cfg: OpenApi2KotlinUseCase.ModelConfig) {
    val byName: Map<String, ModelDO> = associateBy { it.rawSchema.originalName }

    forEach { component ->
        component.kdoc = component.rawSchema.description
        val parentPropSchemas = component.resolveParentPropertySchemas(byName, cfg)
        val fields = parentPropSchemas.toInheritedFields()
        component.mergeOwnPropertyFields(fields, parentPropSchemas, cfg)
        component.prependDiscriminatorFieldIfMissing(fields)
        component.fields = fields.toMutableList()
    }

    val byNameAfter = associateBy { it.rawSchema.originalName }

    forEach { component ->
        val parentName =
            when (val shape = component.modelShape) {
                is ModelShapeDO.DataClass -> shape.extend
                is ModelShapeDO.EmptyClass -> shape.extend
                is ModelShapeDO.OpenClass -> shape.extend
                else -> null
            } ?: return@forEach

        val parent = byNameAfter[parentName] ?: return@forEach

        component.fields =
            component.fields
                .map { field ->
                    val parentField = parent.fields.firstOrNull { it.generatedName == field.generatedName }

                    val parentRequired = parentField?.required ?: false
                    val finalRequired = parentRequired || field.required
                    val finalType = field.type.withNullability(nullable = !finalRequired)

                    // requiredness pass must not rewrite docs; docs were resolved in the first pass
                    field.copy(type = finalType, required = finalRequired)
                }.toMutableList()
    }
}

private fun ModelDO.resolveParentPropertySchemas(
    byName: Map<String, ModelDO>,
    cfg: OpenApi2KotlinUseCase.ModelConfig,
): MutableMap<String, PropInfo> =
    mutableMapOf<String, PropInfo>().also { parentPropSchemas ->
        rawSchema.allOfParents.forEach { parentName ->
            collectAllPropertiesFromSchema(
                schemaName = parentName,
                schemas = byName,
                into = parentPropSchemas,
                visited = mutableSetOf(),
                cfg = cfg,
            )
        }
    }

private fun Map<String, PropInfo>.toInheritedFields(): MutableList<FieldDO> =
    map { (propName, info) ->
        FieldDO(
            originalName = propName,
            generatedName = propName.toKotlinName(),
            overridden = true,
            type = info.type,
            required = info.required,
            defaultValueCode = info.defaultValueCode,
            kdoc = info.kdoc,
        )
    }.toMutableList()

private fun ModelDO.mergeOwnPropertyFields(
    fields: MutableList<FieldDO>,
    parentPropSchemas: Map<String, PropInfo>,
    cfg: OpenApi2KotlinUseCase.ModelConfig,
) {
    rawSchema.ownProperties.forEach { (propName, prop) ->
        val ownInfo =
            PropInfo(
                type = prop.type.toFinalType(cfg),
                required = prop.required,
                defaultValueCode = prop.defaultValue?.let { renderDefault(prop.type, cfg, it) },
                kdoc = prop.description,
            )
        val mergedField = createMergedField(propName, ownInfo, parentPropSchemas[propName])
        fields.upsertByOriginalName(mergedField)
    }
}

private fun createMergedField(
    propName: String,
    ownInfo: PropInfo,
    parentInfo: PropInfo?,
): FieldDO {
    val effectiveRequired = (parentInfo?.required ?: false) || ownInfo.required
    val effectiveType =
        if (parentInfo != null && !parentInfo.type.canBeOverriddenBy(ownInfo.type)) {
            parentInfo.type
        } else {
            ownInfo.type
        }

    return FieldDO(
        originalName = propName,
        generatedName = propName.toKotlinName(),
        overridden = parentInfo != null,
        type = effectiveType.withNullability(nullable = !effectiveRequired),
        required = effectiveRequired,
        defaultValueCode = ownInfo.defaultValueCode,
        kdoc = ownInfo.kdoc ?: parentInfo?.kdoc,
    )
}

private fun MutableList<FieldDO>.upsertByOriginalName(newField: FieldDO) {
    val existingIndex = indexOfFirst { it.originalName == newField.originalName }
    if (existingIndex >= 0) {
        this[existingIndex] = newField
    } else {
        add(newField)
    }
}

private fun ModelDO.prependDiscriminatorFieldIfMissing(fields: MutableList<FieldDO>) {
    val discName = rawSchema.discriminatorPropertyName ?: return
    if (fields.any { it.originalName == discName }) return

    fields.add(
        0,
        FieldDO(
            originalName = discName,
            generatedName = discName.toKotlinName(),
            overridden = false,
            type = TrivialTypeDO(TrivialTypeDO.Kind.STRING, nullable = false),
            required = true,
            defaultValueCode = null,
            kdoc = null,
        ),
    )
}

private fun FieldTypeDO.canBeOverriddenBy(child: FieldTypeDO): Boolean =
    when (this) {
        is TrivialTypeDO -> {
            when {
                kind == TrivialTypeDO.Kind.ANY -> true
                child !is TrivialTypeDO -> false
                else -> kind == child.kind
            }
        }

        is RefTypeDO -> {
            child is RefTypeDO && schemaName == child.schemaName
        }

        is ListTypeDO -> {
            child is ListTypeDO && elementType.canBeOverriddenBy(child.elementType)
        }
    }

private data class PropInfo(
    val type: FieldTypeDO,
    val required: Boolean,
    val defaultValueCode: String?,
    val kdoc: String?,
)

private fun collectAllPropertiesFromSchema(
    schemaName: String,
    schemas: Map<String, ModelDO>,
    into: MutableMap<String, PropInfo>,
    visited: MutableSet<String>,
    cfg: OpenApi2KotlinUseCase.ModelConfig,
) {
    if (!visited.add(schemaName)) return
    val schema = schemas[schemaName] ?: return

    schema.rawSchema.ownProperties.values.forEach { prop ->
        val info =
            PropInfo(
                type = prop.type.toFinalType(cfg),
                required = prop.required,
                defaultValueCode = prop.defaultValue?.let { renderDefault(prop.type, cfg, it) },
                kdoc = prop.description,
            )
        into.merge(prop.name, info) { a, b ->
            PropInfo(
                type = a.type,
                required = a.required || b.required,
                defaultValueCode = a.defaultValueCode ?: b.defaultValueCode,
                // retain any existing doc; if absent, take the incoming one
                kdoc = a.kdoc ?: b.kdoc,
            )
        }
    }

    schema.rawSchema.allOfParents.forEach { parentName ->
        collectAllPropertiesFromSchema(parentName, schemas, into, visited, cfg)
    }
}
