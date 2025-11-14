package com.rejown.pixelbeam.di

import com.rejown.pixelbeam.data.local.preferences.ThemePreferences
import com.rejown.pixelbeam.presentation.home.HomeViewModel
import com.rejown.pixelbeam.presentation.sender.picker.ImagePickerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Preferences
    single { ThemePreferences(androidContext()) }

    // ViewModels
    viewModel { HomeViewModel() }
    viewModel { ImagePickerViewModel() }

    // TODO: Add more ViewModels as we create them
    // viewModel { ImagePreviewViewModel(get()) }
    // viewModel { QRDisplayViewModel(get()) }
}
