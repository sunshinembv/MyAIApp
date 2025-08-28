package com.example.code_agent

import com.example.code_agent.LlmCodeFixer.replaceSlice
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

object LlmCodeFixer {
    private val codeFenceRegex =
        Regex("```(?:kotlin)?\\s*([\\s\\S]*?)\\s*```", setOf(RegexOption.MULTILINE))

    suspend fun fixFileWithLlm(
        api: OpenRouterApi,
        model: String,
        filePath: String,
        taskNote: String
    ): String {
        val src = java.io.File(filePath).readText()

        val systemPrompt = """
            You are a senior Android/Kotlin engineer.
            Goal: fix a real bug OR perform a safe micro-refactor with clear benefit.
            Constraints:
            - Return the **ENTIRE corrected file only** inside a ```kotlin``` block.
            - No explanations, no diffs, no extra text.
            - Keep package/imports and structure. No new deps or CI changes.
            - Keep style as-is (no mass reformatting/renaming).
        """.trimIndent()

        val userPrompt = buildString {
            appendLine("Task note: $taskNote")
            appendLine()
            appendLine("Analyze and fix this FULL file. Return only the entire corrected file wrapped in ```kotlin```:")
            appendLine()
            appendLine("```kotlin")
            appendLine(src)
            appendLine("```")
        }

        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            temperature = 0.2,
            stream = false
        )

        val resp = api.chatCompletions(request)

        TokenTally.add(resp.usage)
        TokenTally.printlnStep("full_file", resp.usage)

        val content = resp.choices.firstOrNull()?.message?.content?.trim().orEmpty()

        val code =
            codeFenceRegex.find(content)?.groups?.get(1)?.value?.trim()
                ?: Regex("```\\s*([\\s\\S]*?)\\s*```", RegexOption.MULTILINE)
                    .find(content)?.groups?.get(1)?.value?.trim()
                ?: content

        require(code.isNotBlank()) { "LLM returned empty code; aborting to avoid overwriting with empty content." }

        // перезаписываем файл
        Files.write(Paths.get(filePath), code.toByteArray(StandardCharsets.UTF_8))
        return code
    }

    /** По имени метода/класса. */
    fun extractMethodSlice(fileText: String, symbol: String): MethodSlice {
        val regex = Regex("""(?m)^\s*(fun\s+$symbol\s*\(|class\s+$symbol(\s|\{|:)|object\s+$symbol(\s|\{|:))""")
        val m = regex.find(fileText) ?: error("Не нашёл TARGET: $symbol")
        return sliceFromMatch(fileText, m.range.first)
    }

    /** По номеру строки (1-based), чтобы привязаться к стектрейсу). */
    fun extractMethodSliceByLine(fileText: String, targetLine: Int): MethodSlice {
        val lines = fileText.split('\n')
        val lineIdx = (targetLine - 1).coerceIn(0, lines.lastIndex)
        // Ищем вверх ближайшую сигнатуру fun/ class / object
        var scanIdx = lineStartIndex(fileText, lineIdx)
        val headText = fileText.substring(0, scanIdx)
        val funMatch = Regex("""(?m)^\s*fun\s+[A-Za-z0-9_]+\s*\(""").findAll(headText).lastOrNull()
        val classMatch = Regex("""(?m)^\s*(class|object)\s+[A-Za-z0-9_]+""").findAll(headText).lastOrNull()
        val anchor = listOfNotNull(funMatch, classMatch).maxByOrNull { it.range.first }
            ?: error("Не удалось определить область метода/класса для строки $targetLine")
        return sliceFromMatch(fileText, anchor.range.first)
    }

    private fun lineStartIndex(text: String, lineIdx: Int): Int {
        var idx = 0
        var line = 0
        while (idx < text.length && line < lineIdx) {
            if (text[idx] == '\n') line++
            idx++
        }
        return idx
    }

    private fun sliceFromMatch(fileText: String, sigStart: Int): MethodSlice {
        val braceStart = fileText.indexOf('{', sigStart)
        require(braceStart >= 0) { "У TARGET отсутствует { блок" }
        var i = braceStart
        var depth = 0
        while (i < fileText.length) {
            when (fileText[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val signature = fileText.substring(sigStart, braceStart).trim()
                        val body = fileText.substring(braceStart, i + 1)
                        return MethodSlice(signature, body, sigStart, i + 1)
                    }
                }
            }
            i++
        }
        error("Несбалансированные скобки у TARGET")
    }

    fun replaceSlice(fileText: String, slice: MethodSlice, newWhole: String): String {
        return buildString {
            append(fileText.substring(0, slice.startIdx))
            append(newWhole)
            append(fileText.substring(slice.endIdx))
        }
    }
}

