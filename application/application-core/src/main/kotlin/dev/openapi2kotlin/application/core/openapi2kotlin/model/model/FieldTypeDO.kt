package dev.openapi2kotlin.application.core.openapi2kotlin.model.model

sealed interface FieldTypeDO {
    val nullable: Boolean
}

data class TrivialTypeDO(
    val kind: Kind,
    override val nullable: Boolean,
) : FieldTypeDO {
    enum class Kind {
        STRING,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        BIG_DECIMAL,
        BOOLEAN,
        LOCAL_DATE,
        OFFSET_DATE_TIME,
        BYTE_ARRAY,
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
