package com.example.soundpad

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SoundRepository(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "soundpad_prefs")

    companion object {
        private val SOUND_PREFIX = "sound_"
        private val FAVORITE_PREFIX = "fav_"

        private fun getSoundKey(id: Int) = stringPreferencesKey("${SOUND_PREFIX}$id")
        private fun getFavoriteKey(id: Int) = stringPreferencesKey("${FAVORITE_PREFIX}$id")
    }

    suspend fun saveSound(sound: SoundItem) {
        context.dataStore.edit { prefs ->
            prefs[getSoundKey(sound.id)] = "${sound.name}|${sound.uri}"
            if (sound.isFavorite) {
                prefs[getFavoriteKey(sound.id)] = "${sound.name}|${sound.uri}"
            } else {
                prefs.remove(getFavoriteKey(sound.id))
            }
        }
    }

    suspend fun removeSound(id: Int) {
        context.dataStore.edit { prefs ->
            prefs.remove(getSoundKey(id))
            prefs.remove(getFavoriteKey(id))
        }
    }

    suspend fun toggleFavorite(sound: SoundItem) {
        context.dataStore.edit { prefs ->
            if (sound.isFavorite) {
                prefs[getFavoriteKey(sound.id)] = "${sound.name}|${sound.uri}"
            } else {
                prefs.remove(getFavoriteKey(sound.id))
            }
            prefs[getSoundKey(sound.id)] = "${sound.name}|${sound.uri}"
        }
    }

    fun getSounds(): Flow<List<SoundItem>> {
        return context.dataStore.data.map { prefs ->
            prefs.asMap()
                .filterKeys { it.name.startsWith(SOUND_PREFIX) }
                .mapNotNull { (key, value) ->
                    val id = key.name.removePrefix(SOUND_PREFIX).toIntOrNull() ?: return@mapNotNull null
                    val (name, uri) = value.toString().split("|", limit = 2)
                    val isFavorite = prefs[getFavoriteKey(id)] != null
                    SoundItem(id, name, uri, isFavorite)
                }
                .sortedBy { it.id }
        }
    }

    fun getFavorites(): Flow<List<SoundItem>> {
        return context.dataStore.data.map { prefs ->
            prefs.asMap()
                .filterKeys { it.name.startsWith(FAVORITE_PREFIX) }
                .mapNotNull { (key, value) ->
                    val id = key.name.removePrefix(FAVORITE_PREFIX).toIntOrNull() ?: return@mapNotNull null
                    val (name, uri) = value.toString().split("|", limit = 2)
                    SoundItem(id, name, uri, true)
                }
                .sortedBy { it.id }
        }
    }
}