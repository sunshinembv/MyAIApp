package com.example.code_agent.legacy_code_review_agent

import java.io.File
import kotlin.system.exitProcess
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.source
import java.security.MessageDigest
import java.util.Properties

/**
 * CLI: legacy code scanner + LLM review + optional patch apply
 * Example:
 *   ./gradlew :legacy-agent:run --args="--root ../MyProject \
 *     --out out/report.md --json out/report.json \
 *     --llm true --apply false \
 *     --llm-include-code true --llm-files 3 --llm-max-chars 120000 \
 *     --llm-temperature 0.0 --llm-retry true \
 *     --llm-model deepseek/deepseek-r1:free"
 *
 * API key is read from env OPENROUTER_API_KEY unless --llm-api-key is passed.
 */

// ======== CLI ========

data class Args(
    val root: File,
    val outMd: File?,
    val outJson: File?,
    val withLlm: Boolean,
    val maxSnippetLines: Int,
    // LLM + patch options
    val applyPatches: Boolean,
    val llmEndpoint: String,
    val llmModel: String,
    val llmApiKey: String?,
    val llmTemperature: Double,
    val llmMaxTokens: Int,
    val llmIncludeCode: Boolean,
    val llmMaxFiles: Int,
    val llmMaxChars: Int,
    val llmRetryIfEmpty: Boolean,
    val debugDiffs: Boolean,
    val llmForceRewriteOnEmpty: Boolean,
)

fun parseArgs(argv: Array<String>): Args {
    var root: File? = null
    var outMd: File? = null
    var outJson: File? = null
    var withLlm = false
    var maxSnippetLines = 60

    var applyPatches = false
    var llmEndpoint = System.getenv("OPENROUTER_ENDPOINT") ?: "https://openrouter.ai/api/v1/chat/completions"
    var llmModel = System.getenv("OPENROUTER_MODEL") ?: "deepseek/deepseek-r1:free"
    var llmApiKey: String? = System.getenv("OPENROUTER_API_KEY")
    var llmTemperature = 0.0
    var llmMaxTokens = 2000
    var llmIncludeCode = true
    var llmMaxFiles = 3
    var llmMaxChars = 120_000
    var llmRetryIfEmpty = true
    var debugDiffs = true                  // NEW: печатать мета-инфо по диффам
    var llmForceRewriteOnEmpty = true

    val it = argv.iterator()
    while (it.hasNext()) {
        when (val a = it.next()) {
            "--root" -> root = File(it.next())
            "--out" -> outMd = File(it.next())
            "--json" -> outJson = File(it.next())
            "--llm" -> withLlm = it.next().toBooleanStrict()
            "--max-snippet-lines" -> maxSnippetLines = it.next().toInt()
            // NEW
            "--apply" -> applyPatches = it.next().toBooleanStrict()
            "--llm-endpoint" -> llmEndpoint = it.next()
            "--llm-model" -> llmModel = it.next()
            "--llm-api-key" -> llmApiKey = it.next()
            "--llm-temperature" -> llmTemperature = it.next().toDouble()
            "--llm-max-tokens" -> llmMaxTokens = it.next().toInt()
            "--llm-include-code" -> llmIncludeCode = it.next().toBooleanStrict()
            "--llm-files" -> llmMaxFiles = it.next().toInt()
            "--llm-max-chars" -> llmMaxChars = it.next().toInt()
            "--llm-retry" -> llmRetryIfEmpty = it.next().toBooleanStrict()
            "--debug-diffs" -> debugDiffs = it.next().toBooleanStrict()
            "--llm-force-rewrite-on-empty" -> llmForceRewriteOnEmpty = it.next().toBooleanStrict()
            else -> error("Unknown arg: $a")
        }
    }
    if (root == null || !root!!.exists()) error("--root dir not found")
    return Args(root!!, outMd, outJson, withLlm, maxSnippetLines, applyPatches, llmEndpoint, llmModel, llmApiKey, llmTemperature, llmMaxTokens, llmIncludeCode, llmMaxFiles, llmMaxChars, llmRetryIfEmpty, debugDiffs, llmForceRewriteOnEmpty)
}

// ======== Report model ========

data class FileSummary(
    val path: String,
    val lines: Int,
    val functions: Int,
    val maxFunctionLen: Int,
    val nestedBlocksMaxDepth: Int,
    val notNullBangCount: Int,
)

