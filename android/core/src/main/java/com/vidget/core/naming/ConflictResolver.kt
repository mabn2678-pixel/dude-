package com.vidget.core.naming

import com.vidget.domain.model.NameConflictResolution

sealed interface ConflictResolutionResult {
    data class ProceedWithFilename(val filename: String, val shouldOverwrite: Boolean = false) : ConflictResolutionResult
    data object SkipDownload : ConflictResolutionResult
    data object RequiresUserPrompt : ConflictResolutionResult
}

/**
 * Handles existing file collisions with automatic numeric incrementing or user prompt.
 */
object ConflictResolver {

    /**
     * Resolves next unique filename with incremental counter: "filename (1).ext", "filename (2).ext".
     */
    fun resolveUniqueName(
        sanitizedBase: String,
        cleanExt: String,
        fileExistsCheck: (String) -> Boolean
    ): String {
        val initialName = "$sanitizedBase.$cleanExt"
        if (!fileExistsCheck(initialName)) {
            return initialName
        }

        var counter = 1
        while (true) {
            val candidate = "$sanitizedBase ($counter).$cleanExt"
            if (!fileExistsCheck(candidate)) {
                return candidate
            }
            counter++
        }
    }

    /**
     * Applies configured conflict resolution strategy.
     */
    fun handleConflict(
        sanitizedBase: String,
        cleanExt: String,
        strategy: NameConflictResolution,
        fileExistsCheck: (String) -> Boolean
    ): ConflictResolutionResult {
        val initialName = "$sanitizedBase.$cleanExt"
        if (!fileExistsCheck(initialName)) {
            return ConflictResolutionResult.ProceedWithFilename(initialName)
        }

        return when (strategy) {
            NameConflictResolution.AUTO_RENAME -> {
                val unique = resolveUniqueName(sanitizedBase, cleanExt, fileExistsCheck)
                ConflictResolutionResult.ProceedWithFilename(unique)
            }
            NameConflictResolution.OVERWRITE -> {
                ConflictResolutionResult.ProceedWithFilename(initialName, shouldOverwrite = true)
            }
            NameConflictResolution.SKIP -> {
                ConflictResolutionResult.SkipDownload
            }
            NameConflictResolution.ASK_EVERY_TIME -> {
                ConflictResolutionResult.RequiresUserPrompt
            }
        }
    }
}
