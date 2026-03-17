package com.chatppp.app.data.remote.provider

import com.chatppp.app.data.remote.model.ChatRequestDto
import com.chatppp.app.data.remote.model.ChatResponseDto
import com.chatppp.app.data.remote.parser.ChatStreamParser
import com.chatppp.app.domain.model.ChatChunk
import com.chatppp.app.domain.model.ChatError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DirectApiProvider(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val streamParser: ChatStreamParser,
    private val endpointUrl: String,
    private val apiKey: String
) : ChatProvider {
    override suspend fun send(request: ChatRequestDto): String {
        val response = okHttpClient.newCall(buildRequest(request)).execute()
        response.use { httpResponse ->
            throwIfUnsuccessful(httpResponse.code)
            val body = httpResponse.body?.string().orEmpty()
            val payload = json.decodeFromString(ChatResponseDto.serializer(), body)
            return payload.choices.firstOrNull()?.message?.content.orEmpty()
        }
    }

    override fun stream(request: ChatRequestDto): Flow<ChatChunk> = flow {
        val response = okHttpClient.newCall(buildRequest(request)).execute()
        response.use { httpResponse ->
            throwIfUnsuccessful(httpResponse.code)
            val source = httpResponse.body?.source() ?: return@use
            while (true) {
                val line = source.readUtf8Line() ?: break
                val chunk = streamParser.parseLine(line) ?: continue
                emit(chunk)
                if (chunk == ChatChunk.Done) {
                    break
                }
            }
        }
    }

    private fun buildRequest(request: ChatRequestDto): Request {
        val requestBody = json.encodeToString(ChatRequestDto.serializer(), request)
            .toRequestBody(JSON_MEDIA_TYPE)

        return Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .post(requestBody)
            .build()
    }

    private fun throwIfUnsuccessful(code: Int) {
        if (code == 401) {
            throw ChatError.Auth()
        }
        if (code !in 200..299) {
            throw ChatError.HttpStatus(code)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
