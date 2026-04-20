package com.example.velodrome.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.util.CacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Settings screen.
 */
data class SettingsUiState(
    val imageCacheSizeMb: Int = 100,
    val musicCacheSizeGb: Int = 1,
    val accentColor: String = "#B6A0FF",
    val currentImageCacheSize: String = "0 MB",
    val currentMusicCacheSize: String = "0 GB",
    val isClearingCache: Boolean = false
)

/**
 * ViewModel for the Settings screen.
 * Manages user preferences and cache configuration.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cacheManager: CacheManager
) : ViewModel() {

    private val _currentCacheSizes = MutableStateFlow(
        Pair(cacheManager.getImageCacheSizeFormatted(), cacheManager.getMusicCacheSizeFormatted())
    )

    private val _isClearingCache = MutableStateFlow(false)

    /**
     * UI State as a StateFlow.
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.imageCacheSizeMb,
        settingsRepository.musicCacheSizeGb,
        settingsRepository.accentColor,
        _currentCacheSizes,
        _isClearingCache
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            imageCacheSizeMb = values[0] as Int,
            musicCacheSizeGb = values[1] as Int,
            accentColor = values[2] as String,
            currentImageCacheSize = (values[3] as Pair<*, *>).first as String,
            currentMusicCacheSize = (values[3] as Pair<*, *>).second as String,
            isClearingCache = values[4] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        // Clean caches on startup based on current settings
        viewModelScope.launch {
            settingsRepository.imageCacheSizeMb.collect { limitMb ->
                cacheManager.cleanImageCacheIfNeeded(limitMb)
                refreshCacheSizes()
            }
        }
        viewModelScope.launch {
            settingsRepository.musicCacheSizeGb.collect { limitGb ->
                cacheManager.cleanMusicCacheIfNeeded(limitGb)
                refreshCacheSizes()
            }
        }
    }

    // --- Actions ---

    /**
     * Update image cache size limit in MB.
     */
    fun setImageCacheSizeMb(sizeMb: Int) {
        viewModelScope.launch {
            settingsRepository.setImageCacheSizeMb(sizeMb)
            // Clean immediately after setting new limit
            cacheManager.cleanImageCacheIfNeeded(sizeMb)
            refreshCacheSizes()
        }
    }

    /**
     * Update music cache size limit in GB.
     */
    fun setMusicCacheSizeGb(sizeGb: Int) {
        viewModelScope.launch {
            settingsRepository.setMusicCacheSizeGb(sizeGb)
            // Clean immediately after setting new limit
            cacheManager.cleanMusicCacheIfNeeded(sizeGb)
            refreshCacheSizes()
        }
    }

    /**
     * Update accent color.
     */
    fun setAccentColor(hexColor: String) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(hexColor)
        }
    }

    /**
     * Clear all caches immediately.
     */
    fun clearAllCaches() {
        viewModelScope.launch {
            _isClearingCache.value = true
            cacheManager.clearAllCaches()
            refreshCacheSizes()
            _isClearingCache.value = false
        }
    }

    /**
     * Refresh current cache sizes display.
     */
    private fun refreshCacheSizes() {
        _currentCacheSizes.value = Pair(
            cacheManager.getImageCacheSizeFormatted(),
            cacheManager.getMusicCacheSizeFormatted()
        )
    }

    // --- Available Accent Colors ---

    /**
     * List of available accent colors for the user to choose.
     */
    val availableAccentColors = listOf(
        AccentColorOption("Purple", "#B6A0FF"),
        AccentColorOption("Blue", "#64B5F6"),
        AccentColorOption("Cyan", "#4DD0E1"),
        AccentColorOption("Teal", "#4DB6AC"),
        AccentColorOption("Green", "#81C784"),
        AccentColorOption("Yellow", "#FFD54F"),
        AccentColorOption("Orange", "#FFB74D"),
        AccentColorOption("Red", "#E57373"),
        AccentColorOption("Pink", "#F06292"),
        AccentColorOption("Deep Purple", "#9575CD")
    )
}

/**
 * Data class for accent color options.
 */
data class AccentColorOption(
    val name: String,
    val hexColor: String
)