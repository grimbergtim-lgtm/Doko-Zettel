package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doppelkopf_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val DEFAULT_BOCK_ROUNDS = intPreferencesKey("default_bock_rounds")
        val AIDA_EXTREME_MODE_ENABLED = booleanPreferencesKey("aida_extreme_mode_enabled")
        val AIDA_CUSTOM_TASKS = stringPreferencesKey("aida_custom_tasks")
    }

    val defaultBockRounds: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_BOCK_ROUNDS] ?: 4
    }

    suspend fun setDefaultBockRounds(rounds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_BOCK_ROUNDS] = rounds.coerceIn(1, 10)
        }
    }

    // AIDA Extreme Mode Schalter
    val aidaExtremeModeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AIDA_EXTREME_MODE_ENABLED] ?: false
    }

    suspend fun setAidaExtremeModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AIDA_EXTREME_MODE_ENABLED] = enabled
        }
    }

    // Eigene AIDA Extreme Mode Aufgaben
    val aidaCustomTasks: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val rawJson = preferences[PreferencesKeys.AIDA_CUSTOM_TASKS] ?: "[]"
        parseTasksJson(rawJson)
    }

    suspend fun addAidaCustomTask(task: String): Boolean {
        val clean = task.trim()
        if (clean.isEmpty()) return false
        context.dataStore.edit { preferences ->
            val list = parseTasksJson(preferences[PreferencesKeys.AIDA_CUSTOM_TASKS] ?: "[]").toMutableList()
            list.add(clean)
            preferences[PreferencesKeys.AIDA_CUSTOM_TASKS] = serializeTasksJson(list)
        }
        return true
    }

    suspend fun updateAidaCustomTask(index: Int, newTask: String): Boolean {
        val clean = newTask.trim()
        if (clean.isEmpty()) return false
        context.dataStore.edit { preferences ->
            val list = parseTasksJson(preferences[PreferencesKeys.AIDA_CUSTOM_TASKS] ?: "[]").toMutableList()
            if (index in 0 until list.size) {
                list[index] = clean
                preferences[PreferencesKeys.AIDA_CUSTOM_TASKS] = serializeTasksJson(list)
            }
        }
        return true
    }

    suspend fun deleteAidaCustomTask(index: Int) {
        context.dataStore.edit { preferences ->
            val list = parseTasksJson(preferences[PreferencesKeys.AIDA_CUSTOM_TASKS] ?: "[]").toMutableList()
            if (index in 0 until list.size) {
                list.removeAt(index)
                preferences[PreferencesKeys.AIDA_CUSTOM_TASKS] = serializeTasksJson(list)
            }
        }
    }

    suspend fun getAidaCustomTasksSync(): List<String> {
        val prefs = context.dataStore.data.first()
        return parseTasksJson(prefs[PreferencesKeys.AIDA_CUSTOM_TASKS] ?: "[]")
    }

    companion object {
        fun parseTasksJson(jsonString: String): List<String> {
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val item = array.optString(i, "").trim()
                    if (item.isNotEmpty()) {
                        list.add(item)
                    }
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun serializeTasksJson(tasks: List<String>): String {
            val array = JSONArray()
            tasks.forEach { if (it.trim().isNotEmpty()) array.put(it.trim()) }
            return array.toString()
        }
    }
}
