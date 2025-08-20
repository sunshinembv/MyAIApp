package com.example.myaiapp.chat.data.ssh

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.Security
import java.util.UUID

/**
 * Executes Kotlin snippets inside an isolated Docker container via SSH.
 *
 * This helper hides the details of connecting to the remote machine, uploading
 * source code via SFTP and launching the container. It limits the container
 * to a single CPU, 512 MiB of RAM and disables networking for safety. A
 * per‑invocation working directory under `/tmp/agent` is used to avoid
 * interfering with other runs.
 *
 * @param host address of the SSH host (emulator sees host as 10.0.2.2)
 * @param port port of the SSH server
 * @param username SSH username
 * @param password SSH password (or null if using a private key)
 * @param privateKeyPath optional path to a private key
 */
class SshDockerExecutor(
    private val host: String = "10.0.2.2",
    private val port: Int = 22,
    private val username: String,
    private val password: String? = null,
    private val privateKeyPath: String? = null,
) {
    /** Establish a new SSH connection with password or key authentication. */
    private fun connect(): SSHClient {
        Security.removeProvider("BC")
        val ssh = SSHClient()
        ssh.loadKeys(privateKeyPath, "")
        // В DEV допускаем любой host key. В проде задайте KnownHostsVerifier.
        ssh.addHostKeyVerifier(PromiscuousVerifier())
        ssh.connect(host, port)
        when {
            privateKeyPath != null -> {
                val keys = ssh.loadKeys(privateKeyPath)
                ssh.authPublickey(username, keys)
            }
            password != null       -> ssh.authPassword(username, password)
            else                   -> error("No authentication method provided")
        }
        return ssh
    }

    /** Результат запуска кода: id, код возврата и вывод. */
    data class RunResult(
        val jobId: String,
        val exitStatus: Int,
        val output: String,
    )

    /**
     * Загружает Kotlin-код и запускает его в контейнере `kotlin-runner`.
     * Каталог `/tmp/agent/<jobId>` используется как рабочая папка, потом удаляется.
     * Стандартный вывод и ошибка возвращаются в одном поле.
     */
    fun runKotlin(code: String): RunResult {
        val jobId = "job-" + UUID.randomUUID().toString().take(8)
        val remoteDir = "/tmp/agent/$jobId"
        val remoteFile = "$remoteDir/Main.kt"
        connect().use { ssh ->
            // 1) SFTP: create the work directory and upload the source file.
            val sftp = ssh.newSFTPClient()
            try {
                // Recursively create directories (equivalent to `mkdir -p`).
                fun mkdirs(path: String) {
                    val parts = path.split('/').filter { it.isNotEmpty() }
                    var current = ""
                    for (segment in parts) {
                        current += "/$segment"
                        try {
                            sftp.stat(current)
                        } catch (_: Throwable) {
                            sftp.mkdir(current)
                        }
                    }
                }
                mkdirs(remoteDir)
                // Пишем исходник во временный файл и заливаем его
                val tempFile = File.createTempFile("Main", ".kt")
                tempFile.writeText(code, Charsets.UTF_8)
                try {
                    sftp.put(tempFile.absolutePath, remoteFile)
                } finally {
                    tempFile.delete()
                }
            } finally {
                try {
                    sftp.close()
                } catch (_: Throwable) {
                    // игнорируем ошибки закрытия
                }
            }
            // 2) SSH: собираем и запускаем docker run.
            val script = """
                set -euo pipefail
                docker run --rm \
                  --network none \
                  --cpus 1 --memory 512m --pids-limit 256 \
                  -v $remoteDir:/work:rw \
                  kotlin-runner:latest sh -lc '
                    kotlinc /work/Main.kt -include-runtime -d /work/app.jar && \
                    java -jar /work/app.jar
                  '
            """.trimIndent()
            ssh.startSession().use { session: Session ->
                session.allocateDefaultPTY()
                val command = session.exec("/bin/sh -lc ${quote(script)}")
                val stdout = IOUtils.readFully(command.inputStream).toString(StandardCharsets.UTF_8)
                val stderr = IOUtils.readFully(command.errorStream).toString(StandardCharsets.UTF_8)
                command.join()
                val exit = command.exitStatus ?: -1
                return RunResult(jobId, exit, stdout + stderr)
            }
        }
    }

    /** Экранирует строку для безопасного включения в одиночные кавычки в shell. */
    private fun quote(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}