package com.example.myaiapp.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private val Context.secureStore by preferencesDataStore("secure_prefs")

class SecurePrefs @Inject constructor(context: Context) {
    private val ds = context.secureStore

    private val keystoreAlias: String = "my_secure_master_key"
    private val crypto = CryptoBox(KeystoreAesGcm.getOrCreateKey(keystoreAlias))

    suspend fun putEncrypted(key: String, value: String) {
        val k = stringPreferencesKey(key)
        val b64 = crypto.encryptToBase64(value.encodeToByteArray())
        ds.edit { it[k] = b64 }
    }

    suspend fun getDecrypted(key: String): String? {
        val k = stringPreferencesKey(key)
        val prefs = try { ds.data.first() } catch (_: Exception) { emptyPreferences() }
        val b64 = prefs[k] ?: return null
        return crypto.decryptFromBase64(b64).decodeToString()
    }

    suspend fun remove(key: String) {
        val k = stringPreferencesKey(key)
        ds.edit { it.remove(k) }
    }

    // синхронный helper для редких случаев
    fun getDecryptedBlocking(key: String): String? = runBlocking { getDecrypted(key) }
}