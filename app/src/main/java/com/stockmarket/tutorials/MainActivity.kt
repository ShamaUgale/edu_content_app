package com.stockmarket.tutorials

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.stockmarket.tutorials.ui.navigation.AppNavigation
import com.stockmarket.tutorials.ui.theme.StockMarketTheme
import com.stockmarket.tutorials.util.SecurityUtils
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point Activity.
 * Applies FLAG_SECURE to prevent screenshots and screen recording.
 * Performs root/emulator detection before rendering content.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ====== ANTI-PIRACY: Disable screenshots and screen recording ======
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        // ====== SECURITY: Root & Emulator detection ======
        val isRooted = SecurityUtils.isDeviceRooted()
        val isEmulator = SecurityUtils.isEmulator()

        setContent {
            StockMarketTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isRooted || isEmulator) {
                        com.stockmarket.tutorials.ui.screens.SecurityBlockScreen(
                            isRooted = isRooted,
                            isEmulator = isEmulator,
                            onExitApp = { finishAffinity() }
                        )
                    } else {
                        AppNavigation()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-apply FLAG_SECURE in case it was removed
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}
