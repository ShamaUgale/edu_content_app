package com.stockmarket.tutorials.util

import android.annotation.SuppressLint
import android.os.Build
import android.provider.Settings
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Security utility class for detecting compromised environments.
 * Checks for root access, emulator usage, and generates device fingerprints.
 */
object SecurityUtils {

    // ========== ROOT DETECTION ==========

    /**
     * Comprehensive root detection using multiple heuristics.
     * Returns true if any root indicator is found.
     */
    fun isDeviceRooted(): Boolean {
        return checkRootBinaries() ||
               checkSuBinary() ||
               checkRootManagementApps() ||
               checkDangerousProps() ||
               checkRWSystem()
    }

    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/system/app/SuperSU.apk",
            "/system/app/SuperSU",
            "/system/app/Superuser",
            "/system/app/superuser.apk",
            "/system/etc/init.d/99telegramhack"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkSuBinary(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            reader.close()
            result != null
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRootManagementApps(): Boolean {
        val knownRootApps = arrayOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "me.phh.superuser",
            "com.kingouser.com"
        )
        // Check if these packages exist (simplified check via file system)
        return knownRootApps.any { pkg ->
            File("/data/data/$pkg").exists()
        }
    }

    private fun checkDangerousProps(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("getprop")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val props = reader.readText()
            reader.close()
            props.contains("ro.debuggable=1") ||
            props.contains("ro.secure=0")
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRWSystem(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("mount")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val mounts = reader.readText()
            reader.close()
            mounts.split("\n").any { line ->
                line.contains("/system") && line.contains("rw,")
            }
        } catch (e: Exception) {
            false
        }
    }

    // ========== EMULATOR DETECTION ==========

    /**
     * Detects if the app is running on an emulator.
     * Uses multiple heuristics including Build properties, hardware, and sensors.
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.BOARD == "QC_Reference_Phone"
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.HOST.startsWith("Build")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    // ========== DEVICE FINGERPRINT ==========

    /**
     * Generates a unique device fingerprint for device binding.
     * Combines multiple device identifiers.
     */
    @SuppressLint("HardwareIds")
    fun getDeviceFingerprint(context: android.content.Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        val deviceInfo = buildString {
            append(Build.MANUFACTURER)
            append(Build.MODEL)
            append(Build.BOARD)
            append(Build.HARDWARE)
            append(androidId)
        }

        // Generate a hash of the combined device info
        return deviceInfo.hashCode().toString(16).uppercase()
    }

    // ========== SCREEN RECORDING DETECTION ==========

    /**
     * Known screen recording app packages.
     * Used for optional advanced screen recording detection.
     */
    val knownScreenRecordingApps = listOf(
        "com.kimcy929.screenrecorder",
        "com.hecorat.screenrecorder.free",
        "com.az.screen.recorder",
        "com.asus.screenrecorder",
        "com.duapps.recorder",
        "com.mobizen.recorder",
        "com.default.recorder",
        "com.screen.recorder"
    )

    /**
     * Check if known screen recording apps are installed.
     */
    fun hasScreenRecordingApps(context: android.content.Context): Boolean {
        val pm = context.packageManager
        return knownScreenRecordingApps.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
