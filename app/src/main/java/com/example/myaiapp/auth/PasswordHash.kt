package com.example.myaiapp.auth

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.max

object PasswordHash {
    private const val ITER = 120_000
    private const val KEY_LEN = 256 // bits
    private const val SALT_LEN = 16

    fun newSalt(): ByteArray = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }

    fun hash(password: CharArray, salt: ByteArray, iter: Int = ITER): ByteArray {
        val spec = PBEKeySpec(password, salt, iter, KEY_LEN)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
    }

    fun encode(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    fun decode(s: String) = Base64.decode(s, Base64.NO_WRAP)

    // тайминг-стойкое сравнение
    fun slowEquals(a: ByteArray, b: ByteArray): Boolean {
        val maxLen = max(a.size, b.size)
        var diff = a.size xor b.size
        for (i in 0 until maxLen) {
            val av = if (i < a.size) a[i].toInt() else 0
            val bv = if (i < b.size) b[i].toInt() else 0
            diff = diff or (av xor bv)
        }
        return diff == 0
    }
}