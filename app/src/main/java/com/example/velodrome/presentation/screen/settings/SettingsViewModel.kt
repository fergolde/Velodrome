package com.example.velodrome.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.repository.SettingsRepository
import com.example.velodrome.util.CacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Settings screen.
 */
data class SettingsUiState(
    val imageCacheSizeMb: Int = 200,
    val musicCacheSizeGb: Int = 2,
    val accentColor: String = "#B6A0FF",
    val scrobbleEnabled: Boolean = false,
    val currentImageCacheSize: String = "0 MB",
    val currentMusicCacheSize: String = "0 GB",
    val isClearingCache: Boolean = false,
    val pendingImageCacheMb: Int = 200,
    val pendingMusicCacheGb: Int = 2,
    val hasPendingChanges: Boolean = false
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

    // Pending changes for confirmation dialog
    private val _pendingImageCacheMb = MutableStateFlow(200)
    private val _pendingMusicCacheGb = MutableStateFlow(2)
    private val _hasPendingChanges = MutableStateFlow(false)

    /**
     * UI State as a StateFlow.
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.imageCacheSizeMb,
        settingsRepository.musicCacheSizeGb,
        settingsRepository.accentColor,
        settingsRepository.scrobbleEnabled,
        _currentCacheSizes,
        _isClearingCache,
        _pendingImageCacheMb,
        _pendingMusicCacheGb,
        _hasPendingChanges
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            imageCacheSizeMb = values[0] as Int,
            musicCacheSizeGb = (values[1] as? Int) ?: 2,
            accentColor = values[2] as String,
            scrobbleEnabled = values[3] as Boolean,
            currentImageCacheSize = (values[4] as Pair<*, *>).first as String,
            currentMusicCacheSize = (values[4] as Pair<*, *>).second as String,
            isClearingCache = values[5] as Boolean,
            pendingImageCacheMb = values[6] as Int,
            pendingMusicCacheGb = values[7] as Int,
            hasPendingChanges = values[8] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        // Initialize pending values from current settings
        viewModelScope.launch {
            settingsRepository.imageCacheSizeMb.collect { limitMb ->
                _pendingImageCacheMb.value = limitMb
            }
        }
        viewModelScope.launch {
            settingsRepository.musicCacheSizeGb.collect { limitGb ->
                _pendingMusicCacheGb.value = limitGb
            }
        }
        // Apply cache cleanup when settings change
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
     * Update image cache size limit in MB (pending, needs confirmation).
     */
    fun setImageCacheSizeMb(sizeMb: Int) {
        _pendingImageCacheMb.value = sizeMb
        checkPendingChanges()
    }

    /**
     * Update music cache size limit in GB (pending, needs confirmation).
     */
    fun setMusicCacheSizeGb(sizeGb: Int) {
        _pendingMusicCacheGb.value = sizeGb
        checkPendingChanges()
    }

    /**
     * Confirm and apply pending changes.
     */
    fun confirmChanges() {
        viewModelScope.launch {
            val musicGb = _pendingMusicCacheGb.value
            settingsRepository.setMusicCacheSizeGb(musicGb)

            // Notificar al SimpleCache del nuevo límite
            // (requiere guardar referencia al SimpleCache en el ViewModel o en CacheManager)
            cacheManager.setMusicCacheLimitBytes(musicGb.toLong() * 1024 * 1024 * 1024)

            refreshCacheSizes()
            _hasPendingChanges.value = false
        }
    }

    /**
     * Discard pending changes.
     */
    fun discardChanges() {
        _pendingImageCacheMb.value = uiState.value.imageCacheSizeMb
        _pendingMusicCacheGb.value = uiState.value.musicCacheSizeGb
        _hasPendingChanges.value = false
    }

    /**
     * Check if there are pending changes.
     */
    private fun checkPendingChanges() {
        val imageChanged = _pendingImageCacheMb.value != uiState.value.imageCacheSizeMb
        val musicChanged = _pendingMusicCacheGb.value != uiState.value.musicCacheSizeGb
        _hasPendingChanges.value = imageChanged || musicChanged
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
     * Toggle scrobble enabled/disabled.
     */
    fun setScrobbleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setScrobbleEnabled(enabled)
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
        AccentColorOption("Velodrome Purple", "#B6A0FF"),
        AccentColorOption("Red", "#EF5350"),
        AccentColorOption("Pink", "#F06292"),
        AccentColorOption("Purple", "#BA68C8"),
        AccentColorOption("Deep Purple", "#9575CD"),
        AccentColorOption("Indigo", "#7986CB"),
        AccentColorOption("Blue", "#64B5F6"),
        AccentColorOption("Light Blue", "#4FC3F7"),
        AccentColorOption("Cyan", "#4DD0E1"),
        AccentColorOption("Teal", "#4DB6AC"),
        AccentColorOption("Green", "#81C784"),
        AccentColorOption("Light Green", "#AED581"),
        AccentColorOption("Lime", "#DCE775"),
        AccentColorOption("Yellow", "#FFF176"),
        AccentColorOption("Amber", "#FFD54F"),
        AccentColorOption("Orange", "#FFB74D"),
        AccentColorOption("Deep Orange", "#FF8A65"),
        AccentColorOption("Brown", "#A1887F"),
        AccentColorOption("Blue Grey", "#90A4AE"),
        AccentColorOption("White", "#FFFFFF")
    )
}

/**
 * Data class for accent color options.
 */
data class AccentColorOption(
    val name: String,
    val hexColor: String
)