data class MethodSlice(val signature: String, val body: String, val startIdx: Int, val endIdx: Int)


/** Самый экономный FIX: отправляем только целевой метод/класс. */
suspend fun fixSliceWithLlm(
    api: OpenRouterApi,
    model: String,
    filePath: String,
    ask: String,
    slice: MethodSlice
): String {
    val system = "You are a Kotlin engineer. Fix exactly as asked.\nOutput only the full corrected target in ```kotlin```. No analysis.\nNo unrelated changes."
    val user = buildString {
        appendLine("ASK: $ask")
        appendLine("TARGET: ${slice.signature.lineTrim()}")
        appendLine("CODE:")
        appendLine("```kotlin")
        appendLine(slice.body.stripCommentsAndBlankLines())
        appendLine("```")
        appendLine("RETURN: full corrected target in ```kotlin``` only.")
    }

    val req = ChatRequest(
        model = model,
        messages = listOf(ChatMessage("system", system), ChatMessage("user", user)),
        temperature = 0.1,
        stream = false
    )
    val resp = api.chatCompletions(req)

    TokenTally.add(resp.usage)
    TokenTally.printlnStep("fix_slice", resp.usage)

    val content = resp.choices.firstOrNull()?.message?.content.orEmpty()
    val out = fence.find(content)?.groupValues?.get(1)?.trim()?.ifEmpty { null } ?: content.trim()

    require(out.startsWith("fun ") || out.startsWith("class ") || out.startsWith("object ")) {
        "LLM returned unexpected format"
    }

    val src = java.io.File(filePath).readText()
    val updated = replaceSlice(src, slice, out)
    Files.write(Paths.get(filePath), updated.toByteArray(StandardCharsets.UTF_8))
    return updated
}

private fun String.lineTrim(): String = this.replace(Regex("\\s+"), " ").trim()

val fence = Regex("```(?:kotlin)?\\s*([\\s\\S]*?)\\s*```", RegexOption.MULTILINE)

private fun String.stripCommentsAndBlankLines(): String {
    // простая экономия токенов: убираем /* ... */ и // ... и пустые строки
    val noBlock = this.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
    return noBlock.lines()
        .map { it.replace(Regex("""\s*//.*$"""), "") }
        .filter { it.isNotBlank() }
        .joinToString("\n")
}

private val DIFF_HEADER = Regex("""(?m)^(---|\+\+\+) """)
private val HUNK_HEADER = Regex("""(?m)^@@\s*-\d+(?:,\d+)?\s+\+\d+(?:,\d+)?\s*@@""")

/** Просим LLM вернуть unified diff с лимитом изменений и применяем его. */
suspend fun fixWithStrictUnifiedDiff(
    api: OpenRouterApi,
    model: String,
    filePath: String,
    ask: String,
    slice: MethodSlice,
    maxChangedLines: Int = 6, // сколько строк разрешаем менять (+/-)
    contextLines: Int = 3
): String {
    val system = """
        You are a Kotlin engineer.
        Return ONLY a valid unified diff patch for the given file path.
        Constraints: at most $maxChangedLines changed lines (+/-) total. Keep unrelated lines verbatim. No reformatting.
        Include headers: --- a/$filePath and +++ b/$filePath and a single @@ hunk.
        No extra text.
    """.trimIndent()

    // Даём модели локальный контекст (слайс), чтобы не слала большой патч
    val user = buildString {
        appendLine("ASK: $ask")
        appendLine("FILE: $filePath")
        appendLine("CONTEXT: $contextLines")
        appendLine("REFERENCE SLICE (for guidance only):")
        appendLine("```kotlin")
        appendLine(slice.body) // можно stripCommentsAndBlankLines() если хочешь минимум токенов
        appendLine("```")
        appendLine("RETURN: unified diff only.")
    }

    val req = ChatRequest(
        model = model,
        messages = listOf(ChatMessage("system", system), ChatMessage("user", user)),
        temperature = 0.1,
        stream = false
    )

    val resp = api.chatCompletions(req)

    TokenTally.add(resp.usage)
    TokenTally.printlnStep("unified_diff", resp.usage)

    val patch = resp.choices.firstOrNull()?.message?.content?.trim().orEmpty()

    require(DIFF_HEADER.containsMatchIn(patch) && HUNK_HEADER.containsMatchIn(patch)) {
        "Model didn't return a unified diff"
    }
    val changes = countChangedLines(patch)
    require(changes <= maxChangedLines) { "Patch too large: $changes lines > $maxChangedLines" }

    val applied = applyPatchInsideSlice(filePath, slice, patch)

    require(applied) { "Failed to apply patch" }
    return java.io.File(filePath).readText()
}