data class DuplicateCluster(
    val representative: String,
    val occurrences: List<Occurrence>,
    val approxLines: Int,
) { data class Occurrence(val file: String, val lineFrom: Int, val lineTo: Int) }

data class Dependency(val notation: String, val where: String)

data class ToolFinding(val tool: String, val summary: String, val htmlReport: String?)

data class ReviewItem(
    val category: String, // duplicates | readability | dependencies | style | risk
    val severity: String, // low|med|high
    val title: String,
    val details: String,
    val files: List<String> = emptyList(),
)

data class ReviewReport(
    val root: String,
    val files: List<FileSummary>,
    val duplicates: List<DuplicateCluster>,
    val dependencies: List<Dependency>,
    val toolFindings: List<ToolFinding>,
    val suggestions: List<ReviewItem>,
)

// ======== Utils ========

private fun readText(file: File): String = file.inputStream().source().buffer().use { it.readUtf8() }

private fun stripCommentsAndNormalize(code: String): String {
    // Very rough: drop /* */ and // comments, then collapse whitespace
    val noBlock = code.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ")
    val noLine = noBlock.replace(Regex("//.*"), " ")
    return noLine.replace(Regex("\\s+"), " ").trim()
}

private fun tokenizeForShingles(s: String): List<String> = Regex("[A-Za-z0-9_]+|\\n|\\S")
    .findAll(s)
    .map { it.value }
    .toList()

private fun sha1(bytes: ByteArray): String = MessageDigest.getInstance("SHA-1").digest(bytes)
    .joinToString("") { "%02x".format(it) }

// k-gram fingerprints with simple winnowing
private fun fingerprints(tokens: List<String>, k: Int = 12, w: Int = 8): List<Pair<String, Int>> {
    if (tokens.size < k) return emptyList()
    val hashes = ArrayList<Int>()
    var h = 0
    val base = 911382323
    val mod = 1_000_000_007
    var pow = 1
    repeat(k) { pow = (pow * base) % mod }

    fun tokHash(t: String) = t.fold(0) { acc, c -> (acc * 131 + c.code) % mod }

    for (i in 0 until k) h = (h * base + tokHash(tokens[i])) % mod
    hashes.add(h)
    for (i in k until tokens.size) {
        h = ((h * base - (tokHash(tokens[i - k]) * pow % mod) + mod) % mod + tokHash(tokens[i])) % mod
        hashes.add(h)
    }

    val res = mutableListOf<Pair<String, Int>>()
    var lastIdx = -1
    for (i in 0..hashes.size - w) {
        var minVal = Int.MAX_VALUE
        var minPos = i
        for (j in i until i + w) {
            val v = hashes[j]
            if (v <= minVal) { minVal = v; minPos = j }
        }
        if (minPos != lastIdx) {
            res += sha1("$minVal".toByteArray()) to minPos
            lastIdx = minPos
        }
    }
    return res
}

// ======== Scanning ========

private fun kotlinJavaFiles(root: File): List<File> = root.walkTopDown()
    .filter { it.isFile && (it.extension in setOf("kt", "kts", "java", "gradle")) }
    .toList()

private fun summarizeFile(file: File, base: File): FileSummary {
    val text = readText(file)
    val lines = text.lines()
    val funcs = Regex("(^|\\s)fun\\s+", RegexOption.MULTILINE).findAll(text).count()
    val funcLens = Regex("(^|\\s)fun\\s+.*?\n", RegexOption.DOT_MATCHES_ALL)
        .findAll(text)
        .map { match ->
            val start = match.range.first
            val end = text.indexOf("\nfun ", start + 1).let { if (it == -1) text.length else it }
            text.substring(start, end).lines().size
        }.toList()
    val maxFn = funcLens.maxOrNull() ?: 0

    var depth = 0; var maxDepth = 0
    lines.forEach { ln ->
        depth += ln.count { it == '{' }
        maxDepth = maxOf(maxDepth, depth)
        depth -= ln.count { it == '}' }
    }

    val notNullBangs = Regex("!!").findAll(text).count()

    // store path relative to the provided base (args.root)
    val rel = try { file.relativeTo(base).path } catch (_: Throwable) { file.absolutePath }
    val norm = rel.replace('\\', '/')

    return FileSummary(
        path = norm,
        lines = lines.size,
        functions = funcs,
        maxFunctionLen = maxFn,
        nestedBlocksMaxDepth = maxDepth,
        notNullBangCount = notNullBangs,
    )
}

