package com.example.ai

class CommandParser {

    fun parse(rawInput: String): DevilIntent {
        val input = rawInput.trim().lowercase()

        // 1. Root Command
        if (input.startsWith("su ") || input.startsWith("root ") || input.contains("shell command") || input.contains("root command") || input.contains("রুট কমান্ড") || input.contains("শেল কমান্ড")) {
            val cmd = extractShellCommand(rawInput)
            if (cmd.isNotBlank()) {
                return DevilIntent.ExecuteRootCommand(cmd)
            }
        }

        // 2. Screenshot
        if (input.contains("screenshot") || input.contains("স্ক্রিনশট") || input.contains("capture screen") || input.contains("screen capture")) {
            return DevilIntent.TakeScreenshot
        }

        // 3. Torch / Flashlight
        if (input.contains("flashlight") || input.contains("torch") || input.contains("টর্চ") || input.contains("ফ্ল্যাশলাইট")) {
            val enable = if (input.contains("off") || input.contains("বন্ধ") || input.contains("disable")) {
                false
            } else if (input.contains("on") || input.contains("জ্বালাও") || input.contains("চালু") || input.contains("enable")) {
                true
            } else {
                null
            }
            return DevilIntent.ToggleTorch(enable)
        }

        // 4. Device Information / Status
        if (input.contains("device info") || input.contains("device information") || input.contains("ডিভাইস তথ্য") ||
            input.contains("battery") || input.contains("ব্যাটারি") || input.contains("phone status") ||
            input.contains("storage info") || input.contains("ram status") || input.contains("phone info") ||
            input.contains("ফোনের তথ্য") || input.contains("সিস্টেম স্ট্যাটাস")) {
            return DevilIntent.DeviceInfo
        }

        // 5. Brightness
        if (input.contains("brightness") || input.contains("উজ্জ্বলতা") || input.contains("ব্রাইটনেস")) {
            val digits = Regex("\\d+").find(input)?.value?.toIntOrNull()
            if (digits != null && (input.contains("%") || input.contains("percent") || digits in 1..100)) {
                return DevilIntent.BrightnessControl(level = digits)
            }
            if (input.contains("down") || input.contains("lower") || input.contains("reduce") || input.contains("dim") || input.contains("কমাও") || input.contains("কম")) {
                return DevilIntent.BrightnessControl(delta = -40)
            }
            if (input.contains("up") || input.contains("increase") || input.contains("raise") || input.contains("brighten") || input.contains("বাড়া") || input.contains("বেশি")) {
                return DevilIntent.BrightnessControl(delta = 40)
            }
            return DevilIntent.BrightnessControl(delta = 0)
        }

        // 6. Volume
        if (input.contains("volume") || input.contains("sound") || input.contains("audio") || input.contains("ভলিউম") || input.contains("সাউন্ড")) {
            if (input.contains("mute") || input.contains("silent") || input.contains("মিউট") || input.contains("নিঃশব্দ")) {
                return DevilIntent.VolumeControl(VolumeAction.MUTE)
            }
            val digits = Regex("\\d+").find(input)?.value?.toIntOrNull()
            if (digits != null && (input.contains("%") || input.contains("percent") || digits in 1..100)) {
                return DevilIntent.VolumeControl(VolumeAction.SET_LEVEL, level = digits)
            }
            if (input.contains("down") || input.contains("lower") || input.contains("reduce") || input.contains("decrease") || input.contains("কমাও") || input.contains("কম")) {
                return DevilIntent.VolumeControl(VolumeAction.DOWN)
            }
            if (input.contains("up") || input.contains("increase") || input.contains("raise") || input.contains("বাড়া") || input.contains("বেশি")) {
                return DevilIntent.VolumeControl(VolumeAction.UP)
            }
            return DevilIntent.VolumeControl(VolumeAction.UP)
        }

        // 7. Settings Pages
        if (input.contains("settings") || input.contains("সেটিংস")) {
            if (input.contains("wifi") || input.contains("wi-fi") || input.contains("ওয়াইফাই")) {
                return DevilIntent.OpenSettings("wifi")
            }
            if (input.contains("bluetooth") || input.contains("ব্লুটুথ")) {
                return DevilIntent.OpenSettings("bluetooth")
            }
            if (input.contains("display") || input.contains("ডিসপ্লে")) {
                return DevilIntent.OpenSettings("display")
            }
            if (input.contains("sound") || input.contains("সাউন্ড")) {
                return DevilIntent.OpenSettings("sound")
            }
            if (input.contains("app") || input.contains("অ্যাপ")) {
                return DevilIntent.OpenSettings("apps")
            }
            return DevilIntent.OpenSettings("general")
        }

        // 8. Create Folder
        if (input.contains("create a folder") || input.contains("create folder") || input.contains("make folder") ||
            input.contains("new folder") || input.contains("ফোল্ডার তৈরি") || input.contains("ফোল্ডার বানাও")) {
            val folderName = extractFolderName(rawInput)
            return DevilIntent.CreateFolder(folderName)
        }

        // 9. Search Files
        if (input.contains("search") && (input.contains("file") || input.contains("photo") || input.contains("doc") || input.contains("image") || input.contains("ফাইল"))) {
            val query = extractSearchQuery(rawInput)
            return DevilIntent.SearchFiles(query)
        }
        if (input.contains("ফাইল খুঁজুন") || input.contains("ফাইল সার্চ")) {
            val query = extractSearchQuery(rawInput)
            return DevilIntent.SearchFiles(query)
        }

        // 10. Close / Stop App
        if (input.startsWith("close ") || input.startsWith("stop ") || input.startsWith("kill ") || input.contains("বন্ধ করো")) {
            val targetApp = extractAppName(input, isClosing = true)
            if (targetApp.isNotBlank()) {
                return DevilIntent.CloseApp(targetApp)
            }
        }

        // 11. Open / Launch App
        if (input.startsWith("open ") || input.startsWith("launch ") || input.startsWith("start ") || input.contains("খোলো") || input.contains("চালু করো")) {
            val targetApp = extractAppName(input, isClosing = false)
            if (targetApp.isNotBlank()) {
                return DevilIntent.OpenApp(targetApp)
            }
        }

        // 12. Global Navigation
        if (input == "go home" || input == "home" || input == "হোম") {
            return DevilIntent.GlobalAction("home")
        }
        if (input == "go back" || input == "back" || input == "ফিরে যাও") {
            return DevilIntent.GlobalAction("back")
        }
        if (input.contains("notifications") || input.contains("নোটিফিকেশন")) {
            return DevilIntent.GlobalAction("notifications")
        }
        if (input.contains("quick settings") || input.contains("কুইক সেটিংস")) {
            return DevilIntent.GlobalAction("quick_settings")
        }

        // 13. Conversational / Q&A
        return DevilIntent.Conversational(rawInput)
    }

