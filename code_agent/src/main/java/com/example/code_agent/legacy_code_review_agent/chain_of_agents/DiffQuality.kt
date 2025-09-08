package com.example.code_agent.legacy_code_review_agent.chain_of_agents

/**
 * Почти‑линтер для unified diff: считает хунки/добавления/удаления,
 * отличает "настоящие" кодовые изменения от комментариев/пустых строк/импортов.
 */
object DiffQuality {
    data class Stats(
        val hunks: Int,
        val added: Int,
        val removed: Int,
        val addedComments: Int,
        val addedBlank: Int,
        val addedImports: Int,
        val removedComments: Int,
        val removedBlank: Int,
        val removedImports: Int,
        val addedCode: Int,
        val removedCode: Int,
    ) {
        val touched: Int get() = added + removed
    }

    enum class Kind { CODE, COMMENT, BLANK, IMPORT }

    fun classifyKotlin(line: String): Kind {
        val t = line.trim()
        if (t.isEmpty()) return Kind.BLANK
        if (t.startsWith("import ")) return Kind.IMPORT
        if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("*/") || t.startsWith("*")) return Kind.COMMENT
        // kdoc start/end lines
        if (t.startsWith("/**") || t == "*/") return Kind.COMMENT
        return Kind.CODE
    }

    fun inspectUnified(unified: String): Stats {
        var hunks = 0
        var add = 0
        var rem = 0
        var addC = 0
        var addB = 0
        var addI = 0
        var remC = 0
        var remB = 0
        var remI = 0

        unified.lineSequence().forEach { raw ->
            val line = raw
            if (line.startsWith("@@")) { hunks++; return@forEach }
            if (line.startsWith("--- ") || line.startsWith("+++ ") || line.startsWith("diff ") || line.startsWith("index ")) return@forEach
            if (line.isEmpty()) return@forEach
            val sign = line.first()
            if (sign != '+' && sign != '-') return@forEach
            // ignore headers like +++ b/file
            if (sign == '+' && line.startsWith("+++ ")) return@forEach
            if (sign == '-' && line.startsWith("--- ")) return@forEach

            val body = line.drop(1)
            val kind = classifyKotlin(body)
            if (sign == '+') {
                add++
                when (kind) {
                    Kind.CODE -> {}
                    Kind.COMMENT -> addC++
                    Kind.BLANK -> addB++
                    Kind.IMPORT -> addI++
                }
            } else {
                rem++
                when (kind) {
                    Kind.CODE -> {}
                    Kind.COMMENT -> remC++
                    Kind.BLANK -> remB++
                    Kind.IMPORT -> remI++
                }
            }
        }
        val addCode = add - (addC + addB + addI)
        val remCode = rem - (remC + remB + remI)
        return Stats(hunks = hunks, added = add, removed = rem,
            addedComments = addC, addedBlank = addB, addedImports = addI,
            removedComments = remC, removedBlank = remB, removedImports = remI,
            addedCode = addCode, removedCode = remCode)
    }

    data class Verdict(val trivial: Boolean, val reason: String)

    /**
     * Считаем дифф тривиальным, если он не меняет (почти) код: только комменты/пустые строки/импорты,
     * или очень маленькое затрагивание кода.
     */
    fun isTrivial(stats: Stats, minCodeTouched: Int = 4): Verdict {
        if (stats.addedCode + stats.removedCode < minCodeTouched) {
            return Verdict(true, "codeTouched=${stats.addedCode + stats.removedCode} < $minCodeTouched")
        }
        if (stats.addedCode == 0 && stats.removedCode == 0) {
            return Verdict(true, "no code changes (comments/imports/whitespace only)")
        }
        return Verdict(false, "ok")
    }
}
