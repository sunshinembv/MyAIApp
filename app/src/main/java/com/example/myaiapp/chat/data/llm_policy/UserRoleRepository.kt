package com.example.myaiapp.chat.data.llm_policy

import com.example.myaiapp.chat.domain.model.UserRole
import com.example.myaiapp.security.SecurePrefs
import javax.inject.Inject

class UserRoleRepository @Inject constructor(
    private val prefs: SecurePrefs,
) {

    suspend fun getCurrentEmail(): String? = prefs.getDecrypted(KEY_EMAIL)

    suspend fun getRole(): UserRole {
        val r = prefs.getDecrypted(KEY_ROLE) ?: return UserRole.GUEST // дефолт
        return runCatching { UserRole.valueOf(r) }.getOrElse { UserRole.GUEST }
    }

    /** Установить роль для текущего пользователя. */
    suspend fun setRole(role: UserRole) {
        // (опционально) требовать подтверждения паролем/пин-кодом здесь
        prefs.putEncrypted(KEY_ROLE, role.name)
        UserRoleHolder.role = role
    }

    /** Гейтинг: бросает IllegalAccessException если роль ниже требуемой. */
    suspend fun requireAtLeast(minRole: UserRole) {
        val current = getRole()
        val ok = current.ordinal >= minRole.ordinal
        if (!ok) throw IllegalAccessException("Role $current is below required $minRole")
    }

    companion object Companion {
        private const val KEY_EMAIL = "auth.email" // уже есть в AuthRepository
        private const val KEY_ROLE = "auth.role"  // роль текущего email
    }
}

object UserRoleHolder {
    var role = UserRole.GUEST
}