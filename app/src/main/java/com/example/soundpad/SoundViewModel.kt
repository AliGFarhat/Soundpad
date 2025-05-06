package com.example.soundpad

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SoundViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SoundRepository(application)

    private val _selectedSound = MutableLiveData<SoundItem?>()
    val selectedSound: LiveData<SoundItem?> = _selectedSound

    private val _playingSound = MutableLiveData<SoundItem?>()
    val playingSound: LiveData<SoundItem?> = _playingSound

    private val _soundToAdd = MutableLiveData<Int?>()
    val soundToAdd: LiveData<Int?> = _soundToAdd

    val sounds = repository.getSounds().asLiveData()
    val favorites = repository.getFavorites().asLiveData()

    fun selectSound(sound: SoundItem) {
        _selectedSound.value = sound
        _playingSound.value = sound
    }

    fun stopPlayingSound() {
        _playingSound.value = null
    }

    fun prepareToAddSound(position: Int) {
        _soundToAdd.value = position
    }

    fun addSound(position: Int, name: String, uri: Uri) {
        viewModelScope.launch {
            repository.saveSound(SoundItem(position, name, uri.toString()))
            _soundToAdd.value = null
        }
    }

    fun toggleFavorite(sound: SoundItem) {
        viewModelScope.launch {
            repository.saveSound(sound)
        }
    }
}