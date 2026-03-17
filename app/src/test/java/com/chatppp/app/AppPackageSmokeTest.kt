package com.chatppp.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPackageSmokeTest {
    @Test
    fun mainActivity_uses_chatppp_package() {
        assertEquals("com.chatppp.app.MainActivity", MainActivity::class.qualifiedName)
    }
}
