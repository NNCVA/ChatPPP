package com.chatppp.app.data.remote.provider

import com.chatppp.app.data.remote.model.ChatMessageDto
import com.chatppp.app.data.remote.model.ChatRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

sealed interface ConnectionTestResult {
    data object Success : ConnectionTestResult
    data class Failure(val message: String) : ConnectionTestResult
}

open class ConnectionTestService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    open suspend fun testConnection(
        baseUrl: String,
        model: String,
        apiKey: String?,
        relayToken: String?,
        isRelay: Boolean
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        try {
            val endpoint = normalizeChatCompletionsEndpoint(baseUrl)
            val body = ChatRequestDto(
                model = model.ifBlank { "deepseek-chat" },
                messages = listOf(ChatMessageDto(role = "user", content = "test")),
                stream = false
            )
            val requestBody = json.encodeToString(ChatRequestDto.serializer(), body)
                .toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .apply {
                    if (isRelay) {
                        addHeader("X-Relay-Token", relayToken ?: "")
                    } else {
                        addHeader("Authorization", "Bearer ${apiKey ?: ""}")
                    }
                }
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    ConnectionTestResult.Success
                } else {
                    ConnectionTestResult.Failure("Error ${it.code}: ${it.message}")
                }
            }
        } catch (e: Exception) {
            ConnectionTestResult.Failure(e.message ?: "Unknown error")
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
