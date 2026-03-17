package com.chatppp.app.domain.model

import java.io.IOException

sealed class ChatError(message: String, cause: Throwable? = null) : IOException(message, cause) {
    class Config(message: String) :
        ChatError(message)

    class Auth :
        ChatError("Authentication failed for chat request")

    class HttpStatus(val code: Int) :
        ChatError("Chat request failed with HTTP $code")

    class StreamProtocol(cause: Throwable? = null) :
        ChatError("Failed to parse chat stream payload", cause)
}