/** Подсчитываем изменённые строки (+/-), игнорируя заголовки и @@. */
private fun countChangedLines(patch: String): Int =
    patch.lineSequence().count { line ->
        when {
            line.startsWith("---") || line.startsWith("+++") -> false
            line.startsWith("@@") -> false
            line.startsWith("+") && !line.startsWith("+++") -> true
            line.startsWith("-") && !line.startsWith("---") -> true
            else -> false
        }
    }

/** Простой fallback: применяем патч ТОЛЬКО внутри найденного слайса (без внешних зависимостей). */
private fun applyPatchInsideSlice(filePath: String, slice: MethodSlice, patch: String): Boolean {
    // Вытащим только сам хунк из диффа
    val hunk = HUNK_HEADER.find(patch)?.let { start ->
        val idx = start.range.first
        patch.substring(idx)
    } ?: return false

    // Разбираем хунк (без проверки всех краёв – достаточно для малых правок)
    val lines = hunk.lineSequence().toList()
    val bodyLines = slice.body.lines().toMutableList()

    // Ищем якорь: первые 2–3 контекстных строки после @@ с пробегом
    val startIdx = lines.indexOfFirst { it.startsWith("@@") }
    if (startIdx == -1) return false
    var i = startIdx + 1

    // Построим «желательное» новое тело из hunks, применяя +/-/ ' ' к текущему bodyLines по скользящему поиску
    // Для надёжности работаем в простом режиме: собираем newBody из контекста и добавлений.
    val newBody = mutableListOf<String>()
    var cursor = 0

    while (i < lines.size) {
        val l = lines[i]
        when {
            l.startsWith(" ") -> {
                val ctx = l.removePrefix(" ")
                // продвигаем cursor до первой строки с совпадающим контекстом
                val pos = bodyLines.indexOfFirstFrom(cursor) { it.trimEnd() == ctx.trimEnd() }
                if (pos == -1) return false
                // переносим нетронутые строки до pos
                while (cursor < pos) { newBody += bodyLines[cursor]; cursor++ }
                // переносим саму контекстную строку
                newBody += bodyLines[pos]; cursor = pos + 1
            }
            l.startsWith("-") -> {
                val rm = l.removePrefix("-")
                // удаляем совпадающую ближайшую строку
                val pos = bodyLines.indexOfFirstFrom(cursor) { it.trimEnd() == rm.trimEnd() }
                if (pos == -1) {
                    // бывает, что удаление сразу рядом: попробуем по текущему
                    if (cursor < bodyLines.size && bodyLines[cursor].trimEnd() == rm.trimEnd()) {
                        cursor++ // пропускаем одну строку
                    } else return false
                } else {
                    // добавим нетронутые до pos
                    while (cursor < pos) { newBody += bodyLines[cursor]; cursor++ }
                    // "удаляем" pos: просто сдвигаем cursor
                    cursor = pos + 1
                }
            }
            l.startsWith("+") -> {
                val add = l.removePrefix("+")
                newBody += add
            }
            else -> { /* ignore */ }
        }
        i++
    }
    // добавим оставшийся хвост
    while (cursor < bodyLines.size) { newBody += bodyLines[cursor]; cursor++ }

    // Сохраняем файл с заменённым слайсом
    val src = java.io.File(filePath).readText()
    val updated = replaceSlice(src, slice, slice.signature + "\n" + newBody.joinToString("\n"))
    java.nio.file.Files.write(java.nio.file.Paths.get(filePath), updated.toByteArray(Charsets.UTF_8))
    return true
}

private fun <T> List<T>.indexOfFirstFrom(from: Int, predicate: (T) -> Boolean): Int {
    var i = from
    while (i < this.size) { if (predicate(this[i])) return i; i++ }
    return -1
}