package com.example.watt_vision_mobile

import com.example.watt_vision_mobile.model.AuthData
import com.example.watt_vision_mobile.model.LoginResponse
import org.junit.Assert.*
import org.junit.Test

class AppAccessPolicyTest {
    @Test
    fun releaseNeverGrantsAccessWithoutAuthentication() {
        for (loginRequested in listOf(false, true)) {
            assertFalse(AppAccessPolicy.canOpenContent(false, false, loginRequested))
        }
        assertTrue(AppAccessPolicy.canOpenContent(false, true, false))
    }

    @Test
    fun debugOpensDashboardButCanStillShowLogin() {
        assertTrue(AppAccessPolicy.canOpenContent(true, false, false))
        assertFalse(AppAccessPolicy.canOpenContent(true, false, true))
        assertTrue(AppAccessPolicy.canOpenContent(true, true, true))
    }

    @Test
    fun demoRequiresDebugAndExplicitSelection() {
        assertFalse(AppAccessPolicy.canShowDemo(false, false))
        assertFalse(AppAccessPolicy.canShowDemo(false, true))
        assertFalse(AppAccessPolicy.canShowDemo(true, false))
        assertTrue(AppAccessPolicy.canShowDemo(true, true))
    }

    @Test
    fun actualBuildVariantCannotBypassReleasePolicy() {
        assertEquals(BuildConfig.DEBUG, AppAccessPolicy.canOpenContent(BuildConfig.DEBUG, false, false))
        assertEquals(BuildConfig.DEBUG, AppAccessPolicy.canShowDemo(BuildConfig.DEBUG, true))
    }

    @Test
    fun successfulResponseWithoutTokenDoesNotAuthenticate() {
        assertFalse(LoginResponse(true, null, null).hasSessionToken)
        assertFalse(LoginResponse(true, null, AuthData(null, null, null)).hasSessionToken)
        assertFalse(LoginResponse(true, null, AuthData("", null, null)).hasSessionToken)
        assertFalse(LoginResponse(true, null, AuthData("  ", null, null)).hasSessionToken)
        assertFalse(LoginResponse(false, null, AuthData(" ", null, null)).hasSessionToken)
    }
}
