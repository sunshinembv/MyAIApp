package com.example.myaiapp.chat.voice

object UtteranceChunker {
    /** Режем по пунктуации; если фраза длинная — дробим по словам.
     *  150–220 символов — комфортно для Android TTS.
     */
    fun split(text: String, maxLen: Int = 220): List<String> {
        val sentences = Regex("""(?<=[.!?…])\s+""")
            .split(text.trim())
            .flatMap { s -> if (s.length <= maxLen) listOf(s) else hardWrap(s, maxLen) }
        return sentences.filter { it.isNotBlank() }
    }

    private fun hardWrap(s: String, maxLen: Int): List<String> {
        val out = mutableListOf<String>()
        val words = s.split(Regex("\\s+"))
        var cur = StringBuilder()
        for (w in words) {
            if (cur.isNotEmpty() && cur.length + 1 + w.length > maxLen) {
                out += cur.toString()
                cur = StringBuilder()
            }
            if (cur.isNotEmpty()) cur.append(' ')
            cur.append(w)
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out
    }
}