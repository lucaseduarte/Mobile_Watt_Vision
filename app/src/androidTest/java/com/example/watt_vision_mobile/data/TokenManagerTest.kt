package com.example.watt_vision_mobile.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenManagerTest {
    @Test fun legacyTokenIsDiscardedAndEncryptedTokenRoundTrips() {
        val testContext = InstrumentationRegistry.getInstrumentation().targetContext
        val isolated = object : ContextWrapper(testContext) {
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("${name}_test", mode)
        }
        val prefs = isolated.getSharedPreferences("watt_vision_prefs", Context.MODE_PRIVATE)
        check(prefs.edit().clear().putString("jwt_token", "token-legado-ficticio")
            .putString("outra_preferencia", "preservar").commit())
        val store = TokenManager(isolated)
        try {
            assertNull(store.read())
            assertFalse(prefs.contains("jwt_token"))
            assertEquals("preservar", prefs.getString("outra_preferencia", null))
            val token = "token-ficticio-para-teste-de-criptografia"
            store.save(token)
            val encrypted = requireNotNull(prefs.getString("jwt_token_encrypted", null))
            assertFalse(encrypted.contains(token))
            assertEquals(token, TokenManager(isolated).read())
            store.clear()
            assertNull(store.read())
            assertEquals("preservar", prefs.getString("outra_preferencia", null))
        } finally {
            check(prefs.edit().clear().commit())
        }
    }
}
