package com.example.code_agent.legacy_code_review_agent.chain_of_agents

import com.example.code_agent.legacy_code_review_agent.LlmDiff
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents.ApplyAgent
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents.RefactorAgent
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents.RetryAgent
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents.ScannerAgent
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents.StrategyAgent
import com.example.code_agent.legacy_code_review_agent.diffFromNewContent
import java.io.File
import kotlin.system.exitProcess
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Properties

/**
 * Orchestrates 5 agents in sequence: Scanner → Strategist → Refactorer → RetryAgent(Optional) → Applier
 * Adds robust retry flow and ALWAYS writes retry_patches.json (even if empty).
 */

enum class Stage { scan, strategize, refactor, apply, all }

data class ChainArgs(
    val root: File,
    val model: String,
    val endpoint: String = System.getenv("OPEN_ROUTER_ENDPOINT") ?: "https://openrouter.ai/api/v1/chat/completions",
    val apiKey: String? = System.getenv("OPEN_ROUTER_API_KEY") ?: run {
        val f = java.io.File("local.properties")
        if (f.exists()) Properties().apply { load(f.inputStream()) }
            .getProperty("OPEN_ROUTER_API_KEY") else null
    } ?: error("Missing OPEN_ROUTER_API_KEY (env var или local.properties)"),
    val temp: Double = 0.0,
    val maxTokens: Int = 10000,
    val includeCode: Boolean = true,
    val ctxFiles: Int = 3,
    val ctxChars: Int = 120_000,
    val stage: Stage = Stage.all,
    val apply: Boolean = false,
    val outDir: File = File("out/chain")
)

fun parseChainArgs(argv: Array<String>): ChainArgs {
    var root: File? = null
    var model: String? = null
    var endpoint: String? = null
    var apiKey: String? = null
    var temp = 0.0
    var maxTok = 2000
    var include = true
    var ctxFiles = 3
    var ctxChars = 120_000
    var stage = Stage.all
    var apply = false
    var outDir: File = File("out/chain")

    val it = argv.iterator()
    require(it.hasNext() && it.next() == "chain") { "first arg must be 'chain'" }
    while (it.hasNext()) {
        when (val a = it.next()) {
            "--root" -> root = File(it.next())
            "--model" -> model = it.next()
            "--endpoint" -> endpoint = it.next()
            "--api-key" -> apiKey = it.next()
            "--temp" -> temp = it.next().toDouble()
            "--max-tokens" -> maxTok = it.next().toInt()
            "--include-code" -> include = it.next().toBooleanStrict()
            "--ctx-files" -> ctxFiles = it.next().toInt()
            "--ctx-chars" -> ctxChars = it.next().toInt()
            "--stage" -> stage = Stage.valueOf(it.next())
            "--apply" -> apply = it.next().toBooleanStrict()
            "--out-dir" -> outDir = File(it.next())
            else -> error("Unknown arg: $a")
        }
    }
    require(root != null && root!!.exists()) { "--root not found" }
    require(!model.isNullOrBlank()) { "--model is required" }
    return ChainArgs(
        root = root!!,
        model = model!!,
        endpoint = endpoint ?: System.getenv("OPEN_ROUTER_ENDPOINT") ?: "https://openrouter.ai/api/v1/chat/completions",
        apiKey = apiKey,
        temp = temp,
        maxTokens = maxTok,
        includeCode = include,
        ctxFiles = ctxFiles,
        ctxChars = ctxChars,
        stage = stage,
        apply = apply,
        outDir = outDir
    )
}

// ---------- persistence helpers ----------
private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
private inline fun <reified T> toJson(value: T): String = moshi.adapter(T::class.java).indent("  ").toJson(value)
private inline fun <reified T> saveJson(file: File, value: T) {
    file.parentFile?.mkdirs()
    file.writeText(toJson(value))
}
private fun saveText(file: File, text: String) {
    file.parentFile?.mkdirs()
    file.writeText(text)
}

private fun writePatchesDir(dir: File, diffs: List<LlmDiff>) {
    dir.mkdirs()
    diffs.forEach { d ->
        val out = dir.resolve(d.file + ".patch")
        out.parentFile?.mkdirs()
        out.writeText(d.unified_diff)
    }
}

private fun makeSummaryMd(scan: ScanResult, strat: StrategyResult?, patches: PatchSet?, apply: ApplySummary?, outDir: File): String = buildString {
    appendLine("# Chain Summary")
    appendLine()
    appendLine("Root: `${scan.report.root}`")
    appendLine("Generated: ${java.time.LocalDateTime.now()}")
    appendLine()

    appendLine("## Scan")
    appendLine("- Files: ${scan.report.files.size}")
    appendLine("- Duplicate clusters: ${scan.report.duplicates.size}")
    appendLine("- Dependencies: ${scan.report.dependencies.size}")
    appendLine()

    strat?.let {
        appendLine("## Strategy")
        appendLine("Plan items: ${it.plan.size}")
        it.plan.forEach { p ->
            appendLine("- [${p.severity}] ${p.title} → files: ${p.files.joinToString()}")
        }
        appendLine("Target files: ${it.targetFiles.joinToString()}")
        it.notes?.let { n -> appendLine("Notes: $n") }
        appendLine()
    }

    patches?.let {
        appendLine("## Refactor (LLM)")
        appendLine("Diffs: ${it.diffs.size}, Rewrites: ${it.rewrites.size}")
        if (it.diffs.isNotEmpty()) {
            appendLine("Saved .patch files to: ${outDir.resolve("patches").absolutePath}")
        }
        appendLine()
    }

    apply?.let {
        appendLine("## Apply")
        appendLine("OK: ${it.ok}, FAIL: ${it.fail}")
        it.results.take(50).forEach { r ->
            appendLine("- ${r.targetPath}: ${if (r.ok) "OK" else "FAIL"} (${r.message})")
        }
        appendLine()
    }
}

