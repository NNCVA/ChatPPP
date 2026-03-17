package com.chatppp.app.data.context

data class CompressionBudget(
    val maxContextTokens: Int = DEFAULT_MAX_CONTEXT_TOKENS,
    val compressionTriggerTokens: Int = DEFAULT_COMPRESSION_TRIGGER_TOKENS,
    val targetCompressedTokens: Int = DEFAULT_TARGET_COMPRESSED_TOKENS,
    val reservedResponseTokens: Int = DEFAULT_RESERVED_RESPONSE_TOKENS
) {
    init {
        require(maxContextTokens > 0)
        require(compressionTriggerTokens > 0)
        require(targetCompressedTokens > 0)
        require(reservedResponseTokens >= 0)
        require(targetCompressedTokens <= compressionTriggerTokens)
        require(compressionTriggerTokens <= maxContextTokens)
    }

    companion object {
        const val DEFAULT_MAX_CONTEXT_TOKENS = 32_768
        const val DEFAULT_COMPRESSION_TRIGGER_TOKENS = 24_576
        const val DEFAULT_TARGET_COMPRESSED_TOKENS = 14_336
        const val DEFAULT_RESERVED_RESPONSE_TOKENS = 6_144
    }
}