private fun findDuplicates(files: List<File>, maxSnippetLines: Int): List<DuplicateCluster> {
    data class Hit(val file: File, val lineFrom: Int, val lineTo: Int)
    val index = mutableMapOf<String, MutableList<Hit>>()

    for (f in files) {
        val raw = readText(f)
        val norm = stripCommentsAndNormalize(raw)
        val tokens = tokenizeForShingles(norm)
        val fps = fingerprints(tokens)
        val lineBreakIdx = run {
            val arr = mutableListOf<Int>()
            var pos = 0
            for (ln in raw.lines()) { arr += pos; pos += ln.length + 1 }
            arr + pos
        }
        fun tokenPosToLine(tp: Int): Int {
            var chars = 0
            tokens.take(tp).forEach { chars += it.length + 1 }
            val i = lineBreakIdx.binarySearch(chars).let { if (it < 0) -it - 1 else it }
            return i.coerceAtLeast(1)
        }
        fps.forEach { (fp, pos) ->
            val ln = tokenPosToLine(pos)
            index.getOrPut(fp) { mutableListOf() }.add(Hit(f, ln, (ln + maxSnippetLines)))
        }
    }

    val clusters = index.values
        .filter { hits -> hits.map { it.file }.distinct().size >= 2 }
        .map { hits ->
            val rep = hits.first()
            DuplicateCluster(
                representative = "${rep.file.name}:${rep.lineFrom}…${rep.lineTo}",
                occurrences = hits.map { DuplicateCluster.Occurrence(it.file.path, it.lineFrom, it.lineTo) },
                approxLines = maxSnippetLines
            )
        }
        .distinctBy { it.occurrences.map { o -> o.file + ":" + o.lineFrom }.sorted().joinToString() }
    return clusters.take(50)
}

// ======== Dependencies ========

private fun findDependencies(root: File): List<Dependency> {
    val gradleFiles = root.walkTopDown().filter { it.isFile && (it.name.endsWith("build.gradle") || it.name.endsWith("build.gradle.kts") || it.name == "libs.versions.toml") }.toList()
    val deps = mutableListOf<Dependency>()
    for (f in gradleFiles) {
        val t = readText(f)
        Regex("implementation\\(\\\"([^\\\"]+:([^\\\"]+):([^\\\"]+))\\\"\\)")
            .findAll(t).forEach { m -> deps += Dependency(m.groupValues[1], f.path) }
        Regex("api\\(\\\"([^\\\"]+:([^\\\"]+):([^\\\"]+))\\\"\\)")
            .findAll(t).forEach { m -> deps += Dependency(m.groupValues[1], f.path) }
        Regex("implementation\\((libs\\.[^)]+)\\)").findAll(t).forEach { m -> deps += Dependency(m.groupValues[1], f.path) }
    }
    return deps.distinct()
}

// ======== External tool reports ========

private fun tryReadToolReports(root: File): List<ToolFinding> {
    val finds = mutableListOf<ToolFinding>()
    val detektHtml = root.resolve("build/reports/detekt/detekt.html")
    val detektMd = root.resolve("build/reports/detekt/detekt.md")
    if (detektHtml.exists()) finds += ToolFinding("detekt", "detekt HTML report", detektHtml.absolutePath)
    if (detektMd.exists()) finds += ToolFinding("detekt", "detekt Markdown report", detektMd.absolutePath)

    val ktlintHtml = root.resolve("build/reports/ktlint/ktlint.html")
    if (ktlintHtml.exists()) finds += ToolFinding("ktlint", "ktlint HTML report", ktlintHtml.absolutePath)

    return finds
}

// ======== Heuristic suggestions (pre-LLM) ========

