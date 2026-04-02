package builders.we.globus.bff.tools.detekt.ext

import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolute
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile

internal fun KtFile.repoRoot(): Path? {
    val path = absolutePath() ?: return null
    return generateSequence(path.parent) { current -> current.parent }
        .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
}

internal fun KtFile.absolutePath(): Path? = runCatching { Path.of(virtualFilePath).absolute() }.getOrNull()

internal fun cachedRepositoryTextIndex(
    root: Path,
    pattern: Regex,
    supportedExtensions: Set<String>,
    includeTestSources: Boolean,
): RepositoryTextIndex =
    repositoryTextIndexes.computeIfAbsent(
        RepositoryTextIndexKey(
            root = root,
            pattern = pattern.pattern,
            supportedExtensions = supportedExtensions,
            includeTestSources = includeTestSources,
        ),
    ) {
        buildRepositoryTextIndex(root, pattern, supportedExtensions, includeTestSources)
    }

internal class RepositoryTextIndex(
    private val occurrencesByIdentifier: Map<String, Int>,
) {
    fun occurrences(identifier: String): Int = occurrencesByIdentifier[identifier] ?: 0
}

private fun buildRepositoryTextIndex(
    root: Path,
    pattern: Regex,
    supportedExtensions: Set<String>,
    includeTestSources: Boolean,
): RepositoryTextIndex {
    val occurrencesByIdentifier = mutableMapOf<String, Int>()
    Files.walk(root).use { paths ->
        paths
            .filter { path ->
                isRepositoryTextFile(
                    path = path,
                    supportedExtensions = supportedExtensions,
                    includeTestSources = includeTestSources,
                )
            }.forEach { path ->
                pattern.findAll(Files.readString(path)).forEach { match ->
                    val identifier = match.value
                    occurrencesByIdentifier.merge(identifier, 1, Int::plus)
                }
            }
    }
    return RepositoryTextIndex(occurrencesByIdentifier)
}

private fun isRepositoryTextFile(
    path: Path,
    supportedExtensions: Set<String>,
    includeTestSources: Boolean,
): Boolean {
    val normalizedPath = path.invariantSeparatorsPathString
    return path.isRegularFile() &&
        path.extension in supportedExtensions &&
        "/build/" !in normalizedPath &&
        (includeTestSources || "/src/test/" !in normalizedPath) &&
        "/tools/detekt-tools/src/test/" !in normalizedPath &&
        "/.gradle/" !in normalizedPath &&
        "/.git/" !in normalizedPath
}

private val repositoryTextIndexes = ConcurrentHashMap<RepositoryTextIndexKey, RepositoryTextIndex>()

private data class RepositoryTextIndexKey(
    val root: Path,
    val pattern: String,
    val supportedExtensions: Set<String>,
    val includeTestSources: Boolean,
)
