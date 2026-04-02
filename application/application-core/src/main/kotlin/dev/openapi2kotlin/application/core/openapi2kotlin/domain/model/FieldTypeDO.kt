package dev.openapi2kotlin.application.core.openapi2kotlin.domain.model

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
        JAVA_LOCAL_DATE,
        KOTLINX_LOCAL_DATE,
        OFFSET_DATE_TIME,
        INSTANT,
        BYTE_ARRAY,
        JSON_ELEMENT,
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
