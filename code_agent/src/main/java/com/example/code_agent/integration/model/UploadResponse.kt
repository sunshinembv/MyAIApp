package com.example.code_agent.integration.model

data class UploadResponse(
    val file_path: String?,
    val message: String?,
    val success: Boolean,
    val test_result: TestResult?
) {
    data class TestResult(
        val api_response: ApiResponse?,
        val message: String?,
        val status: String?
    )
    data class ApiResponse(
        val result: Result?,
        val success: Boolean
    )
    data class Result(
        val content: List<ContentItem>?
    )
    data class ContentItem(
        val text: String?,
        val type: String?
    )
}
