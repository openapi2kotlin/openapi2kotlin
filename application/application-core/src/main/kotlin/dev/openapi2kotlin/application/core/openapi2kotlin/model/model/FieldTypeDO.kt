package dev.openapi2kotlin.application.core.openapi2kotlin.model.model

sealed interface FieldTypeDO {
    val nullable: Boolean
}

data class PrimitiveTypeDO(
    val name: PrimitiveTypeNameDO,
    override val nullable: Boolean,
) : FieldTypeDO {
    enum class PrimitiveTypeNameDO {
        STRING,
        INT,
        LONG,
        DOUBLE,
        BOOLEAN,
        ANY,
    }
}

data class RefTypeDO(
    val schemaName: String,
    override val nullable: Boolean,
) : FieldTypeDO

data class ListTypeDO(
    val elementType: FieldTypeDO,
    override val nullable: Boolean,
) : FieldTypeDO
