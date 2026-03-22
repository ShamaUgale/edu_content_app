package com.stockmarket.tutorials

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for StockMarket Tutorials.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class StockMarketApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Application-level initialization
    }
}
