package com.example.soundpad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView

class FavoritesAdapter(
    var sounds: List<SoundItem>,
    private val onItemClick: (SoundItem) -> Unit,
    private val onFavoriteClick: (SoundItem) -> Unit,
    private var playingSound: SoundItem? = null
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    private val buttonColor = android.graphics.Color.parseColor("#c80923")
    private val grayColor = android.graphics.Color.parseColor("#757575")

    fun updatePlayingSound(sound: SoundItem?) {
        this.playingSound = sound
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val soundButton: Button = view.findViewById(R.id.soundButton)
        val favoriteButton: ImageButton = view.findViewById(R.id.favoriteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_sound, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sound = sounds[position]
        holder.soundButton.text = sound.name

        val isPlaying = playingSound != null && playingSound?.id == sound.id
        if (isPlaying) {
            holder.soundButton.setBackgroundColor(grayColor)
        } else {
            holder.soundButton.setBackgroundColor(buttonColor)
        }
        
        holder.soundButton.setOnClickListener { onItemClick(sound) }

        if (sound.isFavorite) {
            holder.favoriteButton.setImageResource(R.drawable.ic_star_filled)
        } else {
            holder.favoriteButton.setImageResource(R.drawable.ic_star_hollow)
        }
        holder.favoriteButton.setOnClickListener {

            val newFavoriteStatus = !sound.isFavorite
            onFavoriteClick(sound.copy(isFavorite = newFavoriteStatus))
            holder.favoriteButton.setImageResource(
                if (newFavoriteStatus) R.drawable.ic_star_filled else R.drawable.ic_star_hollow
            )
        }
    }

    override fun getItemCount() = sounds.size
}