private fun suggestHeuristics(files: List<FileSummary>, dups: List<DuplicateCluster>, deps: List<Dependency>): List<ReviewItem> {
    val items = mutableListOf<ReviewItem>()
    files.filter { it.maxFunctionLen >= 80 }.forEach { f ->
        items += ReviewItem(
            category = "readability",
            severity = if (f.maxFunctionLen >= 200) "high" else "med",
            title = "Слишком длинные функции в ${f.path}",
            details = "Макс длина функции ≈ ${f.maxFunctionLen} строк. Рекомендуется Extract Method/Compose, упростить ветвления, декомпозировать.",
            files = listOf(f.path)
        )
    }
    files.filter { it.nestedBlocksMaxDepth >= 6 }.forEach { f ->
        items += ReviewItem("readability", "med", "Глубокая вложенность в ${f.path}", "Макс глубина = ${f.nestedBlocksMaxDepth}. Рассмотрите ранний возврат/guard‑clauses.", listOf(f.path))
    }
    files.filter { it.notNullBangCount >= 3 }.forEach { f ->
        items += ReviewItem("risk", "med", "Много !! (Not‑null‑assert) в ${f.path}", "Найдены ${f.notNullBangCount} восклицательных оператора !! — потенциальные NPE.", listOf(f.path))
    }
    if (dups.isNotEmpty()) items += ReviewItem("duplicates", "med", "Повторы кода", "Найдено кластеров: ${dups.size}. Вынесите общие части в функции/утилиты.")
    if (deps.any()) items += ReviewItem("dependencies", "low", "Проверка актуальности зависимостей", "Запустите Gradle task `dependencyUpdates` (плагин com.github.ben‑manes.versions) и обновите устаревшие пакеты.")
    return items
}

// ======== Markdown ========

private fun renderMarkdown(r: ReviewReport): String = buildString {
    appendLine("# Legacy Review Report")
    appendLine()
    appendLine("Root: `${r.root}`")
    appendLine("Date: ${java.time.LocalDate.now()}")
    appendLine()

    appendLine("## Summary")
    appendLine("- Files scanned: ${r.files.size}")
    appendLine("- Duplicate clusters: ${r.duplicates.size}")
    appendLine("- Dependencies found: ${r.dependencies.size}")
    appendLine()

    appendLine("## Top Findings")
    r.suggestions.take(20).forEach { f ->
        appendLine("- **[${f.severity.uppercase()} · ${f.category}]** ${f.title} — ${f.details}")
    }
    appendLine()

    appendLine("## Hot Files (high complexity)")
    r.files.sortedByDescending { it.maxFunctionLen }.take(20).forEach { f ->
        appendLine("- ${f.path} — lines=${f.lines}, functions=${f.functions}, maxFn=${f.maxFunctionLen}, depth=${f.nestedBlocksMaxDepth}, '!!'=${f.notNullBangCount}")
    }
    appendLine()

    appendLine("## Duplicate Clusters (top 20)")
    r.duplicates.take(20).forEachIndexed { i, d ->
        appendLine("${i+1}. ${d.representative} (~${d.approxLines} lines)")
        d.occurrences.forEach { o -> appendLine("   - ${o.file}:${o.lineFrom}…${o.lineTo}") }
    }
    appendLine()

    appendLine("## Dependencies (raw)")
    r.dependencies.sortedBy { it.notation }.forEach { dep ->
        appendLine("- `${dep.notation}`  _(in ${dep.where})_")
    }
    appendLine()

    if (r.toolFindings.isNotEmpty()) {
        appendLine("## External Tool Reports")
        r.toolFindings.forEach { t ->
            appendLine("- ${t.tool}: ${t.summary}${t.htmlReport?.let { " — $it" } ?: ""}")
        }
        appendLine()
    }
}

// ======== JSON ========

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
private inline fun <reified T> toJson(value: T): String = moshi.adapter(T::class.java).indent("  ").toJson(value)

// ======== LLM helpers ========

private fun loadSystemPrompt(): String {
    val f = File("prompts/legacy_agent_system.txt")
    if (f.exists()) return f.readText()
    return """
        You are LegacyRefactorAgent — a senior Android/Kotlin engineer.
        GOALS: Improve readability, reduce duplication, and modernize dependencies for a safe first pass.
        PROCESS: THINK → VERIFY → ANSWER (only show ANSWER).
        OUTPUT: Return strictly the JSON schema the user provided.
        If unsure: propose advice-only items; do not invent diffs.
    """.trimIndent()
}

private fun pickFilesWithContent(
    root: File,
    report: ReviewReport,
    maxFiles: Int,
    maxChars: Int
): List<Pair<String, String>> {
    val candidates = report.files
        .sortedWith(
            compareByDescending<FileSummary> { it.maxFunctionLen }
                .thenByDescending { it.nestedBlocksMaxDepth }
                .thenByDescending { it.notNullBangCount }
        )
        .map { it.path }
        .distinct()
        .take(maxFiles)

    val result = mutableListOf<Pair<String, String>>()
    var budget = maxChars
    for (p in candidates) {
        val f = File(root, p)
        if (!f.exists() || !f.isFile) continue
        var text = f.readText()
        if (text.length > budget) {
            val head = text.take((budget * 0.7).toInt())
            val tail = text.takeLast((budget * 0.2).toInt())
            text = head + "\n/* ...snip... */\n" + tail
        }
        result += p to text
        budget -= text.length
        if (budget <= 0) break
    }
    return result
}

