package com.rejown.pixelbeam.di

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
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Preferences
    single { ThemePreferences(androidContext()) }

    // Domain utilities
    single { ImageCompressor(androidContext()) }
    single { QRGenerator() }
    single { ImageReconstructor() }

    // ViewModels
    viewModel { HomeViewModel() }
    viewModel { ImagePickerViewModel() }
    viewModel { ImagePreviewViewModel(get()) }
    viewModel { QRDisplayViewModel(get()) }
    viewModel { QRScannerViewModel(get()) }
    viewModel { ImageResultViewModel(androidContext()) }
}
