package com.example.myaiapp.shared_preferences

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject

interface SharedPreferencesProvider {
    fun getString(key: String, role: String, defaultValue: String?): String
    fun putString(key: String, role: String, value: String)
    fun putInt(key: String, role: String, value: Int)
    fun getInt(key: String, role: String, defaultValue: Int): Int
}

class SharedPreferencesProviderImpl @Inject constructor(private val context: Context) :
    SharedPreferencesProvider {

    private var prefs: SharedPreferences = context.getSharedPreferences(
        "llm_limits_prefs",
        Context.MODE_PRIVATE
    )

    override fun getString(key: String, role: String, defaultValue: String?): String {
        return prefs.getString("${role}_$key", defaultValue).orEmpty()
    }

    override fun putString(key: String, role: String, value: String) {
        prefs.edit().putString("${role}_$key", value)?.apply()
    }

    override fun putInt(key: String, role: String, value: Int) {
        prefs.edit().putInt("${role}_$key", value)?.apply()
    }

    override fun getInt(key: String, role: String, defaultValue: Int): Int {
        return prefs.getInt("${role}_$key", defaultValue) ?: 0
    }
}
