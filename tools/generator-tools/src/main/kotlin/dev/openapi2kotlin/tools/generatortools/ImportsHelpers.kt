package dev.openapi2kotlin.tools.generatortools

import com.squareup.kotlinpoet.FileSpec
import dev.openapi2kotlin.application.core.openapi2kotlin.model.api.ApiAnnotationDO

private val FQCN_CLASS_LITERAL =
    Regex("""(?<![A-Za-z0-9_`])((?:[a-zA-Z_][a-zA-Z0-9_]*\.)+[A-Za-z_][A-Za-z0-9_]*)(::class)""")

private val FQCN_CALL =
    Regex("""(?<![A-Za-z0-9_`])((?:[a-zA-Z_][a-zA-Z0-9_]*\.)+[A-Za-z_][A-Za-z0-9_]*)(\s*\()""")

private val FQCN_DOT_ACCESS =
    Regex("""(?<![A-Za-z0-9_`])((?:[a-zA-Z_][a-zA-Z0-9_]*\.)+[A-Z][A-Za-z0-9_]*)(\.[A-Za-z_][A-Za-z0-9_]*)""")

private data class ImportCandidate(val pkg: String, val simple: String)

fun FileSpec.Builder.addImportsAndShortenArgs(
    annotations: List<ApiAnnotationDO>,
): FileSpec.Builder {
    val candidates = collectImportCandidates(annotations)

    val safe = candidates
        .groupBy { it.simple }
        .filter { (_, v) -> v.size == 1 }
        .map { (_, v) -> v.single() }
        .sortedWith(compareBy({ it.pkg }, { it.simple }))

    safe.forEach { addImport(it.pkg, it.simple) }

    return this
}

/**
 * Rewrites argsCode: FQCNs that were safely imported become simple names.
 *
 * fqName is not rewritten; it must remain fully-qualified.
 */
fun List<ApiAnnotationDO>.shortenArgs(): List<ApiAnnotationDO> {
    val candidates = collectImportCandidates(this)

    val safe = candidates
        .groupBy { it.simple }
        .filter { (_, v) -> v.size == 1 }
        .mapValues { (_, v) -> v.single() }

    fun rewrite(s: String): String {
        var out = s

        // 1) Foo.Bar::class -> Bar::class
        out = out.replace(FQCN_CLASS_LITERAL) { m ->
            val fq = m.groupValues[1]
            val cand = fq.toImportCandidateOrNull()
            if (cand != null && safe[cand.simple] == cand) "${cand.simple}::class" else m.value
        }

        // 2) Foo.Bar( -> Bar(
        out = out.replace(FQCN_CALL) { m ->
            val fq = m.groupValues[1]
            val suffix = m.groupValues[2]
            val cand = fq.toImportCandidateOrNull()
            if (cand != null && safe[cand.simple] == cand) "${cand.simple}$suffix" else m.value
        }

        // 3) Foo.Bar.BAZ -> Bar.BAZ (enum constants / object members), but only when Foo.Bar is a *Type*
        out = out.replace(FQCN_DOT_ACCESS) { m ->
            val fqType = m.groupValues[1]
            val memberSuffix = m.groupValues[2] // includes leading dot
            val cand = fqType.toImportCandidateOrNull()
            if (cand != null && safe[cand.simple] == cand) "${cand.simple}$memberSuffix" else m.value
        }

        return out
    }

    return map { a ->
        a.copy(
            argsCode = a.argsCode.map(::rewrite),
        )
    }
}

private fun collectImportCandidates(annotations: List<ApiAnnotationDO>): LinkedHashSet<ImportCandidate> {
    val candidates = LinkedHashSet<ImportCandidate>()

    annotations.forEach { a ->
        a.fqName.toImportCandidateOrNull()?.let(candidates::add)

        a.argsCode.forEach { arg ->
            // Foo.Bar::class
            FQCN_CLASS_LITERAL.findAll(arg).forEach { m ->
                m.groupValues[1].toImportCandidateOrNull()?.let(candidates::add)
            }

            // Foo.Bar(
            FQCN_CALL.findAll(arg).forEach { m ->
                m.groupValues[1].toImportCandidateOrNull()?.let(candidates::add)
            }

            // Foo.Bar.BAZ  (only when Foo.Bar is a Type name; see regex)
            FQCN_DOT_ACCESS.findAll(arg).forEach { m ->
                m.groupValues[1].toImportCandidateOrNull()?.let(candidates::add)
            }
        }
    }

    return candidates
}

private fun String.toImportCandidateOrNull(): ImportCandidate? {
    val lastDot = lastIndexOf('.')
    if (lastDot <= 0 || lastDot == length - 1) return null
    val pkg = substring(0, lastDot)
    val simple = substring(lastDot + 1)
    return ImportCandidate(pkg, simple)
}