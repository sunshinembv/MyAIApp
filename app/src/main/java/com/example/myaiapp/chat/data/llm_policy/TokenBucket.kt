package com.example.myaiapp.chat.data.llm_policy

import kotlin.math.min

class TokenBucket(private val capacity: Int, private val refillPerMinute: Int) {
    private var tokens = capacity
    private var lastRefillMs = System.currentTimeMillis()
    @Synchronized
    fun tryConsume(): Boolean {
        val now = System.currentTimeMillis()
        val mins = (now - lastRefillMs) / 60_000.0
        val add = (mins * refillPerMinute).toInt()
        if (add > 0) {
            tokens = min(capacity, tokens + add); lastRefillMs = now
        }
        return if (tokens > 0) {
            tokens--; true
        } else false
    }
}