export interface ProjectFile {
  path: string;
  name: string;
  module: string;
  language: 'kotlin' | 'toml' | 'xml' | 'pro' | 'markdown';
  content: string;
  description: string;
}

export const PROJECT_FILES: ProjectFile[] = [
  // --- Storage & Destination (Prompt 6) ---
  {
    path: 'core/src/main/java/com/vidget/core/storage/StorageSpaceChecker.kt',
    name: 'StorageSpaceChecker.kt',
    module: 'core',
    language: 'kotlin',
    description: 'فحص مساحة التخزين قبل البدء (1.10x للمؤقت + 2x لعملية دمج المسارين المنفصلين عبر StatFs)',
    content: `package com.vidget.core.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import kotlin.math.max

data class SpaceCheckResult(
    val isSufficient: Boolean,
    val requiredTempBytes: Long,
    val requiredFinalBytes: Long,
    val availableTempBytes: Long,
    val availableDestinationBytes: Long,
    val deficitBytes: Long
)

object StorageSpaceChecker {

    const val TEMP_BUFFER_MULTIPLIER = 1.10

    fun calculateRequiredTempBytes(
        estimatedSizeBytes: Long,
        hasSeparateStreams: Boolean
    ): Long {
        if (estimatedSizeBytes <= 0) return 0L
        val baseBuffer = (estimatedSizeBytes * TEMP_BUFFER_MULTIPLIER).toLong()
        val mergeOverhead = if (hasSeparateStreams) estimatedSizeBytes else 0L
        return baseBuffer + mergeOverhead
    }

    fun getAvailableBytes(path: String): Long {
        return try {
            val file = File(path)
            val targetFile = if (file.exists()) file else file.parentFile ?: file
            val stat = StatFs(targetFile.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    fun getTotalBytes(path: String): Long {
        return try {
            val file = File(path)
            val targetFile = if (file.exists()) file else file.parentFile ?: file
            val stat = StatFs(targetFile.absolutePath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    fun getFreeSpaceRatio(path: String): Float {
        val total = getTotalBytes(path)
        if (total <= 0L) return 0f
        val free = getAvailableBytes(path)
        return (free.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    fun checkSpace(
        context: Context,
        estimatedSizeBytes: Long,
        hasSeparateStreams: Boolean,
        destinationPath: String? = null
    ): SpaceCheckResult {
        val requiredTemp = calculateRequiredTempBytes(estimatedSizeBytes, hasSeparateStreams)
        val requiredFinal = estimatedSizeBytes

        val tempDir = context.getExternalFilesDir("downloads") ?: context.filesDir
        val availableTemp = getAvailableBytes(tempDir.absolutePath)

        val finalDir = destinationPath ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath ?: tempDir.absolutePath
        val availableDestination = getAvailableBytes(finalDir)

        val tempDeficit = max(0L, requiredTemp - availableTemp)
        val destDeficit = max(0L, requiredFinal - availableDestination)
        val totalDeficit = max(tempDeficit, destDeficit)

        val isSufficient = (availableTemp >= requiredTemp) && (availableDestination >= requiredFinal)

        return SpaceCheckResult(
            isSufficient = isSufficient,
            requiredTempBytes = requiredTemp,
            requiredFinalBytes = requiredFinal,
            availableTempBytes = availableTemp,
            availableDestinationBytes = availableDestination,
            deficitBytes = totalDeficit
        )
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0

        return when {
            bytes >= gb -> String.format(java.util.Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(java.util.Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(java.util.Locale.US, "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}`
  },
  {
    path: 'core/src/main/java/com/vidget/core/storage/FileMover.kt',
    name: 'FileMover.kt',
    module: 'core',
    language: 'kotlin',
    description: 'نقل الملف النهائي بالتدفق مع تقدم منفصل "جارٍ الحفظ" ثم حذف الملف المؤقت بدعم MediaStore و SAF',
    content: `package com.vidget.core.storage

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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

sealed interface MoveProgress {
    data class InProgress(val bytesCopied: Long, val totalBytes: Long, val percent: Int) : MoveProgress
    data class Completed(val destinationUri: Uri?, val finalPath: String?) : MoveProgress
    data class Error(val throwable: Throwable) : MoveProgress
}

class FileMover(
    private val context: Context
) {

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
                            "\${Environment.DIRECTORY_DOWNLOADS}/VidGet"
                        } else {
                            "\${Environment.DIRECTORY_DOWNLOADS}/VidGet/$subfolder"
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

                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                    } else {
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
        private const val DEFAULT_BUFFER_SIZE = 64 * 1024
    }
}`
  },
  {
    path: 'core/src/main/java/com/vidget/core/naming/FileNamer.kt',
    name: 'FileNamer.kt',
    module: 'core',
    language: 'kotlin',
    description: 'تنظيف الاسم (استبدال الرموز غير المدعومة، إزالة المسافات والنقاط الأخيرة، حد 120 حرفاً آمناً، وأسماء ويندوز المحجوزة)',
    content: `package com.vidget.core.naming

import com.vidget.domain.model.SubfolderOrganization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileNamer {

    const val MAX_BASE_NAME_LENGTH = 120
    const val PART_EXTENSION = ".part"

    private val ILLEGAL_CHAR_REGEX = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F\\u007F]")

    private val RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    fun sanitizeBaseName(rawName: String): String {
        if (rawName.isBlank()) return "download"

        var cleaned = rawName.replace(ILLEGAL_CHAR_REGEX, "_")
        cleaned = cleaned.trimEnd { it == '.' || it.isWhitespace() }

        if (cleaned.isBlank()) cleaned = "download"

        cleaned = truncateSafely(cleaned, MAX_BASE_NAME_LENGTH)
        cleaned = cleaned.trimEnd { it == '.' || it.isWhitespace() }
        if (cleaned.isBlank()) cleaned = "download"

        val upper = cleaned.uppercase(Locale.US)
        if (upper in RESERVED_NAMES) {
            cleaned = "_$cleaned"
        }

        return cleaned
    }

    fun truncateSafely(input: String, maxCodePoints: Int): String {
        var codePointsCount = 0
        var byteIndex = 0

        while (byteIndex < input.length && codePointsCount < maxCodePoints) {
            val codePoint = input.codePointAt(byteIndex)
            byteIndex += Character.charCount(codePoint)
            codePointsCount++
        }

        return input.substring(0, byteIndex)
    }

    fun generateTempFileName(baseName: String, ext: String): String {
        val sanitizedBase = sanitizeBaseName(baseName)
        val cleanExt = ext.removePrefix(".")
        return "$sanitizedBase.$cleanExt$PART_EXTENSION"
    }

    fun generateFinalFileName(baseName: String, ext: String): String {
        val sanitizedBase = sanitizeBaseName(baseName)
        val cleanExt = ext.removePrefix(".")
        return "$sanitizedBase.$cleanExt"
    }

    fun generateSubtitleFileName(baseName: String, languageCode: String): String {
        val sanitizedBase = sanitizeBaseName(baseName)
        val cleanLang = languageCode.trim().lowercase(Locale.US)
        return "$sanitizedBase.$cleanLang.srt"
    }

    fun resolveSubfolder(
        organization: SubfolderOrganization,
        platform: String,
        date: Date = Date()
    ): String? {
        return when (organization) {
            SubfolderOrganization.NONE -> null
            SubfolderOrganization.PLATFORM -> sanitizeBaseName(platform)
            SubfolderOrganization.DATE -> SimpleDateFormat("yyyy-MM", Locale.US).format(date)
        }
    }
}`
  },
  {
    path: 'core/src/main/java/com/vidget/core/naming/TemplateParser.kt',
    name: 'TemplateParser.kt',
    module: 'core',
    language: 'kotlin',
    description: 'محلل قوالب التسمية ومعاينة حيّة بالمتغيرات المسموحة فقط ({title}, {quality}, {ext}, {platform}, {date}, {id})',
    content: `package com.vidget.core.naming

import com.vidget.domain.model.NamingTemplateConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VideoTemplateContext(
    val title: String,
    val quality: String,
    val ext: String,
    val platform: String,
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val id: String
)

object TemplateParser {

    val SAMPLE_PREVIEW_CONTEXT = VideoTemplateContext(
        title = "دورة احتراف أندرويد الحديثة",
        quality = "1080p",
        ext = "mp4",
        platform = "YouTube",
        date = "2026-10-12",
        id = "vid_893274"
    )

    fun validateTemplate(template: String): Boolean {
        if (template.isBlank()) return false
        val tokenRegex = Regex("\\\\{[^}]+\\\\}")
        val tokensFound = tokenRegex.findAll(template).map { it.value }.toSet()
        return tokensFound.all { it in NamingTemplateConfig.ALLOWED_PLACEHOLDERS }
    }

    fun parse(template: String, context: VideoTemplateContext): String {
        val safeTemplate = if (validateTemplate(template)) template else NamingTemplateConfig.DEFAULT_TEMPLATE

        var result = safeTemplate
            .replace("{title}", context.title)
            .replace("{quality}", context.quality)
            .replace("{ext}", context.ext.removePrefix("."))
            .replace("{platform}", context.platform)
            .replace("{date}", context.date)
            .replace("{id}", context.id)

        val cleanExt = context.ext.removePrefix(".")
        if (!result.endsWith(".$cleanExt")) {
            result = "$result.$cleanExt"
        }

        return result
    }

    fun preview(template: String): String {
        return parse(template, SAMPLE_PREVIEW_CONTEXT)
    }
}`
  },
  {
    path: 'core/src/main/java/com/vidget/core/naming/ConflictResolver.kt',
    name: 'ConflictResolver.kt',
    module: 'core',
    language: 'kotlin',
    description: 'معالجة تعارض أسماء الملفات (إعادة تسمية تلقائية بإضافة (1) و(2)، استبدال، تخطي، اسأل كل مرة)',
    content: `package com.vidget.core.naming

import com.vidget.domain.model.NameConflictResolution

sealed interface ConflictResolutionResult {
    data class ProceedWithFilename(val filename: String, val shouldOverwrite: Boolean = false) : ConflictResolutionResult
    data object SkipDownload : ConflictResolutionResult
    data object RequiresUserPrompt : ConflictResolutionResult
}

object ConflictResolver {

    fun resolveUniqueName(
        sanitizedBase: String,
        cleanExt: String,
        fileExistsCheck: (String) -> Boolean
    ): String {
        val initialName = "$sanitizedBase.$cleanExt"
        if (!fileExistsCheck(initialName)) {
            return initialName
        }

        var counter = 1
        while (true) {
            val candidate = "$sanitizedBase ($counter).$cleanExt"
            if (!fileExistsCheck(candidate)) {
                return candidate
            }
            counter++
        }
    }

    fun handleConflict(
        sanitizedBase: String,
        cleanExt: String,
        strategy: NameConflictResolution,
        fileExistsCheck: (String) -> Boolean
    ): ConflictResolutionResult {
        val initialName = "$sanitizedBase.$cleanExt"
        if (!fileExistsCheck(initialName)) {
            return ConflictResolutionResult.ProceedWithFilename(initialName)
        }

        return when (strategy) {
            NameConflictResolution.AUTO_RENAME -> {
                val unique = resolveUniqueName(sanitizedBase, cleanExt, fileExistsCheck)
                ConflictResolutionResult.ProceedWithFilename(unique)
            }
            NameConflictResolution.OVERWRITE -> {
                ConflictResolutionResult.ProceedWithFilename(initialName, shouldOverwrite = true)
            }
            NameConflictResolution.SKIP -> {
                ConflictResolutionResult.SkipDownload
            }
            NameConflictResolution.ASK_EVERY_TIME -> {
                ConflictResolutionResult.RequiresUserPrompt
            }
        }
    }
}`
  },
  {
    path: 'domain/src/main/java/com/vidget/domain/model/DestinationType.kt',
    name: 'DestinationType.kt',
    module: 'domain',
    language: 'kotlin',
    description: 'أنواع الوجهات الأربعة (DownloadsDefault, Movies, CustomFolder, SdCard)',
    content: `package com.vidget.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface DestinationType {

    @Serializable
    data object DownloadsDefault : DestinationType

    @Serializable
    data object Movies : DestinationType

    @Serializable
    data class CustomFolder(
        val uriString: String,
        val displayName: String
    ) : DestinationType

    @Serializable
    data class SdCard(
        val uriString: String,
        val displayName: String
    ) : DestinationType
}`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/components/DestinationPickerSheet.kt',
    name: 'DestinationPickerSheet.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'لوحة BottomSheet لاختيار الوجهة مع 4 بطاقات وشريط نسبة المساحة الحرة وزر تغيير',
    content: `package com.vidget.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidget.domain.model.DestinationType
import com.vidget.ui.R
import com.vidget.ui.theme.MinTouchTargetSize
import com.vidget.ui.theme.VidGetTheme

data class DestinationOptionItem(
    val type: DestinationType,
    val titleResId: Int,
    val readablePath: String,
    val icon: ImageVector,
    val freeSpaceFormatted: String,
    val freeSpaceRatio: Float,
    val isCustomOrSdCard: Boolean = false,
    val hintResId: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationPickerSheet(
    selectedType: DestinationType,
    options: List<DestinationOptionItem>,
    onSelectDestination: (DestinationType) -> Unit,
    onChangeCustomFolderClick: () -> Unit,
    onChangeSdCardClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = VidGetTheme.spacing

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.large)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dest_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.dest_picker_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                items(options.size) { index ->
                    val option = options[index]
                    val isSelected = when {
                        option.type is DestinationType.DownloadsDefault && selectedType is DestinationType.DownloadsDefault -> true
                        option.type is DestinationType.Movies && selectedType is DestinationType.Movies -> true
                        option.type is DestinationType.CustomFolder && selectedType is DestinationType.CustomFolder -> true
                        option.type is DestinationType.SdCard && selectedType is DestinationType.SdCard -> true
                        else -> false
                    }

                    DestinationCard(
                        option = option,
                        isSelected = isSelected,
                        onCardClick = { onSelectDestination(option.type) },
                        onChangeClick = {
                            if (option.type is DestinationType.CustomFolder) onChangeCustomFolderClick()
                            else if (option.type is DestinationType.SdCard) onChangeSdCardClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    option: DestinationOptionItem,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = VidGetTheme.spacing
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onCardClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(spacing.medium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(option.titleResId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = option.readablePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }

                if (option.isCustomOrSdCard) {
                    TextButton(
                        onClick = onChangeClick,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.height(MinTouchTargetSize)
                    ) {
                        Text(
                            text = stringResource(R.string.dest_change_btn),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onCardClick
                )
            }

            Spacer(modifier = Modifier.height(spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.freeSpaceFormatted,
                    style = VidGetTheme.metricTextStyle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.extraSmall))

            LinearProgressIndicator(
                progress = { option.freeSpaceRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (option.freeSpaceRatio < 0.10f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (option.hintResId != null) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = stringResource(option.hintResId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )
            }
        }
    }
}`
  },
  {
    path: 'core/src/test/java/com/vidget/core/storage/StorageSpaceCheckerTest.kt',
    name: 'StorageSpaceCheckerTest.kt',
    module: 'core',
    language: 'kotlin',
    description: 'اختبارات وحدة لحساب المساحة المطلوبة (1.10x للمؤقت و2.10x لعملية دمج المسارين)',
    content: `package com.vidget.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageSpaceCheckerTest {

    @Test
    fun calculateRequiredTempBytes_single_stream_applies_10_percent_buffer() {
        val estimatedBytes = 100_000_000L
        val required = StorageSpaceChecker.calculateRequiredTempBytes(
            estimatedSizeBytes = estimatedBytes,
            hasSeparateStreams = false
        )
        assertEquals(110_000_000L, required)
    }

    @Test
    fun calculateRequiredTempBytes_separate_streams_adds_merge_overhead() {
        val estimatedBytes = 100_000_000L
        val required = StorageSpaceChecker.calculateRequiredTempBytes(
            estimatedSizeBytes = estimatedBytes,
            hasSeparateStreams = true
        )
        assertEquals(210_000_000L, required)
    }

    @Test
    fun calculateRequiredTempBytes_zero_or_negative_returns_zero() {
        assertEquals(0L, StorageSpaceChecker.calculateRequiredTempBytes(0L, false))
        assertEquals(0L, StorageSpaceChecker.calculateRequiredTempBytes(-500L, true))
    }

    @Test
    fun formatBytes_formats_units_correctly() {
        assertEquals("0 B", StorageSpaceChecker.formatBytes(0L))
        assertEquals("500 B", StorageSpaceChecker.formatBytes(500L))
        assertEquals("1.0 KB", StorageSpaceChecker.formatBytes(1024L))
        assertEquals("50.0 MB", StorageSpaceChecker.formatBytes(50L * 1024 * 1024))
        assertEquals("1.50 GB", StorageSpaceChecker.formatBytes((1.50 * 1024 * 1024 * 1024).toLong()))
    }
}`
  },
  {
    path: 'core/src/test/java/com/vidget/core/naming/FileNamerTest.kt',
    name: 'FileNamerTest.kt',
    module: 'core',
    language: 'kotlin',
    description: 'اختبارات وحدة شاملة: أسماء عربية، إيموجي، أسماء طويلة 120 حرفاً، أسماء ويندوز المحجوزة، وتكرار متعدد',
    content: `package com.vidget.core.naming

import com.vidget.domain.model.NameConflictResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamerTest {

    @Test
    fun sanitizeBaseName_preserves_arabic_characters_and_diacritics() {
        val rawArabic = "تَحْمِيلُ فِيدْيُو رَائِعْ وَمُمَيَّزْ"
        val sanitized = FileNamer.sanitizeBaseName(rawArabic)
        assertEquals(rawArabic, sanitized)
    }

    @Test
    fun sanitizeBaseName_preserves_emojis_and_surrogate_pairs() {
        val rawWithEmoji = "فيديو رائع 🚀🔥 2026 🎉"
        val sanitized = FileNamer.sanitizeBaseName(rawWithEmoji)
        assertEquals(rawWithEmoji, sanitized)
    }

    @Test
    fun sanitizeBaseName_replaces_illegal_fat32_and_control_characters() {
        val illegalInput = "Video: How to code? <Part 1> / \\"Best\\" | *awesome* \\\\ tutorial"
        val expected = "Video_ How to code_ _Part 1_ _ _Best_ _ _awesome_ _ tutorial"
        val sanitized = FileNamer.sanitizeBaseName(illegalInput)
        assertEquals(expected, sanitized)
    }

    @Test
    fun sanitizeBaseName_removes_trailing_dots_and_spaces() {
        val input = "My Important Video....   "
        val sanitized = FileNamer.sanitizeBaseName(input)
        assertEquals("My Important Video", sanitized)
    }

    @Test
    fun sanitizeBaseName_handles_windows_reserved_names() {
        val reservedCon = "con"
        val reservedNul = "NUL"
        val reservedPrn = "Prn"
        val reservedCom1 = "COM1"

        assertEquals("_con", FileNamer.sanitizeBaseName(reservedCon))
        assertEquals("_NUL", FileNamer.sanitizeBaseName(reservedNul))
        assertEquals("_Prn", FileNamer.sanitizeBaseName(reservedPrn))
        assertEquals("_COM1", FileNamer.sanitizeBaseName(reservedCom1))
    }

    @Test
    fun sanitizeBaseName_truncates_long_names_at_120_codepoints_without_splitting_pairs() {
        val emojiString = "🔥".repeat(150)
        val sanitized = FileNamer.sanitizeBaseName(emojiString)
        assertEquals(120, sanitized.codePointCount(0, sanitized.length))
        assertEquals("🔥".repeat(120), sanitized)
    }

    @Test
    fun generateSubtitleFileName_appends_clean_language_suffix() {
        val subAr = FileNamer.generateSubtitleFileName("lecture_01", "ar")
        val subEn = FileNamer.generateSubtitleFileName("lecture_01", "EN")
        assertEquals("lecture_01.ar.srt", subAr)
        assertEquals("lecture_01.en.srt", subEn)
    }

    @Test
    fun conflictResolver_increments_numbers_properly() {
        val existingFiles = mutableSetOf("test_video.mp4", "test_video (1).mp4", "test_video (2).mp4")
        val uniqueName = ConflictResolver.resolveUniqueName(
            sanitizedBase = "test_video",
            cleanExt = "mp4",
            fileExistsCheck = { existingFiles.contains(it) }
        )
        assertEquals("test_video (3).mp4", uniqueName)
    }

    @Test
    fun templateParser_substitutes_allowed_placeholders() {
        val context = VideoTemplateContext(
            title = "أندرويد 15 للمحترفين",
            quality = "1080p",
            ext = "mp4",
            platform = "YouTube",
            date = "2026-10-12",
            id = "xyz123"
        )
        val template = "[{platform}] {title} ({quality}) - {date}.{ext}"
        val parsed = TemplateParser.parse(template, context)
        assertEquals("[YouTube] أندرويد 15 للمحترفين (1080p) - 2026-10-12.mp4", parsed)
    }

    @Test
    fun templateParser_rejects_unauthorized_placeholders() {
        assertFalse(TemplateParser.validateTemplate("{title} - {hacked_variable}"))
        assertTrue(TemplateParser.validateTemplate("{title} - {quality}.{ext}"))
    }
}`
  }
];
