package com.example

import com.example.core.navigation.sanitizeRouteId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `xtra://profile/{userId}` and `xtra://chat/{conversationId}` are reachable from any app because
 * MainActivity is exported with a `xtra` scheme filter. These cases pin the sanitiser that keeps
 * an advertised argument from becoming part of a route or a repository key.
 */
class RouteArgSanitizationTest {

    @Test
    fun `keeps well-formed app ids untouched`() {
        assertEquals("current_user", sanitizeRouteId("current_user", "current_user"))
        assertEquals("post_poll_1", sanitizeRouteId("post_poll_1", "current_user"))
        assertEquals("acct.dev-1", sanitizeRouteId("acct.dev-1", "current_user"))
    }

    @Test
    fun `rejects ids that could change the resolved destination`() {
        assertEquals("current_user", sanitizeRouteId("../settings", "current_user"))
        assertEquals("current_user", sanitizeRouteId("user/1", "current_user"))
        assertEquals("current_user", sanitizeRouteId("user?section=account", "current_user"))
        assertEquals("current_user", sanitizeRouteId("user#frag", "current_user"))
        assertEquals("current_user", sanitizeRouteId("", "current_user"))
        assertEquals("current_user", sanitizeRouteId(null, "current_user"))
    }

    @Test
    fun `rejects overlong ids`() {
        val tooLong = "a".repeat(65)
        assertEquals("conv_kairo", sanitizeRouteId(tooLong, "conv_kairo"))
        assertEquals("a".repeat(64), sanitizeRouteId("a".repeat(64), "conv_kairo"))
    }
}
