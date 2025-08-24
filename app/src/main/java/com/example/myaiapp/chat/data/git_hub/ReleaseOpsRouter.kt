package com.example.myaiapp.chat.data.git_hub

import com.example.myaiapp.chat.data.git_hub.models.DispatchBody
import com.example.myaiapp.chat.data.git_hub.models.OpsEnvelope
import com.example.myaiapp.chat.data.git_hub.models.boolString
import com.example.myaiapp.chat.data.git_hub.models.str
import com.example.myaiapp.network.GitHubActionsApi
import kotlinx.coroutines.delay
import javax.inject.Inject

class ReleaseOpsRouter @Inject constructor(
    private val gh: GitHubActionsApi,
) {
    suspend fun handle(env: OpsEnvelope): String {
        // Отслеживаем только что созданные репы в рамках одного вызова
        val justCreated = mutableSetOf<Pair<String, String>>() // (owner, repo)

        for (a in env.actions) {
            when (a.type) {
                "create_repo_from_template" -> {
                    val owner = a.inputs.str("target_owner")
                    val opsRepo  = a.inputs.str("ops_repo")
                    val file  = a.inputs.str("ops_workflow_file")
                    val ref  = a.inputs.str("ops_ref")
                    gh.dispatch(
                        owner, opsRepo, file,
                        DispatchBody(
                            ref = ref,
                            inputs = mapOf(
                                "target_owner"   to a.inputs.str("target_owner"),
                                "repo_name"      to a.inputs.str("repo_name"),
                                "description"    to a.inputs.str("description"),
                                "private"        to a.inputs.boolString("private"),
                                "template_owner" to a.inputs.str("template_owner"),
                                "template_repo"  to a.inputs.str("template_repo")
                            )
                        )
                    )
                    justCreated += a.inputs.str("target_owner") to a.inputs.str("repo_name")
                }

                "trigger_release" -> {
                    val owner = a.inputs.str("owner")
                    val repo  = a.inputs.str("repo")
                    val refIn = a.inputs.str("ref")
                    val file = a.inputs.str("workflow_file")

                    // Если этот репозиторий только что создавали — подождём готовности
                    if ((owner to repo) in justCreated) {
                        dispatchReleaseSmart(
                            gh, owner, repo, refIn,
                            inputs = mapOf(
                                "versionName"          to a.inputs.str("versionName"),
                                "tag"                  to a.inputs.str("tag"),
                                "notes"                to a.inputs.str("notes"),
                                "generateReleaseNotes" to a.inputs.boolString("generateReleaseNotes")
                            ),
                            workflowFileName = file
                        )
                    } else {
                        // существующий репо — можно пробовать сразу (с fallback)
                        runCatching {
                            gh.dispatch(owner, repo, file,
                                DispatchBody(refIn, mapOf(
                                    "versionName"          to a.inputs.str("versionName"),
                                    "tag"                  to a.inputs.str("tag"),
                                    "notes"                to a.inputs.str("notes"),
                                    "generateReleaseNotes" to a.inputs.boolString("generateReleaseNotes")
                                ))
                            )
                        }.onFailure {
                            // fallback по ID
                            dispatchReleaseSmart(
                                gh, owner, repo, refIn,
                                inputs = mapOf(
                                    "versionName"          to a.inputs.str("versionName"),
                                    "tag"                  to a.inputs.str("tag"),
                                    "notes"                to a.inputs.str("notes"),
                                    "generateReleaseNotes" to a.inputs.boolString("generateReleaseNotes")
                                ),
                                workflowFileName = file
                            )
                        }
                    }
                }
            }
        }
        return env.notes ?: "Готово"
    }

    suspend fun dispatchReleaseSmart(
        gh: GitHubActionsApi,
        owner: String,
        repo: String,
        refOrNull: String?,
        inputs: Map<String, String>,
        workflowFileName: String,
    ) {
        // 1) дождаться репозитория
        val defaultBranch = waitForRepoReady(gh, owner, repo)
        val ref = refOrNull ?: defaultBranch

        // 2) дождаться workflow и взять его ID
        val wfId = waitForWorkflowId(gh, owner, repo, workflowFileName)

        // 3) диспатчим по ID (надёжнее)
        gh.dispatch(owner, repo, wfId.toString(), DispatchBody(ref, inputs))
    }

    private suspend fun waitForRepoReady(
        gh: GitHubActionsApi,
        owner: String,
        repo: String,
        timeoutSec: Int = 120,
        pollMs: Long = 2000
    ): String {
        var elapsed = 0
        while (elapsed <= timeoutSec) {
            try {
                val meta = gh.repoMeta(owner, repo) // 200 как только репо появится
                return meta.defaultBranch // например, "main"
            } catch (e: retrofit2.HttpException) {
                if (e.code() != 404) {
                    throw e
                }
            }
            delay(pollMs)
            elapsed += (pollMs / 1000).toInt()
        }
        error("Repo $owner/$repo not visible after $timeoutSec s")
    }

    private suspend fun waitForWorkflowId(
        gh: GitHubActionsApi,
        owner: String,
        repo: String,
        fileName: String = "release-on-dispatch.yml",
        timeoutSec: Int = 120,
        pollMs: Long = 2000
    ): Long {
        var elapsed = 0
        val expectedPath = ".github/workflows/$fileName"
        while (elapsed <= timeoutSec) {
            val list = gh.listWorkflows(owner, repo)
            val hit = list.workflows.firstOrNull {
                it.path.equals(expectedPath, ignoreCase = true) ||
                        it.name.equals("Release (manual)", ignoreCase = true) ||
                        it.path.endsWith("/$fileName", ignoreCase = true)
            }
            if (hit != null && hit.state.equals("active", ignoreCase = true)) {
                return hit.id
            }
            delay(pollMs)
            elapsed += (pollMs / 1000).toInt()
        }
        error("Workflow $fileName not found/active in $owner/$repo after $timeoutSec s")
    }
}
