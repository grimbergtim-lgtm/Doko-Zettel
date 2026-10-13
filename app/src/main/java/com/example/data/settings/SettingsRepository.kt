package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doppelkopf_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val DEFAULT_BOCK_ROUNDS = intPreferencesKey("default_bock_rounds")
    }

    val defaultBockRounds: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_BOCK_ROUNDS] ?: 4
    }

    suspend fun setDefaultBockRounds(rounds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_BOCK_ROUNDS] = rounds.coerceIn(1, 10)
        }
    }
}
