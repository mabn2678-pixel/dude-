package com.vidget.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vidget.domain.model.DestinationType
import com.vidget.domain.model.NameConflictResolution
import com.vidget.domain.model.NamingTemplateConfig
import com.vidget.domain.model.SubfolderOrganization
import com.vidget.domain.repository.DestinationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DestinationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : DestinationRepository {

    private object PreferencesKeys {
        val DEST_KIND = stringPreferencesKey("dest_kind")
        val CUSTOM_URI = stringPreferencesKey("custom_folder_uri")
        val CUSTOM_NAME = stringPreferencesKey("custom_folder_name")
        val SDCARD_URI = stringPreferencesKey("sdcard_uri")
        val SDCARD_NAME = stringPreferencesKey("sdcard_name")
        val NAMING_TEMPLATE = stringPreferencesKey("naming_template")
        val SUBFOLDER_ORG = stringPreferencesKey("subfolder_organization")
        val CONFLICT_RES = stringPreferencesKey("conflict_resolution")
    }

    override val destinationType: Flow<DestinationType> = dataStore.data.map { prefs ->
        val kind = prefs[PreferencesKeys.DEST_KIND] ?: "DOWNLOADS_DEFAULT"
        when (kind) {
            "MOVIES" -> DestinationType.Movies
            "CUSTOM_FOLDER" -> {
                val uri = prefs[PreferencesKeys.CUSTOM_URI]
                val name = prefs[PreferencesKeys.CUSTOM_NAME] ?: "Custom Folder"
                if (!uri.isNullOrBlank() && isUriPermissionValid(uri)) {
                    DestinationType.CustomFolder(uri, name)
                } else {
                    DestinationType.DownloadsDefault // Fallback if permission revoked
                }
            }
            "SD_CARD" -> {
                val uri = prefs[PreferencesKeys.SDCARD_URI]
                val name = prefs[PreferencesKeys.SDCARD_NAME] ?: "SD Card"
                if (!uri.isNullOrBlank() && isUriPermissionValid(uri)) {
                    DestinationType.SdCard(uri, name)
                } else {
                    DestinationType.DownloadsDefault
                }
            }
            else -> DestinationType.DownloadsDefault
        }
    }

    override val namingConfig: Flow<NamingTemplateConfig> = dataStore.data.map { prefs ->
        val template = prefs[PreferencesKeys.NAMING_TEMPLATE] ?: NamingTemplateConfig.DEFAULT_TEMPLATE
        val subfolderStr = prefs[PreferencesKeys.SUBFOLDER_ORG] ?: SubfolderOrganization.NONE.name
        val conflictStr = prefs[PreferencesKeys.CONFLICT_RES] ?: NameConflictResolution.AUTO_RENAME.name

        NamingTemplateConfig(
            template = template,
            subfolderOrganization = try { SubfolderOrganization.valueOf(subfolderStr) } catch (e: Exception) { SubfolderOrganization.NONE },
            conflictResolution = try { NameConflictResolution.valueOf(conflictStr) } catch (e: Exception) { NameConflictResolution.AUTO_RENAME }
        )
    }

    override suspend fun setDestinationType(type: DestinationType) {
        dataStore.edit { prefs ->
            when (type) {
                is DestinationType.DownloadsDefault -> {
                    prefs[PreferencesKeys.DEST_KIND] = "DOWNLOADS_DEFAULT"
                }
                is DestinationType.Movies -> {
                    prefs[PreferencesKeys.DEST_KIND] = "MOVIES"
                }
                is DestinationType.CustomFolder -> {
                    prefs[PreferencesKeys.DEST_KIND] = "CUSTOM_FOLDER"
                    prefs[PreferencesKeys.CUSTOM_URI] = type.uriString
                    prefs[PreferencesKeys.CUSTOM_NAME] = type.displayName
                }
                is DestinationType.SdCard -> {
                    prefs[PreferencesKeys.DEST_KIND] = "SD_CARD"
                    prefs[PreferencesKeys.SDCARD_URI] = type.uriString
                    prefs[PreferencesKeys.SDCARD_NAME] = type.displayName
                }
            }
        }
    }

    override suspend fun isUriPermissionValid(uriString: String): Boolean {
        if (uriString.isBlank()) return false
        val targetUri = try { Uri.parse(uriString) } catch (e: Exception) { return false }
        val permissions = context.contentResolver.persistedUriPermissions
        return permissions.any { it.uri == targetUri && it.isWritePermission }
    }

    override suspend fun releasePersistedUri(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(uri, flags)
        } catch (ignored: Exception) {
        }
    }

    override suspend fun setNamingTemplate(template: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.NAMING_TEMPLATE] = template
        }
    }

    override suspend fun setSubfolderOrganization(organization: SubfolderOrganization) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.SUBFOLDER_ORG] = organization.name
        }
    }

    override suspend fun setConflictResolution(resolution: NameConflictResolution) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.CONFLICT_RES] = resolution.name
        }
    }
}
