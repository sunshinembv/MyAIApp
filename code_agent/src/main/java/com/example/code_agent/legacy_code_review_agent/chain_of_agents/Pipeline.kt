package com.example.code_agent.legacy_code_review_agent.chain_of_agents

import com.example.code_agent.legacy_code_review_agent.ApplyResult
import com.example.code_agent.legacy_code_review_agent.LlmDiff
import com.example.code_agent.legacy_code_review_agent.LlmPlanItem
import com.example.code_agent.legacy_code_review_agent.LlmRewrite
import java.io.File

data class PipelineCtx(
    val root: File,
    val llmModel: String,
    val llmEndpoint: String,
    val llmApiKey: String?,
    val llmTemperature: Double = 0.0,
    val llmMaxTokens: Int = 10000,
    val includeCode: Boolean = true,
    val maxFilesForContext: Int = 3,
    val maxCharsForContext: Int = 120_000,
    val dryRun: Boolean = true,
    val allowLocalFallback: Boolean = false // if true, we may synthesize a patch locally as last resort
)

// 1) Scanner → produces a ScanResult (reusing report structure)

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
    val category: String,
    val severity: String,
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

data class ScanResult(val report: ReviewReport)

// 2) Strategist → focuses and orders work

data class StrategyResult(
    val plan: List<LlmPlanItem>,
    val targetFiles: List<String>,
    val notes: String? = null
)

// 3) Refactorer → produces patches

data class PatchSet(
    val diffs: List<LlmDiff> = emptyList(),
    val rewrites: List<LlmRewrite> = emptyList()
)

// 4) Applier → final summary

data class ApplySummary(
    val results: List<ApplyResult>,
    val ok: Int,
    val fail: Int
)

// Generic Agent contract
interface Agent<I, O> { fun run(input: I, ctx: PipelineCtx): O }
