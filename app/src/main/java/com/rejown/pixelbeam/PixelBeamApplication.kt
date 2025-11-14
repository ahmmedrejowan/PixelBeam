package com.rejown.pixelbeam

import android.app.Application
import com.rejown.pixelbeam.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class PixelBeamApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for Dependency Injection
        startKoin {
            androidLogger(Level.ERROR) // Only show errors in production
            androidContext(this@PixelBeamApplication)
            modules(appModule)
        }
    }
}
