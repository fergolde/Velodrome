package com.example.velodrome.presentation.screen.artists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ArtistsViewModel"

data class ArtistsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Artist> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistsUiState())
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    val pagedArtists: Flow<PagingData<Artist>> = Pager(
        config = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false),
        pagingSourceFactory = { artistRepository.getArtistsPaged() }
    ).flow.cachedIn(viewModelScope)

    init {
        observeSearch()
        _uiState.update { it.copy(isLoading = false) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSearch() {
        viewModelScope.launch {
            Log.d(TAG, "observeSearch: Iniciando observación")
            searchQuery.flatMapLatest { query ->
                Log.d(TAG, "observeSearch: query=$query")
                if (query.isBlank()) {
                    flowOf(emptyList())
                } else {
                    flowOf(artistRepository.searchLocal(query))
                }
            }.collect { results ->
                Log.d(TAG, "observeSearch: resultados=${results.size}")
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = searchQuery.value.isNotBlank()
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }
}