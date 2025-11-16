package com.rejown.pixelbeam

import android.app.Application
import com.rejown.pixelbeam.di.appModule
import org.koin.core.context.startKoin

class PixelBeamApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for Dependency Injection
        startKoin {
            modules(appModule(this@PixelBeamApplication))
        }
    }
}
