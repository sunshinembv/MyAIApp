package com.example.code_agent

object TokenTally {
    private var prompt = 0
    private var completion = 0
    private var total = 0

    fun add(u: Usage?) {
        if (u == null) return
        prompt += u.promptTokens
        completion += u.completionTokens
        total += u.totalTokens
    }

    fun printlnStep(tag: String, u: Usage?) {
        if (u == null) {
            println("🔢 [$tag] usage: (no usage returned)")
        } else {
            println("🔢 [$tag] usage: prompt=${u.promptTokens}, completion=${u.completionTokens}, total=${u.totalTokens}")
        }
    }

    fun printlnSummary() {
        println("🧮 Tokens total: prompt=$prompt, completion=$completion, total=$total")
    }
}