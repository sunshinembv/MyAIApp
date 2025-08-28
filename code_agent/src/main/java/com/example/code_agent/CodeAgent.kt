package com.example.code_agent

import com.example.code_agent.LlmCodeFixer.extractMethodSliceByLine
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.util.Properties

fun main(vararg args: String) = runBlocking {
    val argMap = args.toList().chunked(2).filter { it.size == 2 }.associate { it[0] to it[1] }
    val model = argMap["--model"] ?: "deepseek/deepseek-r1:free"

    val stack = argMap["--file"] ?: readLinePrompt("Введите стектрейс: ")
    val ask  = argMap["--ask"]  ?: readLinePrompt("Опишите, что нужно поправить (user prompt): ")

    // ---- Ключ OpenRouter: ENV → local.properties ----
    val apiKey = System.getenv("OPEN_ROUTER_API_KEY") ?: run {
        val f = java.io.File("local.properties")
        if (f.exists()) Properties().apply { load(f.inputStream()) }.getProperty("OPEN_ROUTER_API_KEY") else null
    } ?: error("Missing OPEN_ROUTER_API_KEY (env var или local.properties)")

    val api = createOpenRouterApi(apiKey)

    val frame = parseTopFramesFlexible(stack).first()


    val filePath: Path = resolveFilePath(frame.fileName)
        ?: error("Файл ${frame.fileName} не найден в репозитории")

    // ---- Извлекаем минимальный слайс (метод/класс) вокруг строки из стека ----
    val src = java.io.File(filePath.toString()).readText()
    val slice = extractMethodSliceByLine(src, frame.line)

    // ---- Шлём в LLM только целевой слайс + короткий ask; вшиваем результат ----
    val updated = fixWithStrictUnifiedDiff(
        api = api,
        model = model,
        filePath = filePath.toString(),
        ask = ask.ifBlank { "Fix the issue indicated by the stacktrace." },
        slice = slice
    )

    println("✅ Updated: $filePath")
    println("----\n$updated")
    TokenTally.printlnSummary()
}

private fun readLinePrompt(prompt: String): String {
    print(prompt)
    return readln().trim()
}
