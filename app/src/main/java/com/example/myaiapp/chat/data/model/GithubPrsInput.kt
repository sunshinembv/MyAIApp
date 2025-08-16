package com.example.myaiapp.chat.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class GithubPrsInput(
    val action: String,          // должен быть "github.prs.list_and_detail"
    val owner: String,
    val repo: String,
    val state: PrState = PrState.OPEN,
    val limit: Int = 20          // 1..20
) {
    fun validate(): GithubPrsInput {
        require(action == ACTION) { "action must be \"$ACTION\"" }
        require(owner.isNotBlank()) { "owner is blank" }
        require(repo.isNotBlank())  { "repo is blank" }
        require(limit in 1..20)     { "limit must be in 1..20" }
        return this
    }

    companion object {
        const val ACTION = "github.prs.list_and_detail"

        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }
        private val adapter by lazy { moshi.adapter(GithubPrsInput::class.java) }

        /** Парсит JSON от LLM и валидирует значения. */
        fun fromJson(json: String): GithubPrsInput =
            requireNotNull(adapter.fromJson(json)) { "Bad JSON for GithubPrsInput" }
                .validate()
    }
}

enum class PrState {
    @Json(name = "open") OPEN,
    @Json(name = "closed") CLOSED,
    @Json(name = "all") ALL;

    /** Строковое значение для MCP-аргумента `state`. */
    fun asArg(): String = when (this) {
        OPEN -> "open"; CLOSED -> "closed"; ALL -> "all"
    }
}