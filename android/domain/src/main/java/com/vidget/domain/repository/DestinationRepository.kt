package com.vidget.domain.repository

import com.vidget.domain.model.DestinationType
import com.vidget.domain.model.NameConflictResolution
import com.vidget.domain.model.NamingTemplateConfig
import com.vidget.domain.model.SubfolderOrganization
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing destination storage locations, SAF persisted URIs,
 * and file naming preferences.
 */
interface DestinationRepository {

    /**
     * Emits current active DestinationType.
     */
    val destinationType: Flow<DestinationType>

    /**
     * Emits naming and organization configuration.
     */
    val namingConfig: Flow<NamingTemplateConfig>

    /**
     * Updates selected destination type.
     */
    suspend fun setDestinationType(type: DestinationType)

    /**
     * Verifies if a given custom SAF tree URI still has active persistable permissions.
     */
    suspend fun isUriPermissionValid(uriString: String): Boolean

    /**
     * Releases persisted URI permission when changing away or removing.
     */
    suspend fun releasePersistedUri(uriString: String)

    /**
     * Updates naming template string.
     */
    suspend fun setNamingTemplate(template: String)

    /**
     * Updates subfolder organization scheme.
     */
    suspend fun setSubfolderOrganization(organization: SubfolderOrganization)

    /**
     * Updates default conflict resolution strategy.
     */
    suspend fun setConflictResolution(resolution: NameConflictResolution)
}
