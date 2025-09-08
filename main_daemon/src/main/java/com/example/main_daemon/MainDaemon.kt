package com.example.main_daemon

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.time.Duration.Companion.seconds

@Serializable
data class RunMainReq(
    val className: String,           // FQCN, напр. com.example.tools.PatchKt
    val args: List<String> = emptyList(),
    val timeoutSec: Long = 300
)
@Serializable
data class RunMainRes(
    val ok: Boolean,
    val stdout: String,
    val stderr: String,
    val elapsedMs: Long,
    val error: String? = null
)

private fun ApplicationCall.isAuthOk(): Boolean {
    val token = System.getenv("MAIN_D_TOKEN") ?: return true // без переменной — auth off
    return request.headers["Authorization"] == "Bearer $token"
}

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080) {
        install(CallLogging)
        install(ContentNegotiation) { json() }

        routing {
            get("/health") { call.respondText("OK") }

            post("/run-main") {
                if (!call.isAuthOk()) {
                    call.respond(HttpStatusCode.Unauthorized, "Missing/invalid Authorization")
                    return@post
                }
                val ct = call.request.headers["Content-Type"]
                val raw = call.receiveText() // читаем ровно то, что пришло
                println("[run-main] CT=$ct RAW=$raw")

                val req = try {
                    Json { ignoreUnknownKeys = true; isLenient = true }
                        .decodeFromString<RunMainReq>(raw)
                } catch (e: Exception) {
                    println("[run-main] JSON error: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, "Bad JSON: ${e.message}")
                    return@post
                }

                println("[main-daemon] START class=${req.className} args=${req.args}")
                val start = System.nanoTime()
                val res = runCatching {
                    withTimeout(req.timeoutSec.seconds) {
                        invokeMain(req.className, req.args.toTypedArray())
                    }
                }

                val elapsed = (System.nanoTime() - start) / 1_000_000
                res.fold(onSuccess = { out ->
                    println("[main-daemon] DONE  class=${req.className} elapsed=${elapsed}ms")
                    call.respond(RunMainRes(ok = true, stdout = out.first, stderr = out.second, elapsedMs = elapsed))
                }, onFailure = { e ->
                    println("[main-daemon] FAIL  class=${req.className} error=${e.message}")
                    val cause = e.cause ?: e
                    val msg = cause::class.java.simpleName + ": " + (cause.message ?: "")
                    call.respond(RunMainRes(ok = false, stdout = "", stderr = "", elapsedMs = elapsed, error = msg))
                })
            }
        }
    }.start(wait = true)
}

/** Вызывает static fun main(String[]) у класса (в т.ч. Kotlin top-level — *Kt). */
private suspend fun invokeMain(className: String, args: Array<String>): Pair<String, String> =
    withContext(Dispatchers.IO) {
        val clazz = Class.forName(className)
        val method = clazz.methods.firstOrNull {
            it.name == "main" && it.parameterTypes.size == 1 && it.parameterTypes[0] == Array<String>::class.java
        } ?: error("No main(String[]) in $className")

        // Перехват stdout/stderr на время запуска:
        val oldOut = System.out
        val oldErr = System.err
        val bout = ByteArrayOutputStream()
        val berr = ByteArrayOutputStream()
        System.setOut(PrintStream(bout, true, Charsets.UTF_8))
        System.setErr(PrintStream(berr, true, Charsets.UTF_8))
        try {
            method.invoke(null, args as Any) // static main
        } finally {
            System.setOut(oldOut)
            System.setErr(oldErr)
        }
        bout.toString(Charsets.UTF_8) to berr.toString(Charsets.UTF_8)
    }