private fun baseUserPrompt(r: ReviewReport): String = buildString {
    appendLine("PROJECT ROOT: ${r.root}")
    appendLine("SUMMARY: files=${r.files.size}, duplicates=${r.duplicates.size}, deps=${r.dependencies.size}")
    appendLine("TOP HOT FILES:")
    r.files.sortedByDescending { it.maxFunctionLen }.take(10).forEach { f ->
        appendLine("- ${f.path} | lines=${f.lines}, maxFn=${f.maxFunctionLen}, depth=${f.nestedBlocksMaxDepth}, bangs=${f.notNullBangCount}")
    }
    appendLine()
    appendLine("DEPENDENCIES (raw):")
    r.dependencies.take(50).forEach { d -> appendLine("- ${d.notation} [${d.where}]") }
    appendLine()
    appendLine("DUPLICATES (clusters top 10):")
    r.duplicates.take(10).forEach { d ->
        appendLine("- ${d.representative}")
        d.occurrences.take(3).forEach { o -> appendLine("  * ${o.file}:${o.lineFrom}…${o.lineTo}") }
    }
}

private fun buildUserWithCode(base: String, filesWithCode: List<Pair<String, String>>, mandatoryDiff: Boolean): String = buildString {
    appendLine(base)
    appendLine()
    appendLine("### FILE CONTEXT (read-only)")
    appendLine("I am attaching the exact content of ${filesWithCode.size} files. Generate diffs ONLY for files listed below.")
    filesWithCode.forEach { (path, code) ->
        appendLine()
        appendLine("FILE: $path")
        appendLine("```kotlin")
        appendLine(code)
        appendLine("```")
    }
    appendLine()
    appendLine("Constraints:")
    appendLine("- Use unified diff format with headers (---, +++) and hunks @@.")
    appendLine("- Keep patches small and mechanical; avoid risky rewrites.")
    if (mandatoryDiff) {
        appendLine("- MANDATORY: return at least one diff in 'diffs'. If you cannot, return the exact new file content in 'rewrites'.")
    } else {
        appendLine("- Prefer 'diffs'; you may use 'rewrites' if easier.")
    }
    appendLine()
    appendLine("OUTPUT JSON SCHEMA (strict):\n```json\n{\n  \"plan\": [ { \"severity\": \"high|med|low\", \"title\": \"...\", \"rationale\": \"...\", \"files\": [\"path\"] } ],\n  \"diffs\": [ { \"file\": \"path\", \"unified_diff\": \"...\" } ],\n  \"rewrites\": [ { \"file\": \"path\", \"new_content\": \"...\" } ],\n  \"notes\": \"...\"\n}\n```\nReturn ONLY this JSON.")
}

