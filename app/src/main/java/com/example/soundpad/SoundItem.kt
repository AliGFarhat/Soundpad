package com.example.soundpad

data class SoundItem(
    val id: Int,
    val name: String,
    val uri: String,
    val isFavorite: Boolean = false
)