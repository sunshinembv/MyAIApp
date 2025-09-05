package com.example.myaiapp.memory

import com.example.myaiapp.AgentPrefs
import com.example.myaiapp.memory.data.models.AgentConfigV1
import com.example.myaiapp.memory.data.models.AgentPrefsDTO
import com.example.myaiapp.memory.data.models.MemoryDTO
import com.example.myaiapp.memory.data.models.ProfileDTO
import com.example.myaiapp.memory.data.models.QuietHourDTO
import com.example.myaiapp.memory.data.repository.PersonalizationRepository
import com.example.myaiapp.quietHour
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ImportExportConfig @Inject constructor() {

    val moshi: Moshi = Moshi.Builder().build()
    val agentConfigAdapter = moshi.adapter(AgentConfigV1::class.java).indent("  ")

    suspend fun exportConfig(repo: PersonalizationRepository): String {
        val p = repo.profileFlow.first()
        val ap = repo.prefsFlow.first()
        val mem = repo.selectContextMemories(50)

        val dto = AgentConfigV1(
            profile = ProfileDTO(
                p.name,
                p.locale,
                p.timezone,
                p.city,
                p.rolesList,
                p.interestsList
            ),
            prefs = AgentPrefsDTO(
                tone = ap.tone.name, style = ap.style.name, detail = ap.detail.name,
                useEmoji = ap.useEmoji, defaultLanguage = ap.defaultLanguage,
                allowedModels = ap.allowedModelsList,
                shareProfileWithRemoteLLM = ap.shareProfileWithRemoteLlm,
                shareMemoriesWithRemoteLLM = ap.shareMemoriesWithRemoteLlm,
                quietHours = ap.quietHoursList.map { QuietHourDTO(it.start, it.end) }
            ),
            memories = mem.map { MemoryDTO(it.text, it.importance, it.kind) }
        )
        return agentConfigAdapter.toJson(dto)
    }

    suspend fun importConfig(repo: PersonalizationRepository, json: String) {
        val dto = agentConfigAdapter.fromJson(json) ?: error("Invalid config JSON")
        require(dto.version == 1) { "Unsupported config version: ${dto.version}" }

        repo.updateProfile {
            name = dto.profile.name
            locale = dto.profile.locale
            timezone = dto.profile.timezone
            city = dto.profile.city
            roles.clear(); roles += dto.profile.roles
            interests.clear(); interests += dto.profile.interests
        }

        repo.updatePrefs {
            tone = enumOrDefault(dto.prefs.tone, AgentPrefs.Tone.TONE_UNSPECIFIED)
            style = enumOrDefault(dto.prefs.style, AgentPrefs.Style.STYLE_UNSPECIFIED)
            detail = enumOrDefault(dto.prefs.detail, AgentPrefs.Detail.DETAIL_UNSPECIFIED)
            useEmoji = dto.prefs.useEmoji
            defaultLanguage = dto.prefs.defaultLanguage
            allowedModels.clear(); allowedModels += dto.prefs.allowedModels
            shareProfileWithRemoteLlm = dto.prefs.shareProfileWithRemoteLLM
            shareMemoriesWithRemoteLlm = dto.prefs.shareMemoriesWithRemoteLLM
            quietHours.clear(); dto.prefs.quietHours.forEach { q ->
            quietHours += quietHour { start = q.start; end = q.end }
        }
        }

        dto.memories.forEach { repo.addMemory(it.text, it.importance, it.kind, "import") }
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String, default: E): E =
        runCatching {
            java.lang.Enum.valueOf(
                E::class.java,
                name.uppercase()
            )
        }.getOrDefault(default)
}