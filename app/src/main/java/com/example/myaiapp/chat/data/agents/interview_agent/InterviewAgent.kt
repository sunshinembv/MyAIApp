package com.example.myaiapp.chat.data.agents.interview_agent

import com.example.myaiapp.chat.data.LlmContentParser
import com.example.myaiapp.chat.data.agents.interview_agent.model.InterviewAgentTurn
import com.example.myaiapp.chat.data.model.Ask
import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaOptions
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.data.model.Summary
import com.example.myaiapp.chat.data.toOllama
import com.example.myaiapp.chat.data.toOpenRouter
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject

class InterviewAgent @Inject constructor(
    private val mistralApi: MistralApi,
    private val openRouterApi: OpenRouterApi,
    private val llmContentParser: LlmContentParser,
) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val askAdapter = moshi.adapter(Ask::class.java)
    private val summaryAdapter = moshi.adapter(Summary::class.java)

    private val systemPrompt: String = """
        Вы — эксперт по мобильным продуктам и ИНТЕРВЬЮЕР. На каждом шаге выводите РОВНО ОДИН JSON-объект и НИЧЕГО БОЛЬШЕ, затем ставьте маркер <<<END>>>. Когда собрано достаточно контекста, дайте финальный SUMMARY.

            ДОПУСТИМЫЕ ФОРМАТЫ
            1) ASK (задайте один вопрос):
            {"mode":"ask","q":"<короткий вопрос, ≤120 символов>"}<<<END>>>
            Правила для ASK: один вопрос; без советов/обучалок/подтверждений; не обсуждать IDE/инструменты/эмуляторы/консоли; не повторяться; грамотный русский, вежливое «вы».

            2) SUMMARY (финал):
            {"title":"Summary","subtitle":"Project: PROJECT_NAME","summary":"..."}<<<END>>>
            Требования к SUMMARY:
            - PROJECT_NAME — из ответов; если нет — "TBD".
            - "summary" ёмко (можно переносы строк и маркеры "- "): цель и ЦА, платформы, ключевые фичи (3–7), архитектура/стек верхнего уровня, интеграции (авторизация, платежи, карты/гео, пуши, аналитика, сторонние API), приватность/комплаенс (GDPR/PII), сроки/этапы, ограничения/риски/команда, метрики/монетизация, next steps.

            ЖЁСТКИЕ ПРАВИЛА ФОРМАТА
            - В каждом сообщении — РОВНО ОДИН JSON и сразу <<<END>>>. Никакого текста/списков/префиксов до или после.
            - Если нарушили формат — немедленно повторите корректный вариант и завершите <<<END>>>.
            - Никогда не выводите одновременно и ASK, и SUMMARY в одном сообщении.

            КРИТИЧЕСКИЕ УСЛОВИЯ ДЛЯ SUMMARY (модель обязана соблюдать)
            - Минимум **6 последовательных ASK-шагов** (6 ваших сообщений в режиме ASK) перед первой попыткой SUMMARY.
            - Обязательные 5 полей (все пять должны быть явно получены от пользователя, без выдумывания и "TBD"):
              A) PROJECT_NAME (название проекта)
              B) Платформы (Android/iOS/обе/кроссплатформа)
              C) Целевая аудитория И формулировка проблемы (оба аспекта)
              D) MVP-фичи: минимум 3 отдельные фичи
              E) Сроки/этапы (MVP/бета/релиз с ориентиром дат)
            - Дополнительно к этому доведите покрытие до **≥80%** чек-листа ниже (≥10 из 12 пунктов). Только после этого выводите SUMMARY.

            ВНУТРЕННИЙ ЧЕК-ЛИСТ (не печатать пользователю)
            1) Название проекта (PROJECT_NAME)
            2) Платформы
            3) ЦА и проблема (JTBD)
            4) Ценность/бизнес-цель
            5) MVP-фичи (3–7)
            6) Статус дизайна/бренда
            7) Бэкенд/интеграции (авторизация, платежи, карты/гео, пуши, аналитика, сторонние API)
            8) Данные/приватность/комплаенс (GDPR/PII, регион хранения)
            9) Нефункциональные требования (производительность, офлайн, безопасность, локализация/доступность)
            10) Сроки/этапы (MVP/бета/релиз с датами)
            11) Ограничения и команда (бюджет, роли, техограничения)
            12) Метрики успеха и монетизация

            ВЫБОР СЛЕДУЮЩЕГО ВОПРОСА
            - Спрашивайте самый полезный недостающий пункт; ≤120 символов; без уточнений к своим же вопросам и без «правильно ли я понял…».
            - Если ответ «не знаю/пока нет» — внутренне пометьте как TBD и двигайтесь дальше.

            FEW-SHOT ПРИМЕР (строго следовать структуре)
            Пользователь: Хочу мобильное приложение
            Ассистент: {"mode":"ask","q":"На какой платформе планируете разработку?"}<<<END>>>
            Пользователь: Android
            Ассистент: {"mode":"ask","q":"Как называется проект?"}<<<END>>>
            Пользователь: Навигатор
            Ассистент: {"mode":"ask","q":"Кто ваша целевая аудитория и какую проблему решаем?"}<<<END>>>
            Пользователь: Водители; быстро строить оптимальные маршруты
            Ассистент: {"mode":"ask","q":"Какие ключевые фичи войдут в MVP (3–7)?"}<<<END>>>
            Пользователь: Маршруты, трафик, голосовые подсказки
            Ассистент: {"mode":"ask","q":"Сроки: MVP/бета/релиз и ориентировочные даты?"}<<<END>>>
            Пользователь: MVP — октябрь 2025
            Ассистент: {"mode":"ask","q":"Нужны ли карты/гео, платежи, авторизация, пуши, аналитика?"}<<<END>>>
            Пользователь: Карты, пуши, аналитика
            Ассистент: {"title":"Summary","subtitle":"Project: Навигатор","summary":"..."}<<<END>>>

            СТАРТ
            Если ничего не известно — спросите платформы или название проекта. Далее действуйте по правилам выше.
            
            ПРИОРИТЕТ ВОПРОСОВ (внутренне, НЕ печатать)
            1) A) PROJECT_NAME → B) Платформы → C) ЦА+проблема (оба аспекта) → D) MVP-фичи (≥3) → E) Сроки/этапы.
            2) Лишь после A–E переходите к остальным пунктам (дизайн, интеграции, приватность, нефункциональные, ограничения/бюджет, метрики).
            3) Никогда не повторяйте последний заданный вопрос. Если на него получен ответ — пометьте пункт как заполненный и задайте следующий самый полезный недостающий.
            4) Не задавайте вопросы про инвестиции/бюджет до завершения A–E (если пользователь сам не поднял тему ограничения бюджета).
    """.trimIndent()

    private val options = OllamaOptions(
        temperature = 0.0,
        topP = 1.0,
        topK = 0,
        repeatPenalty = 1.25,
        numCtx = 4096,
        numPredict = null,
        stop = listOf("<<<END>>>")
    )

    fun seedHistory(): MutableList<OllamaChatMessage> = mutableListOf(
        OllamaChatMessage(Role.SYSTEM, systemPrompt)
    )

    suspend fun next(
        messages: List<OllamaChatMessage>,
        model: LlmModels,
    ): InterviewAgentTurn {
        require(messages.firstOrNull()?.role == Role.SYSTEM) { "SYSTEM must be first" }
        require(messages.lastOrNull()?.role == Role.USER) { "Last must be USER" }

        val request = OllamaChatRequest(
            model = model.modelName,
            messages = messages,
            options = options,
            format = "json"
        )

        val response = when (model) {
            LlmModels.MISTRAL -> {
                mistralApi.chatOnce(request)
            }
            LlmModels.DEEPSEEK_FREE -> {
                openRouterApi.chat(request.toOpenRouter()).toOllama()
            }
        }

        val normalized =
            (llmContentParser.normalizeAssistantContent(response.message.content))
            ?: error("Interview Agent: no JSON found")

        val json = normalized.removeSuffix("<<<END>>>")

        val turn = runCatching { askAdapter.fromJson(json) }.getOrNull()
            ?: runCatching { summaryAdapter.fromJson(json) }.getOrNull()
            ?: error("Unknown JSON shape")

        return when(turn) {
            is Ask -> {
                InterviewAgentTurn.Ask(
                    question = turn.q,
                    rawAssistant = response.message.copy(content = normalized)
                )
            }
            is Summary -> {
                InterviewAgentTurn.Summary(
                    summaryJson = json,
                    summary = turn,
                    rawAssistant = response.message.copy(content = normalized)
                )
            }

            else -> { error("Unknown JSON shape") }
        }

    }
}
