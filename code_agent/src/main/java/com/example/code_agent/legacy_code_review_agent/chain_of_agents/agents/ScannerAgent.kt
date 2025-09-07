package com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents

import com.example.code_agent.legacy_code_review_agent.chain_of_agents.Agent
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.Dependency
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.DuplicateCluster
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.FileSummary
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.PipelineCtx
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.ReviewItem
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.ReviewReport
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.ScanResult
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.ToolFinding
import java.io.File
import okio.buffer
import okio.source
import java.security.MessageDigest

/**
 * Agent #1 — Scanner
 * Сканирует проект (Kotlin/Java/Gradle), собирает метрики, дубликаты, зависимости,
 * подхватывает отчёты detekt/ktlint и формирует ScanResult.
 */
class ScannerAgent : Agent<Unit, ScanResult> {
    override fun run(input: Unit, ctx: PipelineCtx): ScanResult {
        val root = ctx.root
        val files = kotlinJavaFiles(root)
        val summaries = files.map { summarizeFile(it, root) }
        val duplicates = findDuplicates(files, maxSnippetLines = 80)
        val deps = findDependencies(root)
        val tools = tryReadToolReports(root)
        val suggestions = suggestHeuristics(summaries, duplicates, deps)
        val report = ReviewReport(
            root = root.canonicalPath,
            files = summaries,
            duplicates = duplicates,
            dependencies = deps,
            toolFindings = tools,
            suggestions = suggestions,
        )
        return ScanResult(report)
    }

    // ===== internals (lifted & adapted from the single-file Main) =====

    private fun readText(file: File) = file.inputStream().source().buffer().use { it.readUtf8() }

    private fun kotlinJavaFiles(root: File): List<File> = root.walkTopDown()
        .filter { it.isFile && (it.extension in setOf("kt", "kts", "java", "gradle")) }
        .toList()

    private fun summarizeFile(file: File, base: File): FileSummary {
        val text = readText(file)
        val lines = text.lines()
        val funcs = Regex("(^|\\s)fun\\s+", RegexOption.MULTILINE).findAll(text).count()
        val funcLens = Regex("(^|\\s)fun\\s+.*?\\n", RegexOption.DOT_MATCHES_ALL)
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
        val rel = try { file.relativeTo(base).path } catch (_: Throwable) { file.absolutePath }
        val norm = rel.replace('\\', '/')
        return FileSummary(norm, lines.size, funcs, maxFn, maxDepth, notNullBangs)
    }

    private fun stripCommentsAndNormalize(code: String): String {
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
        return index.values
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
            .take(50)
    }

    private fun findDependencies(root: File): List<Dependency> {
        val gradleFiles = root.walkTopDown().filter { it.isFile && (it.name.endsWith("build.gradle") || it.name.endsWith("build.gradle.kts") || it.name == "libs.versions.toml") }.toList()
        val deps = mutableListOf<Dependency>()
        for (f in gradleFiles) {
            val t = readText(f)
            Regex("implementation\\(\\\"([^\\\"]+:([^\\\"]+):([^\\\"]+))\\\"\\)").findAll(t).forEach { m -> deps += Dependency(m.groupValues[1], f.path) }
            Regex("api\\(\\\"([^\\\"]+:([^\\\"]+):([^\\\"]+))\\\"\\)").findAll(t).forEach { m -> deps += Dependency(m.groupValues[1], f.path) }
            Regex("implementation\\((libs\\.[^)]+)\\)").findAll(t).forEach { m -> deps += Dependency(m.groupValues[1], f.path) }
        }
        return deps.distinct()
    }

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
}