// ======== main ========
fun main(argv: Array<String>) {
    val args = try { parseArgs(argv) } catch (t: Throwable) { System.err.println("Arg error: ${t.message}"); exitProcess(2) }

    val files = kotlinJavaFiles(args.root)
    if (files.isEmpty()) { println("No source files under: ${args.root}"); return }

    val summaries = files.map { summarizeFile(it, args.root) }
    val dups = findDuplicates(files, args.maxSnippetLines)
    val deps = findDependencies(args.root)
    val tools = tryReadToolReports(args.root)

    val suggestions = suggestHeuristics(summaries, dups, deps)

    val report = ReviewReport(
        root = args.root.canonicalPath,
        files = summaries,
        duplicates = dups,
        dependencies = deps,
        toolFindings = tools,
        suggestions = suggestions,
    )

    val md = renderMarkdown(report)
    args.outMd?.let { it.parentFile?.mkdirs(); it.writeText(md); println("Markdown saved: $it") } ?: println(md)
    args.outJson?.let { it.parentFile?.mkdirs(); it.writeText(toJson(report)); println("JSON saved: $it") }

    if (!args.withLlm) return

    val system = loadSystemPrompt()
    val baseUser = baseUserPrompt(report)
    val filesWithCode = if (args.llmIncludeCode) pickFilesWithContent(args.root, report, args.llmMaxFiles, args.llmMaxChars) else emptyList()
    val user1 = if (filesWithCode.isNotEmpty()) buildUserWithCode(baseUser, filesWithCode, mandatoryDiff = false) else baseUser

    val apiKey = System.getenv("OPEN_ROUTER_API_KEY") ?: run {
        val f = java.io.File("local.properties")
        if (f.exists()) Properties().apply { load(f.inputStream()) }.getProperty("OPEN_ROUTER_API_KEY") else null
    } ?: error("Missing OPEN_ROUTER_API_KEY (env var или local.properties)")
    val client = OpenRouterLlmClient(endpoint = args.llmEndpoint, apiKey = apiKey, appTitle = "legacy-agent")

    println("[LLM] Request (attempt 1) to '${args.llmModel}'…")
    var resp: LlmResponse = try {
        client.generatePlan(system = system, user = user1, model = args.llmModel, temperature = args.llmTemperature, maxTokens = args.llmMaxTokens)
    } catch (t: Throwable) {
        System.err.println("LLM error: ${t.message}"); return
    }

    // Fallbacks: rewrites → diffs
    if (resp.diffs.isEmpty() && resp.rewrites.isNotEmpty()) {
        println("[LLM] No diffs, but got ${resp.rewrites.size} rewrites — converting to unified diffs…")
        val diffs = resp.rewrites.map { r -> diffFromNewContent(args.root, r.file, r.new_content) }
        resp = resp.copy(diffs = diffs)
    }

    // После блока с конвертацией rewrites -> diffs
    if (args.debugDiffs && resp.diffs.isNotEmpty()) {
        println("[LLM] Diff meta:")
        resp.diffs.forEach { d ->
            val meta = inspectUnifiedDiff(d.unified_diff)
            println(" - ${d.file} | hunks=${meta.hunks}, touched=${meta.touchedLines}, headerFile=${meta.fileFromHeader ?: "?"}")
        }
    }

// Соберём файлы с пустыми диффами (нет хунков)
    val emptyOrNoop = resp.diffs.filter { inspectUnifiedDiff(it.unified_diff).hunks == 0 }.map { it.file }.distinct()

// Если такие есть — второй запрос: попросим полные переписанные варианты (rewrites)
    if (emptyOrNoop.isNotEmpty() && args.llmForceRewriteOnEmpty) {
        println("[LLM] Found ${emptyOrNoop.size} empty diffs. Requesting full rewrites for those files…")

        // Подготовим контекст: точный код для этих файлов
        val targeted = emptyOrNoop.mapNotNull { path ->
            val f = File(args.root, path)
            if (f.exists()) path to f.readText() else null
        }

        val rewritePrompt = buildString {
            appendLine("Provide REWRITES for the following files.")
            appendLine("Return JSON with 'rewrites' only (no 'diffs').")
            appendLine("For each file, return COMPLETE new file content that implements the described improvements safely.")
            appendLine()
            targeted.forEach { (path, code) ->
                appendLine("FILE: $path (original)")
                appendLine("```kotlin")
                appendLine(code)
                appendLine("```")
            }
            appendLine()
            appendLine("JSON schema:")
            appendLine("""{
  "rewrites": [ { "file": "path", "new_content": "..." } ]
}""")
        }

        val system = loadSystemPrompt()
        val apiKey = System.getenv("OPEN_ROUTER_API_KEY") ?: run {
            val f = java.io.File("local.properties")
            if (f.exists()) Properties().apply { load(f.inputStream()) }.getProperty("OPEN_ROUTER_API_KEY") else null
        } ?: error("Missing OPEN_ROUTER_API_KEY (env var или local.properties)")
        val client = OpenRouterLlmClient(endpoint = args.llmEndpoint, apiKey = apiKey, appTitle = "legacy-agent")
        val rewriteResp = try {
            client.generatePlan(system = system, user = rewritePrompt, model = args.llmModel, temperature = args.llmTemperature, maxTokens = args.llmMaxTokens)
        } catch (t: Throwable) {
            System.err.println("[LLM] Rewrite request failed: ${t.message}")
            null
        }

        if (rewriteResp != null && rewriteResp.rewrites.isNotEmpty()) {
            println("[LLM] Got ${rewriteResp.rewrites.size} rewrites — converting to diffs…")
            val extraDiffs = rewriteResp.rewrites.map { r -> diffFromNewContent(args.root, r.file, r.new_content) }
            // Заменим пустые диффы на новые
            val keep = resp.diffs.filter { inspectUnifiedDiff(it.unified_diff).hunks > 0 }
            val merged = keep + extraDiffs
            // обновим resp
            resp = resp.copy(diffs = merged)
        } else {
            println("[LLM] No rewrites returned on fallback.")
        }
    }

    // Retry with mandatory diff requirement
    if (resp.diffs.isEmpty() && args.llmRetryIfEmpty && filesWithCode.isNotEmpty()) {
        println("[LLM] No diffs returned. Retrying with mandatory diff constraint…")
        val user2 = buildUserWithCode(baseUser, filesWithCode, mandatoryDiff = true)
        resp = try {
            client.generatePlan(system = system, user = user2, model = args.llmModel, temperature = args.llmTemperature, maxTokens = args.llmMaxTokens)
        } catch (t: Throwable) {
            System.err.println("LLM error (retry): ${t.message}"); resp
        }
        if (resp.diffs.isEmpty() && resp.rewrites.isNotEmpty()) {
            println("[LLM] Retry produced rewrites — converting to unified diffs…")
            val diffs = resp.rewrites.map { r -> diffFromNewContent(args.root, r.file, r.new_content) }
            resp = resp.copy(diffs = diffs)
        }
    }

    println("[LLM] Plan items: ${resp.plan.size}, diffs: ${resp.diffs.size}")
    if (resp.diffs.isNotEmpty()) {
        if (args.applyPatches) {
            println("[PATCH] Applying diffs …")
            var results = applyAllDiffs(args.root, resp.diffs, dryRun = false)
            var ok = results.count { it.ok }
            var fail = results.count { !it.ok }
            results.forEach { r -> println(" - ${r.targetPath}: ${if (r.ok) "OK" else "FAIL"} (${r.message})") }
            println("[PATCH] Summary: $ok OK, $fail FAIL")

            // NEW: if some patches failed with CONTENT_DOES_NOT_MATCH_TARGET — request rewrites for those files
            val problemFiles = results.filter { !it.ok && it.message.contains("CONTENT_DOES_NOT_MATCH_TARGET") }
                .map { it.targetPath }.distinct()
            if (problemFiles.isNotEmpty()) {
                println("[PATCH] ${problemFiles.size} files failed due to CONTENT_DOES_NOT_MATCH_TARGET — requesting full rewrites…")
                val targeted = problemFiles.mapNotNull { path ->
                    val f = File(args.root, path)
                    if (f.exists()) path to f.readText() else null
                }
                val rewritePrompt = buildString {
                    appendLine("Provide REWRITES for the following files (complete new content). Return JSON with 'rewrites'.")
                    targeted.forEach { (path, code) ->
                        appendLine()
                        appendLine("FILE: $path (original)")
                        appendLine("```kotlin")
                        appendLine(code)
                        appendLine("```")
                    }
                    appendLine()
                    appendLine("JSON schema:\n{ \"rewrites\": [ { \"file\": \"path\", \"new_content\": \"...\" } ] }")
                }
                val apiKey = args.llmApiKey ?: System.getenv("OPENROUTER_API_KEY") ?: error("No API key")
                val client2 = OpenRouterLlmClient(endpoint = args.llmEndpoint, apiKey = apiKey, appTitle = "legacy-agent")
                val rewriteResp = try { client2.generatePlan(system = loadSystemPrompt(), user = rewritePrompt, model = args.llmModel, temperature = args.llmTemperature, maxTokens = args.llmMaxTokens) } catch (t: Throwable) {
                    System.err.println("[LLM] Rewrite request failed: ${t.message}"); null
                }
                if (rewriteResp != null && rewriteResp.rewrites.isNotEmpty()) {
                    val extraDiffs = rewriteResp.rewrites.map { r -> diffFromNewContent(args.root, r.file, r.new_content) }
                    println("[PATCH] Retrying apply with ${extraDiffs.size} diffs from rewrites…")
                    results = applyAllDiffs(args.root, extraDiffs, dryRun = false)
                    results.forEach { r -> println(" - retry ${r.targetPath}: ${if (r.ok) "OK" else "FAIL"} (${r.message})") }
                }
            }
        } else {
            println("[PATCH] --apply=false — diffs not applied. Use --apply true to apply.")
        }
    } else {
        println("[LLM] Still no diffs. See notes/plan. Try increasing --llm-files/--llm-max-chars or point to a specific file.")
    }
}
