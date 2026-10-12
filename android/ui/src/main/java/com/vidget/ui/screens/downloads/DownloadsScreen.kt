package com.vidget.ui.screens.downloads

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidget.domain.model.DownloadTask
import com.vidget.ui.R
import com.vidget.ui.components.DownloadErrorDialog
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateToPlayer: (videoUri: String) -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val activeTasks by viewModel.activeTasks.collectAsState()
    val completedTasks by viewModel.completedTasks.collectAsState()
    val failedTasks by viewModel.failedTasks.collectAsState()
    val activeErrorDialog by viewModel.activeErrorDialog.collectAsState()

    // Handle UI Events (sharing, player navigation, toasts)
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DownloadUiEvent.OpenPlayer -> {
                    onNavigateToPlayer(event.fileUri)
                }
                is DownloadUiEvent.ShareMedia -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_TEXT, event.title)
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(event.fileUri))
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                }
                is DownloadUiEvent.OpenStorageFolder -> {
                    Toast.makeText(context, "Location: ${event.folderUri}", Toast.LENGTH_SHORT).show()
                }
                is DownloadUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is DownloadUiEvent.RequestFolderPick -> {
                    Toast.makeText(context, "الرجاء اختيار مجلد بديل من الإعدادات", Toast.LENGTH_LONG).show()
                }
                is DownloadUiEvent.RequestStoragePermission -> {
                    Toast.makeText(context, "إذن الوصول للذاكرة مطلوب لإكمال الحفظ", Toast.LENGTH_LONG).show()
                }
                is DownloadUiEvent.ReExtractLink -> {
                    Toast.makeText(context, "جارٍ إعادة فحص الرابط…", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_downloads_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    when (selectedTab) {
                        0 -> {
                            // Active tab actions: Pause All, Resume All
                            IconButton(onClick = { viewModel.pauseAll() }) {
                                Icon(
                                    imageVector = Icons.rounded.PauseCircle,
                                    contentDescription = stringResource(R.string.downloads_action_pause_all)
                                )
                            }
                            IconButton(onClick = { viewModel.resumeAll() }) {
                                Icon(
                                    imageVector = Icons.rounded.PlayCircle,
                                    contentDescription = stringResource(R.string.downloads_action_resume_all)
                                )
                            }
                        }
                        1 -> {
                            // Completed tab action: Clear list
                            IconButton(onClick = { viewModel.clearCompleted() }) {
                                Icon(
                                    imageVector = Icons.rounded.DeleteSweep,
                                    contentDescription = stringResource(R.string.downloads_action_clear_completed)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Tabs with badge counts
            val tabTitles = listOf(
                stringResource(R.string.tab_downloads_active, activeTasks.size),
                stringResource(R.string.tab_downloads_completed, completedTasks.size),
                stringResource(R.string.tab_downloads_failed, failedTasks.size)
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    DownloadsListSection(
                        tasks = activeTasks,
                        emptyIcon = Icons.rounded.CloudDownload,
                        emptyTitle = stringResource(R.string.empty_active_title),
                        emptyDesc = stringResource(R.string.empty_active_desc),
                        viewModel = viewModel,
                        context = context
                    )
                }
                1 -> {
                    DownloadsListSection(
                        tasks = completedTasks,
                        emptyIcon = Icons.rounded.CheckCircle,
                        emptyTitle = stringResource(R.string.empty_completed_title),
                        emptyDesc = stringResource(R.string.empty_completed_desc),
                        viewModel = viewModel,
                        context = context
                    )
                }
                2 -> {
                    DownloadsListSection(
                        tasks = failedTasks,
                        emptyIcon = Icons.rounded.ErrorOutline,
                        emptyTitle = stringResource(R.string.empty_failed_title),
                        emptyDesc = stringResource(R.string.empty_failed_desc),
                        viewModel = viewModel,
                        context = context
                    )
                }
            }
        }
    }

    // Active classified error dialog
    activeErrorDialog?.let { task ->
        DownloadErrorDialog(
            task = task,
            onDismiss = { viewModel.dismissErrorDialog() },
            onPrimaryAction = { reason ->
                viewModel.handleDialogAction(task, reason)
            }
        )
    }
}

@Composable
private fun DownloadsListSection(
    tasks: List<DownloadTask>,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyDesc: String,
    viewModel: DownloadsViewModel,
    context: Context
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = emptyIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = emptyTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = emptyDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = tasks,
                key = { it.id }
            ) { task ->
                DownloadItemCard(
                    task = task,
                    onPause = { viewModel.pauseTask(task.id) },
                    onResume = { viewModel.resumeTask(task.id) },
                    onCancel = { viewModel.cancelTask(task.id) },
                    onRetry = { viewModel.retryTask(task.id) },
                    onPlay = { viewModel.playMedia(task) },
                    onOpenFolder = { viewModel.openFolder(task) },
                    onShare = { viewModel.shareMedia(task) },
                    onShowErrorDetails = { viewModel.showErrorDetails(task) },
                    onDelete = { deleteWithFile -> viewModel.deleteTask(task.id, deleteWithFile) },
                    onCopyLink = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("URL", task.url)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ الرابط", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
