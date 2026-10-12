package com.vidget.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidget.domain.model.DownloadStatus
import com.vidget.domain.model.DownloadTask
import com.vidget.domain.model.FailureReason
import com.vidget.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DownloadUiEvent {
    data class ShowToast(val message: String) : DownloadUiEvent
    data class OpenPlayer(val fileUri: String) : DownloadUiEvent
    data class OpenStorageFolder(val folderUri: String) : DownloadUiEvent
    data class ShareMedia(val fileUri: String, val title: String) : DownloadUiEvent
    data class RequestFolderPick(val taskId: String) : DownloadUiEvent
    data class RequestStoragePermission(val taskId: String) : DownloadUiEvent
    data class ReExtractLink(val taskId: String, val url: String) : DownloadUiEvent
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    // Tab state: 0 = Active, 1 = Completed, 2 = Failed
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Currently focused error dialog (if any)
    private val _activeErrorDialog = MutableStateFlow<DownloadTask?>(null)
    val activeErrorDialog: StateFlow<DownloadTask?> = _activeErrorDialog.asStateFlow()

    // Event bus for one-time navigation/intent actions
    private val _events = MutableSharedFlow<DownloadUiEvent>()
    val events: SharedFlow<DownloadUiEvent> = _events.asSharedFlow()

    // Downstream stream sampled at 500ms to guarantee smooth 60fps Compose recomposition
    val allTasks = downloadRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks = allTasks.map { list ->
        list.filter { it.status in setOf(DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED, DownloadStatus.MERGING, DownloadStatus.SAVING, DownloadStatus.PAUSED) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks = allTasks.map { list ->
        list.filter { it.status == DownloadStatus.COMPLETED }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val failedTasks = allTasks.map { list ->
        list.filter { it.status in setOf(DownloadStatus.FAILED, DownloadStatus.CANCELLED) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun pauseTask(taskId: String) {
        viewModelScope.launch {
            downloadRepository.pauseTask(taskId)
        }
    }

    fun resumeTask(taskId: String) {
        viewModelScope.launch {
            downloadRepository.resumeTask(taskId)
        }
    }

    fun cancelTask(taskId: String) {
        viewModelScope.launch {
            downloadRepository.cancelTask(taskId)
        }
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch {
            downloadRepository.retryTask(taskId)
        }
    }

    fun deleteTask(taskId: String, deleteFromDisk: Boolean = false) {
        viewModelScope.launch {
            downloadRepository.deleteTask(taskId, deleteFromDisk)
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            downloadRepository.pauseAllActive()
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            downloadRepository.resumeAllPaused()
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadRepository.clearCompleted()
        }
    }

    fun showErrorDetails(task: DownloadTask) {
        _activeErrorDialog.value = task
    }

    fun dismissErrorDialog() {
        _activeErrorDialog.value = null
    }

    fun handleDialogAction(task: DownloadTask, reason: FailureReason) {
        dismissErrorDialog()
        viewModelScope.launch {
            when (reason) {
                FailureReason.NETWORK_ERROR -> {
                    downloadRepository.retryTask(task.id)
                }
                FailureReason.DISK_FULL -> {
                    _events.emit(DownloadUiEvent.RequestFolderPick(task.id))
                }
                FailureReason.PERMISSION_DENIED -> {
                    _events.emit(DownloadUiEvent.RequestStoragePermission(task.id))
                }
                FailureReason.MERGE_FAILED -> {
                    // Retry with MKV
                    downloadRepository.retryTask(task.id)
                }
                FailureReason.LINK_EXPIRED -> {
                    _events.emit(DownloadUiEvent.ReExtractLink(task.id, task.url))
                }
                FailureReason.UNSUPPORTED_OR_PRIVATE, FailureReason.UNKNOWN -> {
                    downloadRepository.retryTask(task.id)
                }
            }
        }
    }

    fun playMedia(task: DownloadTask) {
        val uri = task.localFilePath ?: task.url
        viewModelScope.launch {
            _events.emit(DownloadUiEvent.OpenPlayer(uri))
        }
    }

    fun openFolder(task: DownloadTask) {
        val path = task.destinationUri ?: task.localFilePath ?: ""
        viewModelScope.launch {
            _events.emit(DownloadUiEvent.OpenStorageFolder(path))
        }
    }

    fun shareMedia(task: DownloadTask) {
        val uri = task.localFilePath ?: task.url
        viewModelScope.launch {
            _events.emit(DownloadUiEvent.ShareMedia(uri, task.title))
        }
    }
}
