package com.fergolde.velodrome.presentation.screen.settings

import androidx.compose.runtime.Immutable

import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.fergolde.velodrome.domain.repository.AccountDataRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.util.CacheManager
import com.fergolde.velodrome.util.CredentialsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

/**
 * UI State for the Settings screen.
 */
@Immutable
data class SettingsUiState(
    val imageCacheSizeMb: Int = 200,
    val musicCacheSizeGb: Int = 2,
    val accentColor: String = "#B6A0FF",
    val scrobbleEnabled: Boolean = false,
    val eqEnabled: Boolean = false,
    val bassBoostEnabled: Boolean = false,
    val currentImageCacheSize: String = "0 MB",
    val currentMusicCacheSize: String = "0 GB",
    val isClearingCache: Boolean = false,
    val pendingImageCacheMb: Int = 200,
    val pendingMusicCacheGb: Int = 2,
    val hasPendingChanges: Boolean = false,
    val appVersion: String = "",
    val shouldLogout: Boolean = false
)

/**
 * ViewModel for the Settings screen.
 * Manages user preferences and cache configuration.
 */
@UnstableApi
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cacheManager: CacheManager,
    private val credentialsManager: CredentialsManager,
    private val accountDataRepository: AccountDataRepository,
    @Named("appVersion") private val appVersion: String
) : ViewModel() {

    private val _currentCacheSizes = MutableStateFlow(Pair("0 MB", "0 GB"))

    private val _isClearingCache = MutableStateFlow(false)

    // Pending changes for confirmation dialog
    private val _pendingImageCacheMb = MutableStateFlow(200)
    private val _pendingMusicCacheGb = MutableStateFlow(2)
    private val _hasPendingChanges = MutableStateFlow(false)

    private val _shouldLogout = MutableStateFlow(false)

    private data class PersistedSettings(
        val imageCacheSizeMb: Int,
        val musicCacheSizeGb: Int,
        val accentColor: String,
        val scrobbleEnabled: Boolean,
        val eqEnabled: Boolean,
        val bassBoostEnabled: Boolean
    )

    private data class LocalSettingsState(
        val currentImageCacheSize: String,
        val currentMusicCacheSize: String,
        val isClearingCache: Boolean,
        val pendingImageCacheMb: Int,
        val pendingMusicCacheGb: Int,
        val hasPendingChanges: Boolean
    )

    private val persistedCore: Flow<PersistedSettings> = combine(
        settingsRepository.imageCacheSizeMb,
        settingsRepository.musicCacheSizeGb,
        settingsRepository.accentColor,
        settingsRepository.scrobbleEnabled,
        settingsRepository.eqEnabled
    ) { image, music, accent, scrobble, eqEnabled ->
        PersistedSettings(image, music, accent, scrobble, eqEnabled, bassBoostEnabled = false)
    }

    private val persistedSettings: Flow<PersistedSettings> = combine(
        persistedCore,
        settingsRepository.bassBoostEnabled
    ) { settings, bassBoostEnabled ->
        settings.copy(bassBoostEnabled = bassBoostEnabled)
    }

    private val localState: Flow<LocalSettingsState> = combine(
        _currentCacheSizes,
        _isClearingCache,
        _pendingImageCacheMb,
        _pendingMusicCacheGb,
        _hasPendingChanges
    ) { sizes, clearing, pendingImage, pendingMusic, hasPending ->
        LocalSettingsState(
            currentImageCacheSize = sizes.first,
            currentMusicCacheSize = sizes.second,
            isClearingCache = clearing,
            pendingImageCacheMb = pendingImage,
            pendingMusicCacheGb = pendingMusic,
            hasPendingChanges = hasPending
        )
    }

    /**
     * UI State as a StateFlow. Each source keeps its own typed shape, so no
     * positional casts are needed when combining them.
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        persistedSettings,
        localState,
        _shouldLogout
    ) { settings, local, shouldLogout ->
        SettingsUiState(
            imageCacheSizeMb = settings.imageCacheSizeMb,
            musicCacheSizeGb = settings.musicCacheSizeGb,
            accentColor = settings.accentColor,
            scrobbleEnabled = settings.scrobbleEnabled,
            eqEnabled = settings.eqEnabled,
            bassBoostEnabled = settings.bassBoostEnabled,
            currentImageCacheSize = local.currentImageCacheSize,
            currentMusicCacheSize = local.currentMusicCacheSize,
            isClearingCache = local.isClearingCache,
            pendingImageCacheMb = local.pendingImageCacheMb,
            pendingMusicCacheGb = local.pendingMusicCacheGb,
            hasPendingChanges = local.hasPendingChanges,
            appVersion = appVersion,
            shouldLogout = shouldLogout
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        // Medir tamaños de cache sin bloquear el main thread
        refreshCacheSizes()

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
            val imageMb = _pendingImageCacheMb.value
            val musicGb = _pendingMusicCacheGb.value
            settingsRepository.setImageCacheSizeMb(imageMb)
            settingsRepository.setMusicCacheSizeGb(musicGb)
            cacheManager.setMusicCacheLimitGb(musicGb)

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

    fun setEqEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEqEnabled(enabled) }
    }

    fun setBassBoostEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBassBoostEnabled(enabled) }
    }

    /**
     * Clear all caches immediately.
     */
    @OptIn(UnstableApi::class)
    fun clearAllCaches() {
        viewModelScope.launch {
            _isClearingCache.value = true
            cacheManager.clearAllCaches()
            refreshCacheSizes()
            _isClearingCache.value = false
        }
    }

    /**
     * Clears credentials, discards the previous account's local data and
     * signals the UI to return to the login screen.
     *
     * The purge matters: without it the next account inherits the previous
     * user's library and queue, and pending scrobbles get replayed under the
     * new account's token. Audio/image caches survive because their keys are
     * account-scoped, so the signed-in user keeps downloaded music.
     */
    fun logout() {
        viewModelScope.launch {
            // The purge is best-effort cleanup. It touches the Room tables, the
            // queue DataStore and four settings keys, any of which can throw
            // (disk full, corrupted DataStore file). Letting that failure
            // escape would strand the user on this screen with live
            // credentials and no way to sign out, so the logout itself always
            // completes.
            try {
                accountDataRepository.purgeAccountData()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Account purge failed, signing out anyway", e)
            }
            credentialsManager.clearCredentials()
            _shouldLogout.value = true
        }
    }

    /**
     * Marks the logout navigation as handled.
     */
    fun logoutHandled() {
        _shouldLogout.value = false
    }

    /**
     * Refresh current cache sizes display.
     * Runs on IO so the settings screen never blocks on directory scans.
     */
    @OptIn(UnstableApi::class)
    fun refreshCacheSizes() {
        viewModelScope.launch {
            val sizes = withContext(Dispatchers.IO) {
                Pair(
                    cacheManager.getImageCacheSizeFormatted(),
                    cacheManager.getMusicCacheSizeFormatted()
                )
            }
            _currentCacheSizes.value = sizes
        }
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

    private companion object {
        private const val TAG = "SettingsViewModel"
    }
}

/**
 * Data class for accent color options.
 */
data class AccentColorOption(
    val name: String,
    val hexColor: String
)
