package com.example.myaiapp.chat.domain.model

enum class LlmModels(val modelName: String) {
    MISTRAL("mistral:instruct"),
    DEEPSEEK_FREE("deepseek/deepseek-r1:free"),
}