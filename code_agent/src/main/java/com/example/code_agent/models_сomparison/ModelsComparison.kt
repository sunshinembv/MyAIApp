package com.example.code_agent.models_сomparison

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// ---------- Config ----------
private const val OPEN_ROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
private const val APP_URL = "https://example.local/myaiapp"
private const val APP_NAME = "MyAIApp - HF Day21 (3 stages)"

private val MODELS = listOf(
    "deepseek/deepseek-chat-v3.1:free"   to "TOP",
    "google/gemma-2-9b-it:free"          to "MID",
    "mistralai/mistral-7b-instruct:free" to "LOW"
)

data class ModelCfg(val maxTokens: Int?, val temperature: Double?)
private val MODEL_CFG: Map<String, ModelCfg> = mapOf(
    "deepseek/deepseek-chat-v3.1:free"   to ModelCfg(maxTokens = 700, temperature = 0.7),
    "google/gemma-2-9b-it:free"          to ModelCfg(maxTokens = 700, temperature = 0.7),
    "mistralai/mistral-7b-instruct:free" to ModelCfg(maxTokens = 700, temperature = 0.7)
)

// ---------- Prompts ----------
private const val SYSTEM_PROMPT_STAGE1 = """
Ты преподаватель физики.
Всегда отвечай только по-русски. Никогда не используй английский язык.
Объясняй ясно, 150–220 слов, одна короткая аналогия.
Без формул, без кода, без вопросов и лишних фраз.
Do not use English. Respond only in Russian.
"""

private const val USER_STAGE1 = """
Стадия 1. Объясни простыми словами по-русски, что такое квантовый туннельный эффект и почему он невозможен в классике, но возможен в квантовой механике.
"""

private const val SYSTEM_PROMPT_STAGE2 = """
You are a software engineer. Respond with exactly one code block in Python.
Rules:
- Output only one block: ```python ... ```
- Define def is_prime(n): with early returns, handle n<2, use a loop.
- Add 3–5 checks: either `assert is_prime(x) == True/False` OR `print(is_prime(x))  # Expected`
- No text outside the code block.
"""

private const val USER_STAGE2 = "Стадия 2. Напиши функцию is_prime(n) и добавь несколько проверок. Ответ — строго одним блоком ```python```."

// NEW Stage 3: string_utils.py (без is_palindrome)
private const val SYSTEM_PROMPT_STAGE3 = """
You are a software engineer. Respond with exactly one Python code block.
Rules:
- Output only one block: ```python ... ```
- Create a module `string_utils.py` with two functions:
  1) reverse_string(s): return the reversed string.
  2) count_vowels(s): return the number of vowels (a, e, i, o, u, а, е, ё, и, о, у, ы, э, ю, я), case-insensitive.
- At the bottom, in the same block, add 4–6 tests: either `assert ...` or `print(...)  # Expected`.
- Absolutely no text outside the code block.
"""

private const val USER_STAGE3 = "Стадия 3. Напиши модуль string_utils.py с функциями reverse_string(s) и count_vowels(s) и добавь 4–6 проверок. Ответ — строго одним блоком ```python```."

// ---------- API types ----------
@JsonClass(generateAdapter = true)
data class ChatMessage(val role: String, val content: String)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @Json(name = "max_tokens") val maxTokens: Int? = null,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class ChoiceMsg(val role: String?, val content: String?)

@JsonClass(generateAdapter = true)
data class Choice(val message: ChoiceMsg?)

@JsonClass(generateAdapter = true)
data class Usage(
    @Json(name = "prompt_tokens") val promptTokens: Int? = null,
    @Json(name = "completion_tokens") val completionTokens: Int? = null,
    @Json(name = "total_tokens") val totalTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
)

// ---------- Metrics ----------
data class Stage1Metrics(
    val lengthScore: Int,
    val keywordScore: Int,
    val clarityScore: Int,
    val structureScore: Int,
    val words: Int,
    val latencyMs: Long,
    val tokensPrompt: Int?, val tokensCompletion: Int?
) { val total: Int get() = lengthScore + keywordScore + clarityScore + structureScore }

data class Stage2Metrics(
    val codeFence: Int,
    val signature: Int,
    val nlt2: Int,
    val loop: Int,
    val checks: Int,
    val latencyMs: Long,
    val tokensPrompt: Int?, val tokensCompletion: Int?
) { val total: Int get() = codeFence + signature + nlt2 + loop + checks }

data class Stage3Metrics(
    val codeFence: Int,      // 0/1
    val funcs: Int,          // 0..2 (reverse_string, count_vowels)
    val checks: Int,         // 0..2 (≥4 → 2; ≥2 → 1)
    val latencyMs: Long,
    val tokensPrompt: Int?, val tokensCompletion: Int?
) { val total: Int get() = codeFence + funcs + checks }

