package com.example.control

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.net.Inet4Address
import java.net.NetworkInterface

data class DeviceStatus(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val ramUsedMb: Long,
    val ramTotalMb: Long,
    val storageUsedGb: Double,
    val storageTotalGb: Double,
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val networkType: String,
    val ipAddress: String
)

class DeviceManager(private val context: Context) {

    fun getDeviceStatus(): DeviceStatus {
        val batteryInfo = getBatteryInfo()
        val ramInfo = getRamInfo()
        val storageInfo = getStorageInfo()
        val networkInfo = getNetworkInfo()

        return DeviceStatus(
            batteryLevel = batteryInfo.first,
            isCharging = batteryInfo.second,
            ramUsedMb = ramInfo.first,
            ramTotalMb = ramInfo.second,
            storageUsedGb = storageInfo.first,
            storageTotalGb = storageInfo.second,
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            networkType = networkInfo.first,
            ipAddress = networkInfo.second
        )
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 100
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return Pair(batteryPct, isCharging)
    }

    private fun getRamInfo(): Pair<Long, Long> {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        if (actManager != null) {
            actManager.getMemoryInfo(memInfo)
            val totalMb = memInfo.totalMem / (1024 * 1024)
            val availMb = memInfo.availMem / (1024 * 1024)
            val usedMb = (totalMb - availMb).coerceAtLeast(0)
            return Pair(usedMb, totalMb)
        }
        return Pair(0, 0)
    }

    private fun getStorageInfo(): Pair<Double, Double> {
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGb = String.format("%.1f", totalBytes / (1024.0 * 1024.0 * 1024.0)).toDoubleOrNull() ?: 0.0
            val usedGb = String.format("%.1f", usedBytes / (1024.0 * 1024.0 * 1024.0)).toDoubleOrNull() ?: 0.0
            return Pair(usedGb, totalGb)
        } catch (_: Exception) {
            return Pair(0.0, 0.0)
        }
    }

    private fun getNetworkInfo(): Pair<String, String> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        var netType = "Disconnected"

        if (connectivityManager != null) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities != null) {
                netType = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular (Mobile)"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Connected"
                }
            }
        }

        var ip = "Unavailable"
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        ip = addr.hostAddress ?: "Unavailable"
                        break
                    }
                }
                if (ip != "Unavailable") break
            }
        } catch (_: Exception) {}

        return Pair(netType, ip)
    }
}
