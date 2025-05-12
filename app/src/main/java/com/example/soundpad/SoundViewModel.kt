package com.example.soundpad

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// Manages the sound data for the UI.
class SoundViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SoundRepository(application)

    // The LiveData for the currently selected sound.
    private val _selectedSound = MutableLiveData<SoundItem?>()
    val selectedSound: LiveData<SoundItem?> = _selectedSound

    // LiveData for the sound currently being played.
    private val _playingSound = MutableLiveData<SoundItem?>()
    val playingSound: LiveData<SoundItem?> = _playingSound

    // LiveData for the position where a new sound should be added after picking one.
    private val _soundToAdd = MutableLiveData<Int?>()
    val soundToAdd: LiveData<Int?> = _soundToAdd

    // LiveData for the list of all sounds that exist on the soundpad.
    val sounds = repository.getSounds().asLiveData()
    // LiveData for the list of favorite sounds.
    val favorites = repository.getFavorites().asLiveData()

    // Selects a sound (after user clicks on sound) and sets it as currently playing.
    fun selectSound(sound: SoundItem) {
        _selectedSound.value = sound
        _playingSound.value = sound
    }

    // Stops the currently playing sound.
    fun stopPlayingSound() {
        _playingSound.value = null
    }

    // Prepares to add a new sound at a specific position.
    fun prepareToAddSound(position: Int) {
        _soundToAdd.value = position
    }

    // Adds a new sound with the given details. (Such as sound directory on the users device).
    fun addSound(position: Int, name: String, uri: Uri) {
        viewModelScope.launch {
            repository.saveSound(SoundItem(position, name, uri.toString()))
            _soundToAdd.value = null
        }
    }

    // Toggles the favorite status on a sound.
    fun toggleFavorite(sound: SoundItem) {
        viewModelScope.launch {
            repository.saveSound(sound)
        }
    }
}