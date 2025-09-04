package com.example.myaiapp.chat.domain.llm_policy

import com.example.myaiapp.chat.domain.model.UserRole

data class LlmPolicy(
    val userRole: UserRole,
    val canUseRemoteLLM: Boolean,
    val rpmLimit: Int,     // запросов в минуту
    val dailyLimit: Int    // запросов в сутки
)

object LlmPolicies {
    val map = mapOf(
        UserRole.GUEST to LlmPolicy(UserRole.GUEST, canUseRemoteLLM = false, rpmLimit = 6, dailyLimit = 2),
        UserRole.USER  to LlmPolicy(UserRole.USER,  canUseRemoteLLM = true,  rpmLimit = 20, dailyLimit = 3),
        UserRole.POWER to LlmPolicy(UserRole.POWER, canUseRemoteLLM = true,  rpmLimit = 60, dailyLimit = 2000),
        UserRole.ADMIN to LlmPolicy(UserRole.ADMIN, canUseRemoteLLM = true,  rpmLimit = 120, dailyLimit = 10000)
    )
}