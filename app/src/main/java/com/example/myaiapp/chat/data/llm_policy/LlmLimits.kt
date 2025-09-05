package com.example.myaiapp.chat.data.llm_policy

import com.example.myaiapp.chat.domain.model.UserRole
import com.example.myaiapp.shared_preferences.SharedPreferencesProvider
import javax.inject.Inject

class LlmLimits @Inject constructor(private val prefsProvider: SharedPreferencesProvider) {

    val dailyByRole = mapOf(
        UserRole.USER to DailyCounter(prefsProvider, UserRole.USER.name),
        UserRole.POWER to DailyCounter(prefsProvider, UserRole.POWER.name),
        UserRole.ADMIN to DailyCounter(prefsProvider, UserRole.ADMIN.name),
        UserRole.GUEST to DailyCounter(prefsProvider, UserRole.GUEST.name),
    )

    val bucketByRole = mapOf(
        UserRole.USER to TokenBucket(capacity = 20, refillPerMinute = 20),
        UserRole.POWER to TokenBucket(capacity = 60, refillPerMinute = 60),
        UserRole.ADMIN to TokenBucket(capacity = 120, refillPerMinute = 120),
        UserRole.GUEST to TokenBucket(capacity = 6, refillPerMinute = 6),
    )
}