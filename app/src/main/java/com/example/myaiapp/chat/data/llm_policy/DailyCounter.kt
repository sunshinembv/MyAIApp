package com.example.myaiapp.chat.data.llm_policy

import com.example.myaiapp.shared_preferences.SharedPreferencesProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyCounter(private val prefsProvider: SharedPreferencesProvider, private val role: String) {

    fun tryConsume(maxPerDay: Int): Boolean {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
            .format(Date(System.currentTimeMillis()))
        val savedDay = prefsProvider.getString("day", role, null)
        var used = if (savedDay == day) prefsProvider.getInt("used", role, 0) else 0
        return if (used < maxPerDay) {
            used += 1
            prefsProvider.putString("day", role, day)
            prefsProvider.putInt("used", role, used)
            true
        } else false
    }

    fun getRemaining(maxPerDay: Int): Int {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDay = prefsProvider.getString("day", role, null)
        val used = if (savedDay == day) prefsProvider.getInt("used", role, 0) else 0
        return (maxPerDay - used).coerceAtLeast(0)
    }
}