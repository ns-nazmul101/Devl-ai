package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.CommandParser
import com.example.ai.DevilIntent
import com.example.root.RootController
import org.junit.Assert.assertEquals
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
        assertEquals("Devil AI", appName)
    }

    @Test
    fun `test command parser intents`() {
        val parser = CommandParser()

        val openIntent = parser.parse("Open YouTube")
        assertTrue(openIntent is DevilIntent.OpenApp)
        assertEquals("youtube", (openIntent as DevilIntent.OpenApp).appName)

        val bengaliIntent = parser.parse("ইউটিউব খোলো")
        assertTrue(bengaliIntent is DevilIntent.OpenApp)

        val brightIntent = parser.parse("Turn the brightness down")
        assertTrue(brightIntent is DevilIntent.BrightnessControl)

        val folderIntent = parser.parse("Create a folder named Test")
        assertTrue(folderIntent is DevilIntent.CreateFolder)
        assertEquals("Test", (folderIntent as DevilIntent.CreateFolder).folderName)

        val rootDangerIntent = parser.parse("execute this authorized shell command: rm -rf /")
        assertTrue(rootDangerIntent is DevilIntent.ExecuteRootCommand)
        val rootCtrl = RootController()
        assertTrue(rootCtrl.isDangerousCommand((rootDangerIntent as DevilIntent.ExecuteRootCommand).command))
    }
}
