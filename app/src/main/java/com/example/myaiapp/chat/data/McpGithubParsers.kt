package com.example.myaiapp.chat.data

import com.example.myaiapp.chat.data.model.PrBrief
import com.example.myaiapp.chat.data.model.PrRaw
import com.example.myaiapp.chat.data.model.RpcEnvelope
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object McpGithubParsers {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val envelopeAdapter = moshi.adapter(RpcEnvelope::class.java)

    private val prRawListType = Types.newParameterizedType(List::class.java, PrRaw::class.java)
    private val prRawListAdapter = moshi.adapter<List<PrRaw>>(prRawListType)

    private val prRawAdapter = moshi.adapter(PrRaw::class.java)
    private val prDetailAdapter = moshi.adapter(PrRaw::class.java)

    /**
     * Принимает СЫРОЙ ответ JSON-RPC от MCP (то, что вернул rpc("tools/call", ...)),
     * и возвращает список PrBrief:
     * - если result.items есть и это список объектов PR — парсим сразу;
     * - иначе ищем result.content[?].text, внутри которого строкой лежит JSON (массив или объект).
     */
    fun parsePrListFromMcpJson(rawJson: String): List<PrBrief> {
        val env = requireNotNull(envelopeAdapter.fromJson(rawJson)) { "Bad MCP JSON (envelope)" }
        val result = requireNotNull(env.result) { "No 'result' in MCP response" }

        // Вариант 1: items уже список объектов
        result.items?.let { return it.map { pr -> pr.toBrief() } }

        // Вариант 2: content[..].text = "<json>"
        val textPayload = result.content
            ?.firstOrNull { it.type == "text" && !it.text.isNullOrBlank() }
            ?.text
            ?: error("No text content in MCP result")

        // Внутри text может быть МАССИВ или ОБЪЕКТ. Поддержим оба:
        return if (textPayload.trimStart().startsWith("[")) {
            prRawListAdapter.fromJson(textPayload).orEmpty().map { it.toBrief() }
        } else {
            listOfNotNull(prRawAdapter.fromJson(textPayload)?.toBrief())
        }
    }

    fun parsePrDetail(rawJson: String): PrRaw {
        val env = requireNotNull(envelopeAdapter.fromJson(rawJson)) { "Bad MCP JSON" }
        val res = requireNotNull(env.result) { "No result" }
        val text = res.content?.firstOrNull { it.type == "text" && !it.text.isNullOrBlank() }?.text
            ?: error("No text content in result")
        return requireNotNull(prDetailAdapter.fromJson(text)) { "Bad PR detail JSON" }
    }
}