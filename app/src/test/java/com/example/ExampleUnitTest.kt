package com.example

import com.example.ai.CommandParser
import com.example.ai.DevilIntent
import com.example.ai.VolumeAction
import com.example.root.RootController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val parser = CommandParser()
    private val rootController = RootController()

    @Test
    fun testOpenAppCommands() {
        val intent1 = parser.parse("Open YouTube.")
        assertTrue(intent1 is DevilIntent.OpenApp)

        val intent2 = parser.parse("launch Chrome")
        assertTrue(intent2 is DevilIntent.OpenApp)
    }

    @Test
    fun testVolumeCommands() {
        val intentUp = parser.parse("Turn the volume up")
        assertTrue(intentUp is DevilIntent.VolumeControl)
        assertEquals(VolumeAction.UP, (intentUp as DevilIntent.VolumeControl).action)

        val intentMute = parser.parse("mute volume")
        assertTrue(intentMute is DevilIntent.VolumeControl)
        assertEquals(VolumeAction.MUTE, (intentMute as DevilIntent.VolumeControl).action)
    }

    @Test
    fun testDeviceInfoCommands() {
        val intent = parser.parse("Show my device information.")
        assertTrue(intent is DevilIntent.DeviceInfo)

        val intentBn = parser.parse("ডিভাইস তথ্য দেখাও")
        assertTrue(intentBn is DevilIntent.DeviceInfo)
    }

    @Test
    fun testCreateFolderCommand() {
        val intent = parser.parse("Create a folder named Test.")
        assertTrue(intent is DevilIntent.CreateFolder)
        assertEquals("Test", (intent as DevilIntent.CreateFolder).folderName)
    }

    @Test
    fun testScreenshotCommand() {
        val intent = parser.parse("Take a screenshot.")
        assertTrue(intent is DevilIntent.TakeScreenshot)

        val intentBn = parser.parse("স্ক্রিনশট নাও")
        assertTrue(intentBn is DevilIntent.TakeScreenshot)
    }

    @Test
    fun testSearchFilesCommand() {
        val intent = parser.parse("Search my files for photo.")
        assertTrue(intent is DevilIntent.SearchFiles)
        assertEquals("photo", (intent as DevilIntent.SearchFiles).query)
    }

    @Test
    fun testRootDangerousCommands() {
        assertTrue(rootController.isDangerousCommand("rm -rf /"))
        assertTrue(rootController.isDangerousCommand("reboot"))
        assertTrue(rootController.isDangerousCommand("format"))
        assertFalse(rootController.isDangerousCommand("id"))
        assertFalse(rootController.isDangerousCommand("ls -la /sdcard"))
    }
}