data class RunSummary(
    val bucket: String, val model: String,
    val stage1: Stage1Metrics,
    val stage2: Stage2Metrics,
    val stage3: Stage3Metrics
) { val totalScore: Int get() = stage1.total + stage2.total + stage3.total }

// ---------- Main ----------
fun main() {
    val apiKey = System.getenv("OPEN_ROUTER_API_KEY") ?: run {
        val f = java.io.File("local.properties")
        if (f.exists()) Properties().apply { load(f.inputStream()) }.getProperty("OPEN_ROUTER_API_KEY") else null
    } ?: error("Missing OPEN_ROUTER_API_KEY (env var или local.properties)")

    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val reqAdapter = moshi.adapter(ChatRequest::class.java)
    val respAdapter = moshi.adapter(ChatResponse::class.java)
    val client = OkHttpClient.Builder()
        .callTimeout(120, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    val mediaType: MediaType = "application/json; charset=utf-8".toMediaType()

    val results = mutableListOf<RunSummary>()
    println("== HF Day21: 3 Stages (S1 9 + S2 6 + S3 5 = 20 max) ==")

    for ((modelId, bucket) in MODELS) {
        val cfg = MODEL_CFG[modelId] ?: ModelCfg(null, null)

        // Stage 1
        val (c1,u1,t1) = callOpenRouter(
            client, reqAdapter, respAdapter, apiKey, modelId, cfg,
            listOf(ChatMessage("system", SYSTEM_PROMPT_STAGE1), ChatMessage("user", USER_STAGE1)), mediaType
        )
        val m1 = evalStage1(c1, t1, u1)
        println("----- $bucket | $modelId | Stage 1 (${m1.latencyMs} ms) -----")
        println(c1.trim().take(700))
        println("S1 score: ${m1.total}/9 (len=${m1.words}w)\n")

        // Stage 2
        val (c2,u2,t2) = callOpenRouter(
            client, reqAdapter, respAdapter, apiKey, modelId, cfg,
            listOf(ChatMessage("system", SYSTEM_PROMPT_STAGE2), ChatMessage("user", USER_STAGE2)), mediaType
        )
        val m2 = evalStage2(c2, t2, u2)
        println("----- $bucket | $modelId | Stage 2 (${m2.latencyMs} ms) -----")
        println(firstPythonBlockOrAll(c2).trim().take(700))
        println("S2 score: ${m2.total}/6\n")

        // Stage 3
        val (c3,u3,t3) = callOpenRouter(
            client, reqAdapter, respAdapter, apiKey, modelId, cfg,
            listOf(ChatMessage("system", SYSTEM_PROMPT_STAGE3), ChatMessage("user", USER_STAGE3)), mediaType
        )
        val m3 = evalStage3(c3, t3, u3)
        println("----- $bucket | $modelId | Stage 3 (${m3.latencyMs} ms) -----")
        println(firstPythonBlockOrAll(c3).trim().take(700))
        println("S3 score: ${m3.total}/5\n")

        results += RunSummary(bucket, modelId, m1, m2, m3)
    }

    // Summary
    println("== Summary ==")
    println(listOf(
        "Bucket","Model",
        "S1(ms)","S1Score",
        "S2(ms)","S2Score",
        "S3(ms)","S3Score",
        "TOTAL(20)",
        "Tok(S1 p/c/t)","Tok(S2 p/c/t)","Tok(S3 p/c/t)"
    ).joinToString(" | "))

    results.forEach {
        val s1=it.stage1; val s2=it.stage2; val s3=it.stage3
        println(listOf(
            it.bucket, it.model,
            s1.latencyMs.toString(), "${s1.total}/9",
            s2.latencyMs.toString(), "${s2.total}/6",
            s3.latencyMs.toString(), "${s3.total}/5",
            "${it.totalScore}/20",
            "${s1.tokensPrompt ?: "-"} / ${s1.tokensCompletion ?: "-"} / ${(s1.tokensPrompt ?: 0) + (s1.tokensCompletion ?: 0)}",
            "${s2.tokensPrompt ?: "-"} / ${s2.tokensCompletion ?: "-"} / ${(s2.tokensPrompt ?: 0) + (s2.tokensCompletion ?: 0)}",
            "${s3.tokensPrompt ?: "-"} / ${s3.tokensCompletion ?: "-"} / ${(s3.tokensPrompt ?: 0) + (s3.tokensCompletion ?: 0)}"
        ).joinToString(" | "))
    }

    val winner = results.maxByOrNull { it.totalScore }
    println("\nWinner: ${winner?.bucket} — ${winner?.model} with ${winner?.totalScore}/20")
}

// ---------- Networking ----------
private fun callOpenRouter(
    client: OkHttpClient,
    reqAdapter: com.squareup.moshi.JsonAdapter<ChatRequest>,
    respAdapter: com.squareup.moshi.JsonAdapter<ChatResponse>,
    apiKey: String, modelId: String, cfg: ModelCfg,
    messages: List<ChatMessage>, mediaType: MediaType
): Triple<String, Usage?, Long> {
    val reqObj = ChatRequest(
        model = modelId,
        messages = messages,
        maxTokens = cfg.maxTokens,     // null -> Moshi не сериализует
        temperature = cfg.temperature  // null -> Moshi не сериализует
    )
    val body = reqAdapter.toJson(reqObj)
    val httpReq = Request.Builder()
        .url(OPEN_ROUTER_URL)
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("HTTP-Referer", APP_URL)
        .addHeader("X-Title", APP_NAME)
        .post(body.toRequestBody(mediaType))
        .build()

    val t0 = System.nanoTime()
    val (content, usage) = client.newCall(httpReq).execute().use { resp ->
        if (!resp.isSuccessful) error("HTTP ${resp.code}: ${resp.body?.string()}")
        val str = resp.body!!.string()
        val parsed = respAdapter.fromJson(str) ?: ChatResponse()
        (parsed.choices.firstOrNull()?.message?.content.orEmpty()) to parsed.usage
    }
    val t1 = System.nanoTime()
    val latencyMs = ((t1 - t0) / 1_000_000.0).roundToInt().toLong()

    return Triple(content, usage, latencyMs)
}

// ---------- Helpers ----------
private fun firstPythonBlockOrAll(txt: String): String {
    val m = Regex("""```python\s*([\s\S]+?)\s*```""").find(txt)
    return m?.value ?: txt
}

// Stage 1 (physics) evaluation
private fun evalStage1(txt: String, latency: Long, usage: Usage?): Stage1Metrics {
    val normalized = txt.lowercase()
    val words = txt.split(Regex("""\s+""")).count { it.isNotBlank() }

    val lengthScore = when (words) {
        in 150..220 -> 2
        in 120..260 -> 1
        else -> 0
    }
    val keywordScore = listOf("квант", "туннел", "барьер", "вероятност")
        .count { normalized.contains(it) }.coerceAtMost(4)
    val clarityScore = if (listOf("например", "представьте", "проще говоря").any { normalized.contains(it) }) 2 else 1
    val structureScore = if (Regex("""(^|\n)\s*[-*•]|\d\.""").containsMatchIn(txt)) 1 else 0

    return Stage1Metrics(
        lengthScore, keywordScore, clarityScore, structureScore,
        words, latency,
        usage?.promptTokens, usage?.completionTokens
    )
}

// Stage 2 (is_prime) evaluation
private fun evalStage2(txt: String, latency: Long, usage: Usage?): Stage2Metrics {
    val hasFence = Regex("""```python""").containsMatchIn(txt)
    val hasSig = Regex("""\bdef\s+is_prime\s*\(""").containsMatchIn(txt)
    val hasLT2 = Regex("""n\\s*<\\s*2""").containsMatchIn(txt) || Regex("""n\\s*<=\\s*1""").containsMatchIn(txt)
    val hasLoop = Regex("""\\b(for|while)\\b""").containsMatchIn(txt)
    val assertsCount = Regex("""assert\\s+is_prime""").findAll(txt).count()
    val printsWithCall = Regex("""print\s*\(\s*is_prime\s*\(\s*\w+|\d+\s*\)\s*\)""").findAll(txt).count()
    val checksScore = when {
        assertsCount + printsWithCall >= 4 -> 2
        assertsCount + printsWithCall >= 2 -> 1
        else -> 0
    }
    return Stage2Metrics(
        codeFence = if (hasFence) 1 else 0,
        signature = if (hasSig) 1 else 0,
        nlt2 = if (hasLT2) 1 else 0,
        loop = if (hasLoop) 1 else 0,
        checks = checksScore,
        latencyMs = latency,
        tokensPrompt = usage?.promptTokens, tokensCompletion = usage?.completionTokens
    )
}

// Stage 3 (string_utils without palindrome) evaluation
private fun evalStage3(txt: String, latency: Long, usage: Usage?): Stage3Metrics {
    val hasFence = Regex("""```python""").containsMatchIn(txt)
    val fReverse = Regex("""\bdef\s+reverse_string\s*\(""").containsMatchIn(txt)
    val fVowels  = Regex("""\bdef\s+count_vowels\s*\(""").containsMatchIn(txt)
    val funcs = listOf(fReverse, fVowels).count { it } // 0..2

    val checks = Regex("""\bassert\b""").findAll(txt).count() +
            Regex("""\bprint\s*\(""").findAll(txt).count()
    val checksScore = when {
        checks >= 4 -> 2
        checks >= 2 -> 1
        else -> 0
    }

    return Stage3Metrics(
        codeFence = if (hasFence) 1 else 0,
        funcs = funcs,
        checks = checksScore,
        latencyMs = latency,
        tokensPrompt = usage?.promptTokens, tokensCompletion = usage?.completionTokens
    )
}