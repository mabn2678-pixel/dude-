package com.vidget.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.vidget.data.repository.ThemePreferencesRepositoryImpl
import com.vidget.domain.repository.ThemePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesBindingModule {

    @Binds
    @Singleton
    abstract fun bindThemePreferencesRepository(
        impl: ThemePreferencesRepositoryImpl
    ): ThemePreferencesRepository

    @Binds
    @Singleton
    abstract fun bindDestinationRepository(
        impl: com.vidget.data.repository.DestinationRepositoryImpl
    ): com.vidget.domain.repository.DestinationRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesDataStoreModule {

    private const val PREFERENCES_NAME = "vidget_preferences"

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(PREFERENCES_NAME) }
        )
    }
}
