package com.example.myaiapp.network

import com.example.myaiapp.chat.data.git_hub.models.DispatchBody
import com.example.myaiapp.chat.data.git_hub.models.RepoMeta
import com.example.myaiapp.chat.data.git_hub.models.WorkflowsResp
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GitHubActionsApi {
    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun dispatch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowFileNameOrId: String,
        @Body body: DispatchBody
    )

    @GET("repos/{owner}/{repo}")
    suspend fun repoMeta(@Path("owner") owner: String, @Path("repo") repo: String): RepoMeta

    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun listWorkflows(@Path("owner") owner: String, @Path("repo") repo: String): WorkflowsResp
}
