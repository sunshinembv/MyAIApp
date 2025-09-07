package com.example.code_agent.legacy_code_review_agent

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch

/** Result of applying one unified diff to a file */
data class ApplyResult(
    val targetPath: String,
    val ok: Boolean,
    val message: String,
    val backupPath: String? = null
)

/** Lightweight meta to quickly check if a diff has hunks */
data class DiffMeta(
    val fileFromHeader: String?,
    val hunks: Int,
    val touchedLines: Int
)

/** Inspect unified diff without applying it */
fun inspectUnifiedDiff(unifiedDiffText: String): DiffMeta {
    val lines = unifiedDiffText.replace("\r\n", "\n").replace('\r', '\n').lines()
    val patch: Patch<String> = try { UnifiedDiffUtils.parseUnifiedDiff(lines) } catch (_: Throwable) {
        return DiffMeta(fileFromHeader = guessTargetPath(lines), hunks = 0, touchedLines = 0)
    }
    val hunks = patch.deltas.size
    val touched = patch.deltas.sumOf { it.target.lines.size + it.source.lines.size }
    val file = guessTargetPath(lines)
    return DiffMeta(fileFromHeader = file, hunks = hunks, touchedLines = touched)
}

/** Apply a list of unified diffs produced by the LLM. */
fun applyAllDiffs(rootDir: File, diffs: List<LlmDiff>, dryRun: Boolean = false): List<ApplyResult> =
    diffs.map { d -> applyUnifiedDiffText(rootDir, d.unified_diff, expectedTarget = d.file, dryRun = dryRun) }

// ----- Smarter path resolution (handles Cyrillic lookalikes) -----
private fun canonicalizePathForCompare(p: String): String {
    val map = mapOf(
        'А' to 'A','а' to 'a','В' to 'B','Е' to 'E','е' to 'e','К' to 'K','к' to 'k','М' to 'M','Н' to 'H','О' to 'O','о' to 'o','Р' to 'P','р' to 'p','С' to 'C','с' to 'c','Т' to 'T','Х' to 'X','х' to 'x','У' to 'Y','у' to 'y'
    )
    val sb = StringBuilder(p.length)
    p.forEach { ch -> sb.append(map[ch] ?: ch) }
    return sb.toString().replace('\\', '/').lowercase()
}

private fun resolveTargetFile(rootDir: File, targetPath: String): File? {
    val direct = File(rootDir, targetPath)
    if (direct.exists()) return direct

    // Try to find by canonicalized (latinized) comparison — protects from Cyrillic lookalike letters
    val wanted = canonicalizePathForCompare(targetPath)
    var best: File? = null
    rootDir.walkTopDown().forEach { f ->
        if (!f.isFile) return@forEach
        val rel = try { f.relativeTo(rootDir).path } catch (_: Throwable) { f.absolutePath }
        val canon = canonicalizePathForCompare(rel)
        if (canon == wanted) { best = f; return@forEach }
    }
    return best
}

/** Parse and apply a single unified diff text against a file under [rootDir]. */
fun applyUnifiedDiffText(
    rootDir: File,
    unifiedDiffText: String,
    expectedTarget: String? = null,
    dryRun: Boolean = false
): ApplyResult {
    val lines = unifiedDiffText
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lines()

    val targetRaw = expectedTarget ?: guessTargetPath(lines)
    ?: return ApplyResult("?", false, "Can't guess target file from diff headers (no +++ line)")

    val file = resolveTargetFile(rootDir, targetRaw)
        ?: return ApplyResult(targetRaw, false, "Target file does not exist under root (or path uses lookalike letters): $targetRaw")

    val original = file.readLines()
    val patch: Patch<String> = try {
        UnifiedDiffUtils.parseUnifiedDiff(lines)
    } catch (t: Throwable) {
        return ApplyResult(targetRaw, false, "Failed to parse unified diff: ${t.message}")
    }

    val hunks = patch.deltas.size
    val touchedLines = patch.deltas.sumOf { it.target.lines.size + it.source.lines.size }

    val resultLines = try {
        DiffUtils.patch(original, patch)
    } catch (t: Throwable) {
        return ApplyResult(targetRaw, false, "Failed to apply patch: ${t.message}")
    }

    // NO-OP: identical content — don't create a .bak and don't overwrite
    if (resultLines == original) {
        return ApplyResult(targetRaw, false, "No-op: patch produced identical content (hunks=$hunks, touchedLines=$touchedLines)")
    }

    if (dryRun) {
        return ApplyResult(targetRaw, true, "DRY-RUN: would change file (hunks=$hunks, touchedLines=$touchedLines)")
    }

    // Backup only if content really changes
    val backup = file.toPath().resolveSibling(file.name + ".bak")
    Files.copy(file.toPath(), backup, StandardCopyOption.REPLACE_EXISTING)
    file.writeText(resultLines.joinToString(separator = System.lineSeparator()))

    return ApplyResult(targetRaw, true, "Patched successfully (hunks=$hunks, touchedLines=$touchedLines)", backup.toString())
}

/** Create a unified diff from new full file content (fallback if model returns rewrites). */
fun diffFromNewContent(rootDir: File, filePath: String, newContent: String): LlmDiff {
    val file = resolveTargetFile(rootDir, filePath) ?: File(rootDir, filePath)
    val oldLines = if (file.exists()) file.readLines() else emptyList()
    val newLines = newContent.replace("\r\n", "\n").replace('\r', '\n').lines()
    val patch = DiffUtils.diff(oldLines, newLines)
    val unified = UnifiedDiffUtils.generateUnifiedDiff(
        filePath, filePath, oldLines, patch, 3
    ).joinToString("\n")
    return LlmDiff(filePath, unified)
}

/** Try to infer target path from unified diff headers. */
private fun guessTargetPath(unifiedLines: List<String>): String? {
    val plus = unifiedLines.firstOrNull { it.startsWith("+++ ") } ?: return null
    var raw = plus.removePrefix("+++ ").trim()
    if (raw.contains('\t')) raw = raw.substringBefore('\t')
    if (raw.contains(' ')) raw = raw.substringBefore(' ')
    raw = raw.removePrefix("a/").removePrefix("b/")
    raw = raw.removePrefix("\"").removeSuffix("\"")
    return raw
}