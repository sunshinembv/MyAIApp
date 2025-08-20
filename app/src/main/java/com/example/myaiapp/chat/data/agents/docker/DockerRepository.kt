package com.example.myaiapp.chat.data.agents.docker

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaOptions
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.data.ssh.SshDockerExecutor
import com.example.myaiapp.chat.data.ssh.SshDockerExecutor.RunResult
import com.example.myaiapp.chat.domain.PromptBuilder
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.OutputFormat
import com.example.myaiapp.network.AIApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DockerRepository @Inject constructor(
    private val api: AIApi,
) {

    private val history: MutableList<OllamaChatMessage> = mutableListOf(
        OllamaChatMessage(Role.SYSTEM, PromptBuilder.systemPrompt(OutputFormat.DOCKER_KOTLIN))
    )

    suspend fun callLlmToDocker(content: String, login: String, key: String): RunResult {
        return withContext(Dispatchers.IO) {
            history += OllamaChatMessage(Role.USER, content)
            val request = OllamaChatRequest(
                model = LlmModels.MISTRAL.modelName,
                messages = history,
                options = OllamaOptions(
                    temperature = 0.1,
                    topP = 0.95,
                    numCtx = 4096,
                    //stop = listOf("```") // страховка от кодовых блоков
                ),
                stream = false,
                keepAlive = "5m"
            )

            val response = api.chatOnce(request)
            val formatedResponse = response.message.content.replace("`", "")
            history += response.message

            val exec = SshDockerExecutor(username = login, privateKeyPath = key)
            val res = exec.runKotlin(formatedResponse)
            res
        }
    }
}