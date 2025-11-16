package com.rejown.pixelbeam.di

import android.app.Application
import com.rejown.pixelbeam.data.local.preferences.ThemePreferences
import com.rejown.pixelbeam.domain.util.ImageCompressor
import com.rejown.pixelbeam.domain.util.ImageReconstructor
import com.rejown.pixelbeam.domain.util.QRGenerator
import com.rejown.pixelbeam.presentation.home.HomeViewModel
import com.rejown.pixelbeam.presentation.sender.picker.ImagePickerViewModel
import com.rejown.pixelbeam.presentation.sender.preview.ImagePreviewViewModel
import com.rejown.pixelbeam.presentation.sender.display.QRDisplayViewModel
import com.rejown.pixelbeam.presentation.receiver.scanner.QRScannerViewModel
import com.rejown.pixelbeam.presentation.receiver.result.ImageResultViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


fun appModule(application: Application) = module {
    // Preferences
    single { ThemePreferences(application) }

    // Domain utilities
    single { ImageCompressor(application) }
    single { QRGenerator() }
    single { ImageReconstructor() }

    // ViewModels
    viewModel { HomeViewModel() }
    viewModel { ImagePickerViewModel() }
    viewModel { ImagePreviewViewModel(get()) }
    viewModel { QRDisplayViewModel(get()) }
    viewModel { QRScannerViewModel(get(), application) }
    viewModel { ImageResultViewModel(application) }
}