// ---------- main ----------
fun main(args: Array<String>) {
    if (args.isEmpty() || args[0] != "chain") return // not our subcommand
    val a = try { parseChainArgs(args) } catch (t: Throwable) { System.err.println("Arg error: ${t.message}"); exitProcess(2) }

    a.outDir.mkdirs()

    val ctx = PipelineCtx(
        root = a.root,
        llmModel = a.model,
        llmEndpoint = a.endpoint,
        llmApiKey = a.apiKey,
        llmTemperature = a.temp,
        llmMaxTokens = a.maxTokens,
        includeCode = a.includeCode,
        maxFilesForContext = a.ctxFiles,
        maxCharsForContext = a.ctxChars,
        dryRun = !a.apply,
        allowLocalFallback = false // enforce LLM-generated patches only
    )

    // 1) Scan
    val scan = ScannerAgent().run(Unit, ctx)
    println("[SCAN] files=${scan.report.files.size}, dups=${scan.report.duplicates.size}, deps=${scan.report.dependencies.size}")
    saveJson(a.outDir.resolve("scan.json"), scan)
    if (a.stage == Stage.scan) {
        saveText(a.outDir.resolve("summary.md"), makeSummaryMd(scan, null, null, null, a.outDir))
        return
    }

    // 2) Strategy
    val strat = StrategyAgent().run(scan, ctx)
    println("[STRATEGY] plan=${strat.plan.size}, targets=${strat.targetFiles.size}")
    saveJson(a.outDir.resolve("strategy.json"), strat)
    if (a.stage == Stage.strategize) {
        saveText(a.outDir.resolve("summary.md"), makeSummaryMd(scan, strat, null, null, a.outDir))
        return
    }

    // 3) Refactor (LLM)
    val patches = RefactorAgent().run(strat, ctx)
    println("[REFACTOR] diffs=${patches.diffs.size}, rewrites=${patches.rewrites.size}")
    saveJson(a.outDir.resolve("patches.json"), patches)
    if (patches.diffs.isNotEmpty()) writePatchesDir(a.outDir.resolve("patches"), patches.diffs)
    if (a.stage == Stage.refactor) {
        saveText(a.outDir.resolve("summary.md"), makeSummaryMd(scan, strat, patches, null, a.outDir))
        return
    }

    // 4) Apply — первый проход
    val summary1 = ApplyAgent().run(patches, ctx)
    println("[APPLY#1] ok=${summary1.ok}, fail=${summary1.fail}")
    saveJson(a.outDir.resolve("apply.json"), summary1)

    // Подготовка данных для ретрая: ВСЕГДА сохраняем заготовки
    val failed = summary1.results.filter { !it.ok }.map { it.targetPath }
    saveJson(a.outDir.resolve("retry_prep.json"), mapOf("failed" to failed))

    // Если нет падений — всё равно создадим пустой retry_patches.json и завершим
    if (failed.isEmpty()) {
        saveJson(a.outDir.resolve("retry_patches.json"), PatchSet())
        saveText(a.outDir.resolve("summary.md"), makeSummaryMd(scan, strat, patches, summary1, a.outDir))
        return
    }

    // Сохраним артефакты для анализа падений
    val failDir = a.outDir.resolve("patch_failures").also { it.mkdirs() }
    failed.forEach { p ->
        val snapshot = File(ctx.root, p).takeIf { it.exists() }?.readText() ?: ""
        saveText(failDir.resolve(p.replace('/', '_') + ".orig.kt"), snapshot)
        val origDiff = patches.diffs.firstOrNull { it.file == p }?.unified_diff ?: ""
        saveText(failDir.resolve(p.replace('/', '_') + ".orig.diff"), origDiff)
    }

    // Подготовим карту file->failedDiff и вызовем RetryAgent (v3 рекомендуем для слабых моделей)
    val failedDiffByFile: Map<String,String> = patches.diffs
        .filter { d -> failed.contains(d.file) }
        .associate { it.file to it.unified_diff }

    println("[RETRY] failed targets: ${failed.joinToString()}")
    val retryInput = RetryAgent.Input(failedPaths = failed)
    val retryPatchSet = RetryAgent().run(retryInput, ctx)

    // ВСЕГДА сохраняем retry_patches.json (даже если пусто)
    saveJson(a.outDir.resolve("retry_patches.json"), retryPatchSet)

    // Конвертируем rewrites → diffs, если нужно
    val retryDiffs: List<LlmDiff> = if (retryPatchSet.diffs.isNotEmpty()) retryPatchSet.diffs else retryPatchSet.rewrites.map { r ->
        diffFromNewContent(ctx.root, r.file, r.new_content)
    }
    if (retryDiffs.isNotEmpty()) writePatchesDir(a.outDir.resolve("patches_retry"), retryDiffs)

    val summary2 = ApplyAgent().run(PatchSet(diffs = retryDiffs, rewrites = emptyList()), ctx)
    println("[APPLY#2] ok=${summary2.ok}, fail=${summary2.fail}")
    saveJson(a.outDir.resolve("apply_retry.json"), summary2)

    // Итоговый summary.md
    saveText(a.outDir.resolve("summary.md"), makeSummaryMd(scan, strat, PatchSet(diffs = patches.diffs + retryDiffs, rewrites = emptyList()), summary2, a.outDir))
}
