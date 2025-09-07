package com.example.code_agent.legacy_code_review_agent.chain_of_agents

/**
 * Извлекает ПОСЛЕДНИЙ валидный JSON-объект из произвольного текста модели.
 * Понимает строки, экранирование и игнорирует markdown-кодовые блоки.
 */
object JsonExtract {
    fun stripCodeFences(text: String): String = text
        .replace(Regex("```(json|JSON)?\\s*"), "")
        .replace("```", "")
        .trim()

    /** Возвращает самый последний корректно сбалансированный JSON-объект в тексте или null. */
    fun lastJsonObject(text: String): String? {
        val s = stripCodeFences(text)
        val n = s.length
        var bestStart = -1
        var bestEnd = -1
        var i = 0
        while (i < n) {
            if (s[i] == '{') {
                var j = i
                var depth = 0
                var inStr = false
                var esc = false
                while (j < n) {
                    val c = s[j]
                    if (inStr) {
                        if (esc) esc = false else if (c == '\\') esc = true else if (c == '"') inStr = false
                    } else {
                        when (c) {
                            '"' -> inStr = true
                            '{' -> depth++
                            '}' -> {
                                depth--
                                if (depth == 0) { bestStart = i; bestEnd = j + 1; break }
                            }
                        }
                    }
                    j++
                }
                i = if (bestEnd > 0) bestEnd else j + 1
            } else i++
        }
        return if (bestStart >= 0 && bestEnd > bestStart) s.substring(bestStart, bestEnd) else null
    }
}
