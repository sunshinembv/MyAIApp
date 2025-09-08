package com.example.code_agent.legacy_code_review_agent.chain_of_agents

import com.example.code_agent.legacy_code_review_agent.LlmDiff
import com.example.code_agent.legacy_code_review_agent.LlmResponse
import com.example.code_agent.legacy_code_review_agent.LlmRewrite
import com.squareup.moshi.Moshi

/**
 * Parser: accepts alt key names like patches/changes, filepath/path, diff/patch, content/code.
 */
object LlmResponseFlexible {
    @Suppress("UNCHECKED_CAST")
    fun parse(moshi: Moshi, json: String): LlmResponse? {
        val any = runCatching { moshi.adapter(Any::class.java).fromJson(json) }.getOrNull()
        val map = (any as? Map<*, *>)?.mapKeys { (k, _) -> k?.toString()?.lowercase() ?: "" } ?: return null

        /*fun <T> getList(vararg names: String): List<T> = names.asSequence()
            .mapNotNull { map[it] as? List<*> }
            .firstOrNull()?.filterIsInstance<T>() ?: emptyList()*/

        fun getFirstList(vararg names: String): List<Map<*, *>> = names.asSequence()
            .mapNotNull { map[it] as? List<*> }
            .firstOrNull()?.filterIsInstance<Map<*, *>>() ?: emptyList()

        val diffsRaw = getFirstList("diffs", "patches", "changes")
        val rewritesRaw = getFirstList("rewrites", "files", "updates", "replacements")

        fun str(m: Map<*, *>, vararg keys: String): String? = keys.asSequence()
            .mapNotNull { k -> m[k] as? String ?: m[k.toString()] as? String }
            .firstOrNull()

        val diffs = diffsRaw.mapNotNull { m ->
            val file = str(m, "file", "path", "filepath", "target", "targetpath") ?: return@mapNotNull null
            val uni = str(m, "unified_diff", "diff", "patch", "unifiedDiff") ?: return@mapNotNull null
            LlmDiff(file = file, unified_diff = uni)
        }
        val rewrites = rewritesRaw.mapNotNull { m ->
            val file = str(m, "file", "path", "filepath", "target", "targetpath") ?: return@mapNotNull null
            val code = str(m, "new_content", "content", "code", "newCode", "text") ?: return@mapNotNull null
            LlmRewrite(file = file, new_content = code)
        }

        if (diffs.isEmpty() && rewrites.isEmpty()) return null
        return LlmResponse(diffs = diffs, rewrites = rewrites)
    }
}
