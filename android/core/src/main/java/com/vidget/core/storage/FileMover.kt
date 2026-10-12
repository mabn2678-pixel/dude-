package com.vidget.core.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.vidget.domain.model.DestinationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

sealed interface MoveProgress {
    data class InProgress(val bytesCopied: Long, val totalBytes: Long, val percent: Int) : MoveProgress
    data class Completed(val destinationUri: Uri?, val finalPath: String?) : MoveProgress
    data class Error(val throwable: Throwable) : MoveProgress
}

/**
 * Handles streaming transfer from temp app cache to the final selected destination.
 * Performs stream copying, tracks separate "Saving..." progress, and cleans up temp files.
 */
class FileMover(
    private val context: Context
) {

    /**
     * Moves a completed file from temporary app workspace to the selected destination.
     * Emits MoveProgress with fine-grained percentages for UI display.
     */
    fun moveFile(
        sourceTempFile: File,
        targetFilename: String,
        mimeType: String,
        destinationType: DestinationType,
        subfolder: String? = null
    ): Flow<MoveProgress> = flow {
        if (!sourceTempFile.exists() || sourceTempFile.length() <= 0L) {
            emit(MoveProgress.Error(IllegalArgumentException("Source temp file does not exist or is empty")))
            return@flow
        }

        val totalBytes = sourceTempFile.length()
        var destinationUri: Uri? = null
        var finalPath: String? = null

        try {
            when (destinationType) {
                is DestinationType.DownloadsDefault -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val relativePath = if (subfolder.isNullOrBlank()) {
                            "${Environment.DIRECTORY_DOWNLOADS}/VidGet"
                        } else {
                            "${Environment.DIRECTORY_DOWNLOADS}/VidGet/$subfolder"
                        }

                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, targetFilename)
                            put(MediaStore.Downloads.MIME_TYPE, mimeType)
                            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }

                        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            ?: throw IllegalStateException("Failed to create MediaStore.Downloads record")

                        destinationUri = uri
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            copyStreamWithProgress(sourceTempFile, out, totalBytes) { progress ->
                                emit(progress)
                            }
                        }

                        // Mark ready
                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    } else {
                        // Android 9 and older: Direct filesystem write
                        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val vidgetDir = File(downloadDir, if (subfolder.isNullOrBlank()) "VidGet" else "VidGet/$subfolder")
                        vidgetDir.mkdirs()
                        val destFile = File(vidgetDir, targetFilename)
                        finalPath = destFile.absolutePath

                        FileOutputStream(destFile).use { out ->
                            copyStreamWithProgress(sourceTempFile, out, totalBytes) { progress ->
                                emit(progress)
                            }
                        }
                    }
                }

                is DestinationType.Movies -> {
                    val isAudio = mimeType.startsWith("audio/")
                    val collectionUri = if (isAudio) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }

                    val baseDirectory = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
                    val relativePath = if (subfolder.isNullOrBlank()) {
                        "$baseDirectory/VidGet"
                    } else {
                        "$baseDirectory/VidGet/$subfolder"
                    }

                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, targetFilename)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }

                    val uri = context.contentResolver.insert(collectionUri, values)
                        ?: throw IllegalStateException("Failed to create MediaStore record")

                    destinationUri = uri
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        copyStreamWithProgress(sourceTempFile, out, totalBytes) { progress ->
                            emit(progress)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    }
                }

                is DestinationType.CustomFolder, is DestinationType.SdCard -> {
                    val uriString = if (destinationType is DestinationType.CustomFolder) {
                        destinationType.uriString
                    } else {
                        (destinationType as DestinationType.SdCard).uriString
                    }

                    val treeUri = Uri.parse(uriString)
                    val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        DocumentsContract.getTreeDocumentId(treeUri)
                    )

                    val targetDocUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        parentDocUri,
                        mimeType,
                        targetFilename
                    ) ?: throw IllegalStateException("Failed to create SAF document at destination")

                    destinationUri = targetDocUri
                    context.contentResolver.openOutputStream(targetDocUri)?.use { out ->
                        copyStreamWithProgress(sourceTempFile, out, totalBytes) { progress ->
                            emit(progress)
                        }
                    }
                }
            }

            // Successfully copied: delete temporary workspace file
            sourceTempFile.delete()

            emit(MoveProgress.Completed(destinationUri, finalPath))
        } catch (t: Throwable) {
            emit(MoveProgress.Error(t))
        }
    }.flowOn(Dispatchers.IO)

    private inline fun copyStreamWithProgress(
        source: File,
        out: OutputStream,
        totalBytes: Long,
        onProgress: (MoveProgress) -> Unit
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesCopied = 0L

        FileInputStream(source).use { input ->
            var bytes = input.read(buffer)
            while (bytes >= 0) {
                out.write(buffer, 0, bytes)
                bytesCopied += bytes
                val percent = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                onProgress(MoveProgress.InProgress(bytesCopied, totalBytes, percent))
                bytes = input.read(buffer)
            }
        }
        out.flush()
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024 // 64 KB streaming buffer
    }
}
