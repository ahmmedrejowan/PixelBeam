package com.rejown.pixelbeam.presentation.home

import androidx.lifecycle.ViewModel

/**
 * ViewModel for HomeScreen
 * Currently simple as Home only handles navigation
 * Can be extended later for analytics, feature flags, etc.
 */
class HomeViewModel : ViewModel() {
    // No state needed for now - just navigation callbacks
    // Future: Could track user preferences, last used feature, etc.
}
