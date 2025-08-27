package com.example.code_agent

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

data class TraceFrame(val fileName: String, val line: Int, val classFqn: String?)

/** Ищем путь к файлу в репозитории по имени (с приоритетом src/main/java|kotlin). */
fun resolveFilePath(fileName: String, root: Path = Paths.get(".")): Path? {
    val matches = mutableListOf<Path>()
    Files.walkFileTree(root, object : java.nio.file.SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult {
            if (file.fileName.toString().equals(fileName, ignoreCase = true)) matches.add(file)
            return java.nio.file.FileVisitResult.CONTINUE
        }
    })
    if (matches.isEmpty()) return null
    if (matches.size == 1) return matches.first()
    return matches.sortedWith(
        compareBy<Path>(
            { !it.toString().contains("app/src/main/java") && !it.toString().contains("app/src/main/kotlin") },
            { it.toString().count { c -> c == '/' || c == '\\' } }
        )
    ).first()
}

// 1) at com.pkg.Class.method(File.kt:123)
// 2) ... File.kt:123
// 3) ... File.kt line 123
fun parseTopFramesFlexible(trace: String, maxFrames: Int = 5): List<TraceFrame> {
    val frames = mutableListOf<TraceFrame>()

    val p1 = Regex("""at\s+([A-Za-z0-9_.$]+)\([^\n]*?([A-Za-z0-9_]+\.(?:kt|java)):(\d+)\)""")
    val p2 = Regex("""\b([A-Za-z0-9_]+\.(?:kt|java)):(\d+)\b""")
    val p3 = Regex("""\b([A-Za-z0-9_]+\.(?:kt|java))\s+line\s+(\d+)\b""", RegexOption.IGNORE_CASE)

    // try #1
    for (m in p1.findAll(trace)) {
        frames += TraceFrame(fileName = m.groupValues[2], line = m.groupValues[3].toInt(), classFqn = m.groupValues[1])
        if (frames.size >= maxFrames) return frames
    }
    if (frames.isNotEmpty()) return frames

    // try #2
    for (m in p2.findAll(trace)) {
        frames += TraceFrame(fileName = m.groupValues[1], line = m.groupValues[2].toInt(), classFqn = null)
        if (frames.size >= maxFrames) return frames
    }
    if (frames.isNotEmpty()) return frames

    // try #3
    for (m in p3.findAll(trace)) {
        frames += TraceFrame(fileName = m.groupValues[1], line = m.groupValues[2].toInt(), classFqn = null)
        if (frames.size >= maxFrames) return frames
    }

    return frames
}