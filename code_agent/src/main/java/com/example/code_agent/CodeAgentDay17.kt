package com.example.code_agent

import kotlinx.coroutines.runBlocking
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.Properties

fun main(vararg args: String) = runBlocking {
    // ---- 1) Парсим вход ----
    // Поддерживаем:
    //   --file <name_or_relpath>
    //   --ask  "<user prompt>"
    // Если что-то не передали — спросим в консоли.
    val argMap = args.toList().chunked(2).filter { it.size == 2 }.associate { it[0] to it[1] }
    val fileQuery = argMap["--file"] ?: readLinePrompt("Введите имя файла (например, MainActivity.kt): ")
    val userAsk  = argMap["--ask"]  ?: readLinePrompt("Опишите, что нужно поправить (user prompt): ")

    // ---- 2) Ключ OpenRouter ----
    val apiKey = System.getenv("OPEN_ROUTER_API_KEY") ?: run {
        val f = java.io.File("local.properties")
        if (f.exists()) Properties().apply { load(f.inputStream()) }.getProperty("OPEN_ROUTER_API_KEY") else null
    } ?: error("Set OPEN_ROUTER_API_KEY env var or put it into local.properties")

    val api = createOpenRouterApi(apiKey)

    // ---- 3) Находим файл по имени или относительному пути ----
    val targetPath = resolveProjectFile(fileQuery)
        ?: error("Не нашёл файл по запросу '$fileQuery'. Проверь имя или путь.")

    println("🔎 Найден файл: $targetPath")

    // ---- 4) Гоним в LLM с пользовательским промптом ----
    try {
        val updated = LlmCodeFixer.fixFileWithLlm(
            api = api,
            model = "deepseek/deepseek-r1:free",
            filePath = targetPath.toString(),
            taskNote = userAsk
        )
        println("✅ Обновлён: $targetPath")
        println("----\n$updated")
        TokenTally.printlnSummary()
    } catch (t: Throwable) {
        System.err.println("❌ Ошибка: ${t.message}")
        t.printStackTrace()
    }
}

private fun readLinePrompt(prompt: String): String {
    print(prompt)
    return readln().trim()
}

private fun resolveProjectFile(query: String): Path? {
    val q = query.trim()
    val qPathLike = q.contains('/') || q.contains('\\')
    val root = Paths.get(".").toAbsolutePath().normalize()

    // 1) Если выглядит как путь и реально существует — возвращаем.
    if (qPathLike) {
        val p = root.resolve(q).normalize()
        if (Files.exists(p) && Files.isRegularFile(p)) return p
    }

    // 2) Иначе ищем по имени файла
    val candidates = mutableListOf<Path>()
    Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (file.fileName.toString().equals(q, ignoreCase = true)) {
                candidates.add(file.normalize())
            }
            return FileVisitResult.CONTINUE
        }
    })

    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.first()

    // 3) Выбираем лучший по "очкам"
    val scored = candidates.map { it to pathScore(it, root) }.sortedByDescending { it.second }
    val best = scored.first().first

    // Печатаем топ-5 совпадений на всякий случай
    println("Нашёл несколько совпадений, выбираю лучшее. Топ-5:")
    scored.take(5).forEachIndexed { i, (p, s) ->
        println("  ${i + 1}. $p  (score=$s)")
    }

    return best
}

private fun pathScore(p: Path, root: Path): Int {
    val rel = try { root.relativize(p) } catch (_: Exception) { p }
    val s = rel.toString().replace('\\', '/')
    var score = 0
    if (s.contains("app/src/main/java")) score += 100
    if (s.contains("app/src/main/kotlin")) score += 100
    if (s.contains("src/main/java")) score += 50
    if (s.contains("src/main/kotlin")) score += 50
    if (s.endsWith(".kt")) score += 10
    // короче путь предпочтительнее
    score += 20 - s.count { it == '/' }.coerceAtLeast(0)
    return score
}
