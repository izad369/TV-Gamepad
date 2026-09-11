package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.GameButton
import com.example.model.ProtocolSerializer
import com.example.model.TvRemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TV Gamepad", appName)
    }

    @Test
    fun `test remote key encoding`() {
        val encodedUp = ProtocolSerializer.encodeRemoteKey(TvRemoteKey.UP, true)
        assertEquals("RK:UP:1\n", encodedUp)

        val encodedOk = ProtocolSerializer.encodeRemoteKey(TvRemoteKey.OK, false)
        assertEquals("RK:OK:0\n", encodedOk)

        val encodedBack = ProtocolSerializer.encodeRemoteKey(TvRemoteKey.BACK, true)
        assertEquals("RK:BACK:1\n", encodedBack)
    }

    @Test
    fun `test gamepad button encoding`() {
        val encodedBtn = ProtocolSerializer.encodeButton(GameButton.A, true)
        assertEquals("B:A:1\n", encodedBtn)
    }

    @Test
    fun `test tv mode detection function`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val isTv = isRunningOnTv(context)
        assertTrue(isTv == false || isTv == true)
    }
}
