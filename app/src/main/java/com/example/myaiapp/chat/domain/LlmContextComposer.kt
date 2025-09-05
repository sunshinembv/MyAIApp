package com.example.myaiapp.chat.domain

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.memory.data.repository.PersonalizationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LlmContextComposer @Inject constructor(private val repo: PersonalizationRepository) {

    suspend fun buildSystemPrompt(): String {
        val p  = repo.profileFlow.first()
        val ap = repo.prefsFlow.first()
        val mem = repo.selectContextMemories(8)

        return buildString {
            appendLine("Ты персональный помощник для ${p.name} (${p.city}).")
            appendLine("Часовой пояс: ${p.timezone}. Язык по умолчанию: ${ap.defaultLanguage}.")
            appendLine("Тон: ${ap.tone.name.lowercase()}, детализация: ${ap.detail.name.lowercase()}, формат: ${ap.style.name.lowercase()}.")
            if (ap.useEmoji) appendLine("Допускаются уместные эмодзи.")
            if (p.rolesCount > 0) appendLine("Роли: ${p.rolesList.joinToString()}.")
            if (p.interestsCount > 0) appendLine("Интересы: ${p.interestsList.joinToString()}.")
            if (ap.quietHoursCount > 0) {
                val qh = ap.quietHoursList.joinToString { "${it.start}-${it.end}" }
                appendLine("Тихие часы: $qh.")
            }
            if (mem.isNotEmpty()) {
                appendLine("Устойчивые факты о пользователе:")
                mem.forEachIndexed { i, m -> appendLine("${i+1}. [${m.importance}] ${m.text}") }
            }
            appendLine("Отвечай кратко и по делу, учитывая предпочтения.")
        }.trim()
    }

    // Собираем короткий extract-промпт из последних N сообщений диалога
    fun buildExtractPrompt(history: List<OllamaChatMessage>, lastN: Int = 6): String {
        val slice = history.takeLast(lastN)
        val rendered = slice.joinToString("\n") {
            when (it.role) {
                Role.USER  -> "user: ${it.content}"
                Role.ASSISTANT -> "assistant: ${it.content}"
                else -> "${it.role}: ${it.content}"
            }
        }
        return """
        Извлеки устойчивые факты о пользователе из диалога ниже.
        Верни ТОЛЬКО JSON-массив объектов: [{"text": "...", "importance": 1..5}, ...]
        Ничего больше не добавляй. Если фактов нет — верни [].

        <dialog>
        $rendered
        </dialog>
    """.trimIndent()
    }
}
