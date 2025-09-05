package com.example.myaiapp.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoBox(private val key: SecretKey) {
    // формат: [1 байт version=1][12 байт IV][ciphertext+tag]
    fun encryptToBase64(plain: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)          // ← без GCMParameterSpec
        val iv = cipher.iv                              // ← провайдер сгенерировал IV (обычно 12 байт)
        val enc = cipher.doFinal(plain)

        val out = ByteArray(1 + iv.size + enc.size)
        out[0] = 1
        System.arraycopy(iv, 0, out, 1, iv.size)
        System.arraycopy(enc, 0, out, 1 + iv.size, enc.size)
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decryptFromBase64(b64: String): ByteArray {
        val all = Base64.decode(b64, Base64.NO_WRAP)
        require(all.isNotEmpty() && all[0].toInt() == 1) { "Unsupported blob" }

        val iv = all.copyOfRange(1, 13)                // 12-байтовый IV
        val enc = all.copyOfRange(13, all.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(enc)
    }
}