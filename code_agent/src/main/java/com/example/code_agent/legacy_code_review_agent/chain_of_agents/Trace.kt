package com.example.code_agent.legacy_code_review_agent.chain_of_agents

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Simple file-based trace for LLM requests/responses. */
object Trace {
    private val tsFmt = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
    var rootDir: File = File("out/chain/_trace")

    @Synchronized fun save(kind: String, name: String, text: String) {
        val dir = rootDir.resolve(kind)
        dir.mkdirs()
        val file = dir.resolve("${LocalDateTime.now().format(tsFmt)}_${sanitize(name)}.txt")
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    private fun sanitize(s: String): String = s.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
}
