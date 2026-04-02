package builders.we.globus.bff.tools.detekt.ext

import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile

internal fun KtFile.isOpenApi2KotlinCoreDomainFile(): Boolean {
    val packageName = packageFqName.asString()
    return packageName.startsWith("dev.openapi2kotlin.application.core.openapi2kotlin.domain")
}

internal fun KtFile.isOpenApi2KotlinCoreFile(): Boolean {
    val packageName = packageFqName.asString()
    return packageName.startsWith("dev.openapi2kotlin.application.core.openapi2kotlin")
}

internal fun KtAnnotationEntry.resourcePath(): String? {
    val argument = valueArgumentList?.arguments?.singleOrNull() ?: return null
    return argument
        .getArgumentExpression()
        ?.text
        ?.removePrefix("\"")
        ?.removeSuffix("\"")
}