    private fun extractShellCommand(input: String): String {
        val markers = listOf(
            "execute this authorized shell command:",
            "execute this authorized shell command",
            "execute shell command:",
            "execute shell command",
            "run root command:",
            "run root command",
            "root command:",
            "execute command:",
            "রুট কমান্ড:",
            "শেল কমান্ড:"
        )
        for (m in markers) {
            val idx = input.indexOf(m, ignoreCase = true)
            if (idx != -1) {
                return input.substring(idx + m.length).trim().removeSurrounding("\"").removeSurrounding("'")
            }
        }
        if (input.lowercase().startsWith("su ")) return input.substring(3).trim()
        if (input.lowercase().startsWith("root ")) return input.substring(5).trim()
        return input.trim()
    }

    private fun extractFolderName(rawInput: String): String {
        val namedMatch = Regex("(?:named|called|নামের|নামে)\\s+([a-zA-Z0-9_-]+)", RegexOption.IGNORE_CASE).find(rawInput)
        if (namedMatch != null) {
            return namedMatch.groupValues[1].trimEnd('.', ',', '!', '?')
        }
        val folderMatch = Regex("(?:folder|ফোল্ডার)\\s+([a-zA-Z0-9_-]+)", RegexOption.IGNORE_CASE).find(rawInput)
        if (folderMatch != null) {
            return folderMatch.groupValues[1].trimEnd('.', ',', '!', '?')
        }
        return "Devil_Folder"
    }

    private fun extractSearchQuery(rawInput: String): String {
        val forMatch = Regex("(?:for|নামের)\\s+([a-zA-Z0-9_.-]+)", RegexOption.IGNORE_CASE).find(rawInput)
        if (forMatch != null) {
            return forMatch.groupValues[1].trimEnd('.', ',', '!', '?')
        }
        val clean = rawInput.replace("(?i)search".toRegex(), "")
            .replace("(?i)my files".toRegex(), "")
            .replace("(?i)files".toRegex(), "")
            .replace("ফাইল", "")
            .replace("খুঁজুন", "")
            .replace("সার্চ", "")
            .trim()
            .trimEnd('.', ',', '!', '?')
        return if (clean.isNotBlank()) clean else "photo"
    }

    private fun extractAppName(input: String, isClosing: Boolean): String {
        var clean = input
        val prefixes = if (isClosing) listOf("close", "stop", "kill") else listOf("open", "launch", "start")
        for (p in prefixes) {
            if (clean.startsWith(p)) {
                clean = clean.removePrefix(p).trim()
                break
            }
        }
        clean = clean.replace("app", "").replace("application", "")
            .replace("খোলো", "").replace("চালু করো", "")
            .replace("বন্ধ করো", "").trim()
        return clean
    }
}
