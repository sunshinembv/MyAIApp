package com.example.myaiapp.auth.data

import com.example.myaiapp.auth.PasswordHash
import com.example.myaiapp.chat.data.llm_policy.UserRoleHolder
import com.example.myaiapp.chat.domain.model.UserRole
import com.example.myaiapp.security.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

/**
 * Хранит только base64-хэши и соли (для пароля и PIN), сами значения шифруются в DataStore (SecurePrefs).
 * PIN имеет счётчик попыток и временную блокировку.
 */
class AuthRepository @Inject constructor(
    private val prefs: SecurePrefs
) {

    /** Регистрация: сохраняет email и хэш пароля, сбрасывает PIN и блокировки. */
    suspend fun register(email: String, password: CharArray) = io {
        require(email.isNotBlank()) { "Email is blank" }
        require(password.size >= 8) { "Password must be ≥ 8 chars" }

        val salt = PasswordHash.newSalt()
        val hash = PasswordHash.hash(password, salt) // PBKDF2 HMAC-SHA256 (120k итераций)

        prefs.putEncrypted(KEY_EMAIL, email.trim())
        prefs.putEncrypted(KEY_PWD_SALT, PasswordHash.encode(salt))
        prefs.putEncrypted(KEY_PWD_HASH, PasswordHash.encode(hash))

        // роль по умолчанию
        prefs.putEncrypted(KEY_ROLE, UserRole.USER.name)
        UserRoleHolder.role = UserRole.USER

        // сброс PIN и блокировок
        prefs.remove(KEY_PIN_SALT)
        prefs.remove(KEY_PIN_HASH)
        setPinLockState(MAX_RETRY, 0L)
        prefs.putEncrypted("auth.role", UserRole.USER.name)
    }

    /** Установка 4-значного PIN. */
    suspend fun setPin(pin: CharArray) = io {
        require(pin.size == 4 && pin.all { it.isDigit() }) { "PIN must be 4 digits" }
        checkRegistered()

        val salt = PasswordHash.newSalt()
        // Для PIN снижаем итерации, но опираемся на блокировки
        val hash = PasswordHash.hash(pin, salt, iter = 50_000)

        prefs.putEncrypted(KEY_PIN_SALT, PasswordHash.encode(salt))
        prefs.putEncrypted(KEY_PIN_HASH, PasswordHash.encode(hash))
        setPinLockState(MAX_RETRY, 0L)
    }

    /** Проверка PIN с учётом блокировок/счётчика. */
    suspend fun verifyPin(pin: CharArray): PinResult = io {
        // Проверка блокировки
        val (retriesLeft, unlockAt) = getPinLockState()
        val now = System.currentTimeMillis()
        if (unlockAt > now) {
            return@io PinResult.Locked(unlockAtMs = unlockAt)
        }

        val saltB64 = prefs.getDecrypted(KEY_PIN_SALT) ?: return@io PinResult.Failure("PIN not set")
        val hashB64 = prefs.getDecrypted(KEY_PIN_HASH) ?: return@io PinResult.Failure("PIN not set")

        val salt = PasswordHash.decode(saltB64)
        val expected = PasswordHash.decode(hashB64)
        val actual = PasswordHash.hash(pin, salt, iter = 50_000)

        val ok = PasswordHash.slowEquals(expected, actual)
        if (ok) {
            setPinLockState(MAX_RETRY, 0L)
            PinResult.Success
        } else {
            val left = (retriesLeft - 1).coerceAtLeast(0)
            val newUnlock = if (left == 0) now + LOCKOUT_MS else 0L
            setPinLockState(left, newUnlock)
            if (left == 0) PinResult.Locked(unlockAtMs = newUnlock) else PinResult.RetryLeft(left)
        }
    }

    /** Сброс PIN после проверки пароля. Возвращает true/false. */
    suspend fun resetPinWithPassword(password: CharArray): Boolean = io {
        if (!verifyPassword(password)) return@io false
        prefs.remove(KEY_PIN_SALT)
        prefs.remove(KEY_PIN_HASH)
        setPinLockState(MAX_RETRY, 0L)
        true
    }

    /** Полный сброс учётки (лог-аут/очистка). */
    suspend fun clearAll() = io {
        prefs.remove(KEY_EMAIL)
        prefs.remove(KEY_PWD_SALT)
        prefs.remove(KEY_PWD_HASH)
        prefs.remove(KEY_PIN_SALT)
        prefs.remove(KEY_PIN_HASH)
        prefs.remove(KEY_PIN_RETRY_LEFT)
        prefs.remove(KEY_PIN_UNLOCK_AT)
        prefs.remove(KEY_ROLE)
    }

    // -------- Хелперы для UI/Bootstrap --------

    /** Есть ли зарегистрированный пользователь. */
    suspend fun isRegistered(): Boolean = io {
        prefs.getDecrypted(KEY_EMAIL) != null &&
                prefs.getDecrypted(KEY_PWD_HASH) != null &&
                prefs.getDecrypted(KEY_PWD_SALT) != null
    }

    /** Требуется ли первичная установка PIN. */
    suspend fun isPinSet(): Boolean = io {
        prefs.getDecrypted(KEY_PIN_HASH) != null && prefs.getDecrypted(KEY_PIN_SALT) != null
    }

    /** Текст для ретраев/блокировок (для удобного вывода в UI). */
    suspend fun lockInfoText(nowMs: Long = System.currentTimeMillis()): String? = io {
        val (left, unlockAt) = getPinLockState()
        when {
            unlockAt > nowMs -> "Locked until ${Date(unlockAt)}"
            left < MAX_RETRY -> "Attempts left: $left"
            else -> null
        }
    }

    private suspend fun verifyPassword(password: CharArray): Boolean {
        val saltB64 = prefs.getDecrypted(KEY_PWD_SALT) ?: return false
        val hashB64 = prefs.getDecrypted(KEY_PWD_HASH) ?: return false
        val salt = PasswordHash.decode(saltB64)
        val expected = PasswordHash.decode(hashB64)
        val actual = PasswordHash.hash(password, salt)
        return PasswordHash.slowEquals(expected, actual)
    }

    private suspend fun checkRegistered() {
        if (!isRegistered()) error("No account")
    }

    private suspend fun setPinLockState(retryLeft: Int, unlockAtMs: Long) {
        prefs.putEncrypted(KEY_PIN_RETRY_LEFT, retryLeft.toString())
        prefs.putEncrypted(KEY_PIN_UNLOCK_AT, unlockAtMs.toString())
    }

    private suspend fun getPinLockState(): Pair<Int, Long> {
        val left = prefs.getDecrypted(KEY_PIN_RETRY_LEFT)?.toIntOrNull() ?: MAX_RETRY
        val at = prefs.getDecrypted(KEY_PIN_UNLOCK_AT)?.toLongOrNull() ?: 0L
        return left to at
    }

    private suspend inline fun <T> io(crossinline block: suspend () -> T): T =
        withContext(Dispatchers.IO) { block() }

    companion object {
        private const val KEY_EMAIL = "auth.email"
        private const val KEY_PWD_SALT = "auth.pwd.salt"
        private const val KEY_PWD_HASH = "auth.pwd.hash"
        private const val KEY_PIN_SALT = "auth.pin.salt"
        private const val KEY_PIN_HASH = "auth.pin.hash"
        private const val KEY_PIN_RETRY_LEFT = "auth.pin.retry_left"
        private const val KEY_PIN_UNLOCK_AT = "auth.pin.unlock_at"
        private const val KEY_ROLE = "auth.role"

        const val MAX_RETRY = 5
        const val LOCKOUT_MS = 5 * 60_000L // 5 минут
    }
}

/** Результаты проверки PIN. */
sealed interface PinResult {
    data object Success : PinResult
    data class RetryLeft(val count: Int) : PinResult
    data class Locked(val unlockAtMs: Long) : PinResult
    data class Failure(val reason: String) : PinResult
}