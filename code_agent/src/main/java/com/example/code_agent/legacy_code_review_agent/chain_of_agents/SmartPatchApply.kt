package com.example.code_agent.legacy_code_review_agent.chain_of_agents

import com.example.code_agent.legacy_code_review_agent.ApplyResult
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch
import com.github.difflib.patch.PatchFailedException
import java.io.File

/**
 * Расширенное применение unified‑diff:
 * 1) Нормализуем переводы строк (LF) у файла и диффа.
 * 2) Чиним заголовки '--- a/<path>' / '+++ b/<path>' под фактический targetPath.
 * 3) Пробуем java‑diff‑utils (прямое применение).
 * 4) Fallback #1: whitespace (посрочно, по подпоследовательности исходных строк).
 * 5) Fallback #2: anchor‑based — ищем якорь (первая/последняя непустая исходная строка),
 *    допускаем различия в пробелах и форматировании. Хорошо работает для «вставить KDoc перед функцией».
 */
object SmartPatchApply {

    fun applyUnifiedDiffSmart(root: File, targetPath: String, unifiedDiffRaw: String): ApplyResult {
        val file = File(root, targetPath)
        if (!file.exists()) return ApplyResult(targetPath, ok = false, message = "Target file does not exist")

        val backupPath = file.absolutePath + ".bak"
        val originalText = normalizeEol(file.readText())
        val fixedUnified = normalizeHeaders(targetPath, normalizeEol(unifiedDiffRaw))

        // 1) Попытка: обычное применение через UnifiedDiffUtils/DiffUtils
        val r1 = tryApplyWithLib(originalText, fixedUnified)
        if (r1 != null) {
            File(backupPath).writeText(originalText)
            file.writeText(r1)
            return ApplyResult(targetPath, ok = true, message = "Patched successfully (direct)", backupPath = backupPath)
        }

        // Разберём дельты один раз — пригодится для fallback'ов
        val parsed = runCatching { UnifiedDiffUtils.parseUnifiedDiff(fixedUnified.lineSequence().toList()) }.getOrNull()
            ?: return ApplyResult(targetPath, ok = false, message = "Failed to parse unified diff")

        // 2) Fallback #1: whitespace замена блоков
        val r2 = tryApplyWhitespace(originalText, parsed)
        if (r2 != null) {
            File(backupPath).writeText(originalText)
            file.writeText(r2)
            return ApplyResult(targetPath, ok = true, message = "Patched successfully (whitespace)", backupPath = backupPath)
        }

        // 3) Fallback #2: anchor‑based (первая/последняя непустая исходная строка как якорь)
        val r3 = tryApplyWithAnchors(originalText, parsed)
        if (r3 != null) {
            File(backupPath).writeText(originalText)
            file.writeText(r3)
            return ApplyResult(targetPath, ok = true, message = "Patched successfully (anchor‑based)", backupPath = backupPath)
        }

        return ApplyResult(targetPath, ok = false, message = "Failed to apply patch: CONTENT_DOES_NOT_MATCH_TARGET (after fallbacks)")
    }

    private fun normalizeEol(s: String): String = s.replace("\r\n", "\n").replace('\r', '\n')

    /** Приводим шапки к единому виду, даже если модель указала другой путь/префиксы. */
    private fun normalizeHeaders(targetPath: String, unified: String): String {
        val lines = unified.lineSequence().toMutableList()
        var sawHeader = false
        for (i in 0 until minOf(2, lines.size)) {
            if (lines[i].startsWith("--- ")) { lines[i] = "--- a/$targetPath"; sawHeader = true }
            if (lines[i].startsWith("+++ ")) { lines[i] = "+++ b/$targetPath"; sawHeader = true }
        }
        return if (!sawHeader) "--- a/$targetPath\n+++ b/$targetPath\n" + unified.trimStart() else lines.joinToString("\n")
    }

    private fun tryApplyWithLib(originalText: String, unified: String): String? = try {
        val originalLines = originalText.split('\n')
        val patch = UnifiedDiffUtils.parseUnifiedDiff(unified.lineSequence().toList())
        val patchedLines = DiffUtils.patch(originalLines, patch)
        patchedLines.joinToString("\n")
    } catch (_: PatchFailedException) { null } catch (_: Throwable) { null }

    private fun tryApplyWhitespace(originalText: String, patch: Patch<String>): String? = try {
        val lines = originalText.split('\n').toMutableList()
        for (delta in patch.deltas) {
            val src = delta.source.lines
            val repl = delta.target.lines
            val idx = indexOfSubListWhitespaceInsensitive(lines, src)
            if (idx < 0) break
            repeat(src.size) { if (idx < lines.size) lines.removeAt(idx) }
            for ((k, line) in repl.withIndex()) lines.add(idx + k, line)
        }
        lines.joinToString("\n")
    } catch (_: Throwable) { null }

    // --- Anchor‑based ---

    private fun tryApplyWithAnchors(originalText: String, patch: Patch<String>): String? = try {
        var text = originalText
        for (delta in patch.deltas) {
            val src = delta.source.lines
            val repl = delta.target.lines
            // Попробуем привязаться к первой НЕпустой строке из src, если не вышло — к последней
            val anchor = src.firstOrNull { it.isNotBlank() } ?: src.lastOrNull { it.isNotBlank() } ?: continue
            val anchorIdx = findLineByFlexibleMatch(text, anchor)
            if (anchorIdx < 0) break
            // Заменим участок длиной src.size начиная с anchorIdx на repl
            val all = text.split('\n').toMutableList()
            val start = anchorIdx
            val end = (start + src.size).coerceAtMost(all.size)
            for (i in end - 1 downTo start) all.removeAt(i)
            for ((k, line) in repl.withIndex()) all.add(start + k, line)
            text = all.joinToString("\n")
        }
        text
    } catch (_: Throwable) { null }

    /** Гибкий поиск строки: схлопываем пробелы и допускаем вариации в скобках/пробелах. */
    private fun findLineByFlexibleMatch(text: String, probe: String): Int {
        val probeNorm = normWS(probe)
        val lines = text.split('\n')
        for ((i, ln) in lines.withIndex()) {
            if (normWS(ln) == probeNorm) return i
            // Доп. попытка: regex с \s+
            if (probe.isNotBlank()) {
                val pat = Regex(Regex.escape(probe).replace("\\\\s+", "\\\\s+").replace("\\ ", "\\s+"))
                if (pat.containsMatchIn(ln)) return i
            }
        }
        return -1
    }

    private fun normWS(s: String): String = s.trim().replace(Regex("\\s+"), " ")

    /** Ищем подпоследовательность строк, игнорируя различия в пробельных символах. */
    private fun indexOfSubListWhitespaceInsensitive(hay: List<String>, needle: List<String>): Int {
        if (needle.isEmpty()) return 0
        val hayN = hay.map { normWS(it) }
        val neeN = needle.map { normWS(it) }
        outer@ for (i in 0..(hayN.size - neeN.size)) {
            for (j in neeN.indices) {
                if (hayN[i + j] != neeN[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
