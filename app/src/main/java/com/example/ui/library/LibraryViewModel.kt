package com.example.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.decoder.DecoderCapabilities
import com.example.decoder.DeviceDecoderProfile
import com.example.media.ResolutionCategory
import com.example.media.VideoFolder
import com.example.media.VideoItem
import com.example.storage.LocalVideoRepository
import com.example.storage.VideoSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab(val title: String) {
    ALL("All Videos"),
    FOLDERS("Folders"),
    RECENT("Recent"),
    FAVORITES("Favorites"),
    HIGH_RES("16K & 8K Ultra")
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isGridView: Boolean = true,
    val sortOrder: VideoSortOrder = VideoSortOrder.DATE_DESC,
    val selectedResolutionFilter: ResolutionCategory? = null,
    val selectedFolder: VideoFolder? = null,
    val isScanning: Boolean = false,
    val selectedVideoForDetails: VideoItem? = null,
    val isSortSheetVisible: Boolean = false,
    val deviceDecoderProfile: DeviceDecoderProfile = DecoderCapabilities.cachedProfile,
    val scanMessage: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalVideoRepository(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val allVideos = repository.allVideosFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val folders = repository.foldersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val recentlyPlayed = repository.recentlyPlayedFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val favorites = repository.favoritesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val filteredVideos: StateFlow<List<VideoItem>> = combine(
        allVideos,
        recentlyPlayed,
        favorites,
        _uiState
    ) { all, recent, favs, state ->
        val baseList = when {
            state.selectedFolder != null -> all.filter {
                it.bucketId == state.selectedFolder.bucketId || it.folderName == state.selectedFolder.name
            }
            state.selectedTab == LibraryTab.RECENT -> recent
            state.selectedTab == LibraryTab.FAVORITES -> favs
            state.selectedTab == LibraryTab.HIGH_RES -> all.filter {
                it.resolutionCategory == ResolutionCategory.ULTRA_16K ||
                        it.resolutionCategory == ResolutionCategory.UHD_8K ||
                        it.resolutionCategory == ResolutionCategory.UHD_4K
            }
            else -> all
        }

        var result = baseList

        // Apply Resolution Category Filter
        if (state.selectedResolutionFilter != null) {
            result = result.filter { it.resolutionCategory == state.selectedResolutionFilter }
        }

        // Apply Search Query
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            result = result.filter {
                it.displayName.lowercase().contains(q) ||
                        it.codec.lowercase().contains(q) ||
                        it.containerFormat.lowercase().contains(q) ||
                        it.resolutionCategory.label.lowercase().contains(q) ||
                        it.folderName.lowercase().contains(q)
            }
        }

        // Apply Sorting
        when (state.sortOrder) {
            VideoSortOrder.DATE_DESC -> result.sortedByDescending { it.dateAddedMs }
            VideoSortOrder.DATE_ASC -> result.sortedBy { it.dateAddedMs }
            VideoSortOrder.NAME_ASC -> result.sortedBy { it.displayName.lowercase() }
            VideoSortOrder.NAME_DESC -> result.sortedByDescending { it.displayName.lowercase() }
            VideoSortOrder.SIZE_DESC -> result.sortedByDescending { it.sizeBytes }
            VideoSortOrder.SIZE_ASC -> result.sortedBy { it.sizeBytes }
            VideoSortOrder.RESOLUTION_DESC -> result.sortedByDescending { it.width * it.height }
            VideoSortOrder.DURATION_DESC -> result.sortedByDescending { it.durationMs }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    init {
        refreshMedia()
    }

    fun refreshMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            try {
                repository.refreshVideos()
            } catch (e: Exception) {
                // Log or handle
            } finally {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    fun onTabSelected(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            selectedFolder = null // Reset folder drilldown on tab switch
        )
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSearchActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSearchActive = active,
            searchQuery = if (!active) "" else _uiState.value.searchQuery
        )
    }

    fun toggleGridView() {
        _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
    }

    fun setSortOrder(order: VideoSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
    }

    fun setResolutionFilter(filter: ResolutionCategory?) {
        _uiState.value = _uiState.value.copy(selectedResolutionFilter = filter)
    }

    fun selectFolder(folder: VideoFolder?) {
        _uiState.value = _uiState.value.copy(selectedFolder = folder)
    }

    fun selectVideoForDetails(video: VideoItem?) {
        _uiState.value = _uiState.value.copy(selectedVideoForDetails = video)
    }

    fun setSortSheetVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isSortSheetVisible = visible)
    }

    fun toggleFavorite(videoId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(videoId)
        }
    }

    fun importSingleVideoUri(uri: Uri, onImported: (VideoItem) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            try {
                val item = repository.addImportedVideo(uri)
                onImported(item)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(scanMessage = "Failed to import video: ${e.localizedMessage}")
            } finally {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    fun importFolderUri(treeUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            try {
                val count = repository.addImportedFolder(treeUri)
                _uiState.value = _uiState.value.copy(scanMessage = "Imported $count videos from folder")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(scanMessage = "Failed to import folder: ${e.localizedMessage}")
            } finally {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    suspend fun findVideoById(videoId: Long): VideoItem? {
        return repository.getVideoById(videoId)
    }

    fun clearScanMessage() {
        _uiState.value = _uiState.value.copy(scanMessage = null)
    }
}
