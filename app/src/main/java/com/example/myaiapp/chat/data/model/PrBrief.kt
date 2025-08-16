package com.example.myaiapp.chat.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PrBrief(
    val title: String,
    @Json(name = "pullNumber")
    val number: Int,
    val state: PrState,
    val author: String,
    val createdAt: String,
    val comments: Int,   // issue + review
    val additions: Int,
    val deletions: Int
)

@JsonClass(generateAdapter = true)
data class PrRaw(
    val number: Int,
    val title: String,
    val state: PrState,
    val user: UserRaw,
    @Json(name = "created_at") val createdAt: String,
    val comments: Int = 0,
    @Json(name = "review_comments") val reviewComments: Int = 0,
    val additions: Int = 0,
    val deletions: Int = 0,
    @Json(name = "html_url") val htmlUrl: String? = null
) {
    fun toBrief() = PrBrief(
        title = title,
        number = number,
        state = state,
        author = user.login,
        createdAt = createdAt,
        comments = comments + reviewComments,
        additions = additions,
        deletions = deletions
    )
}

@JsonClass(generateAdapter = true)
data class UserRaw(val login: String)

@JsonClass(generateAdapter = true)
data class RpcEnvelope(
    val result: RpcResult? = null
)

@JsonClass(generateAdapter = true)
data class RpcResult(
    val items: List<PrRaw>? = null,
    val content: List<ResultContent>? = null
)

@JsonClass(generateAdapter = true)
data class ResultContent(
    val type: String,
    val text: String? = null
)
