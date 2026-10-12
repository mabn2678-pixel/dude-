package com.vidget.ui.screens.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidget.core.storage.StorageSpaceChecker
import com.vidget.domain.model.DestinationType
import com.vidget.domain.model.MediaInfo
import com.vidget.domain.model.QualityKind
import com.vidget.domain.model.QualityOption
import com.vidget.domain.model.SubtitleTrack
import com.vidget.domain.parser.ParsedQualityResult
import com.vidget.domain.parser.QualityParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface VideoDetailsUiState {
    data object Loading : VideoDetailsUiState
    data object Empty : VideoDetailsUiState
    data class Error(val message: String) : VideoDetailsUiState
    data class Success(
        val mediaInfo: MediaInfo,
        val videoOptions: List<QualityOption>,
        val audioOptions: List<QualityOption>,
        val selectedOption: QualityOption?,
        val selectedTab: Int = 0, // 0 = Video, 1 = Audio, 2 = Subtitles
        val selectedSubtitles: Set<String> = emptySet(), // language codes
        val embedSubtitles: Boolean = true,
        val destination: DestinationType = DestinationType.DownloadsDefault,
        val availableFreeBytes: Long = 0L,
        val totalSizeBytes: Long = 0L,
        val formattedTotalSize: String = "",
        val isPlaylist: Boolean = false,
        val selectedPlaylistItemIds: Set<String> = emptySet()
    ) : VideoDetailsUiState
}

@HiltViewModel
class VideoDetailsViewModel @Inject constructor(
    private val qualityParser: QualityParser
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideoDetailsUiState>(VideoDetailsUiState.Empty)
    val uiState: StateFlow<VideoDetailsUiState> = _uiState.asStateFlow()

    fun loadMedia(mediaInfo: MediaInfo, defaultDestination: DestinationType = DestinationType.DownloadsDefault) {
        _uiState.value = VideoDetailsUiState.Loading

        val parsed: ParsedQualityResult = qualityParser.parse(mediaInfo)

        // Select default quality: "Best" if available, else first
        val defaultOption = parsed.videoOptions.firstOrNull { it.isBest }
            ?: parsed.videoOptions.firstOrNull()
            ?: parsed.audioOptions.firstOrNull()

        val playlistItemIds = if (mediaInfo.isPlaylist) {
            mediaInfo.playlistItems.map { it.id }.toSet()
        } else {
            emptySet()
        }

        val totalSize = defaultOption?.sizeBytes ?: 0L

        _uiState.value = VideoDetailsUiState.Success(
            mediaInfo = mediaInfo,
            videoOptions = parsed.videoOptions,
            audioOptions = parsed.audioOptions,
            selectedOption = defaultOption,
            selectedTab = 0,
            selectedSubtitles = emptySet(),
            embedSubtitles = true,
            destination = defaultDestination,
            availableFreeBytes = 5_000_000_000L, // 5 GB default simulation
            totalSizeBytes = totalSize,
            formattedTotalSize = StorageSpaceChecker.formatBytes(totalSize),
            isPlaylist = mediaInfo.isPlaylist,
            selectedPlaylistItemIds = playlistItemIds
        )
    }

    fun selectOption(option: QualityOption) {
        _uiState.update { current ->
            if (current !is VideoDetailsUiState.Success) return@update current
            val newTotal = option.sizeBytes
            current.copy(
                selectedOption = option,
                totalSizeBytes = newTotal,
                formattedTotalSize = if (option.isLive) "بث مباشر" else StorageSpaceChecker.formatBytes(newTotal)
            )
        }
    }

    fun setTab(index: Int) {
        _uiState.update { current ->
            if (current !is VideoDetailsUiState.Success) return@update current
            current.copy(selectedTab = index)
        }
    }

    fun toggleSubtitle(language: String) {
        _uiState.update { current ->
            if (current !is VideoDetailsUiState.Success) return@update current
            val updated = if (current.selectedSubtitles.contains(language)) {
                current.selectedSubtitles - language
            } else {
                current.selectedSubtitles + language
            }
            current.copy(selectedSubtitles = updated)
        }
    }

    fun setEmbedSubtitles(embed: Boolean) {
        _uiState.update { current ->
            if (current !is VideoDetailsUiState.Success) return@update current
            current.copy(embedSubtitles = embed)
        }
    }

    fun setDestination(destination: DestinationType) {
        _uiState.update { current ->
            if (current !is VideoDetailsUiState.Success) return@update current
            current.copy(destination = destination)
        }
    }

    fun togglePlaylistItem(id: String) {
        _uiState.update { current ->
            if (current !is VideoDetailsUiState.Success) return@update current
            val updated = if (current.selectedPlaylistItemIds.contains(id)) {
                current.selectedPlaylistItemIds - id
            } else {
                current.selectedPlaylistItemIds + id
            }
            current.copy(selectedPlaylistItemIds = updated)
        }
    }

    fun selectAllPlaylistItems(selectAll: Boolean) {
        _uiState.update { current ->
            if (current !is VideoDetailsUiState.Success) return@update current
            val updated = if (selectAll) {
                current.mediaInfo.playlistItems.map { it.id }.toSet()
            } else {
                emptySet()
            }
            current.copy(selectedPlaylistItemIds = updated)
        }
    }
}
