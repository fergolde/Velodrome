package com.fergolde.velodrome.data.local.queue

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.queueDataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
        by preferencesDataStore(name = "queue_snapshot")

/**
 * Local persistence for the player queue so it survives process death.
 * Event-driven only: written on track transitions, pauses, seeks and queue
 * mutations — never on a timer while playing.
 */
@Singleton
class QueueSnapshotStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(snapshot: QueueSnapshot) {
        context.queueDataStore.edit { prefs ->
            prefs[KEY_QUEUE] = json.encodeToString(snapshot)
        }
    }

    suspend fun load(): QueueSnapshot? {
        val raw = context.queueDataStore.data.first()[KEY_QUEUE] ?: return null
        return runCatching { json.decodeFromString<QueueSnapshot>(raw) }.getOrNull()
    }

    suspend fun clear() {
        context.queueDataStore.edit { prefs ->
            prefs.remove(KEY_QUEUE)
        }
    }

    private companion object {
        val KEY_QUEUE = stringPreferencesKey("queue_json")
    }
}
