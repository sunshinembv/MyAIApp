package com.example.myaiapp.chat.data.git_hub.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpsEnvelope(
    val mode: String,
    val actions: List<OpsAction> = emptyList(),
    val notes: String? = null,
)

@JsonClass(generateAdapter = true)
data class OpsAction(
    val type: String,
    val inputs: Map<String, Any?> = emptyMap(),
)

internal fun Map<String, Any?>.str(key: String, default: String = ""): String =
    when (val v = this[key]) {
        null -> default
        is String -> v
        is Number -> v.toString()
        is Boolean -> v.toString()
        else -> default
    }

internal fun Map<String, Any?>.boolString(key: String, default: Boolean = false): String {
    val v = this[key]
    val b = when (v) {
        is Boolean -> v
        is Number -> v.toInt() != 0
        is String -> v.equals("true", true) || v == "1" || v.equals("yes", true)
        else -> default
    }
    return if (b) "true" else "false"
}
