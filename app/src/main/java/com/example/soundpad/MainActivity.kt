package com.example.soundpad

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soundpad.databinding.ActivityMainBinding
import androidx.core.net.toUri
import com.example.soundpad.databinding.SoundButtonLayoutBinding
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: SoundViewModel by viewModels()
    private var mediaPlayer: MediaPlayer? = null
    private val buttonColor = android.graphics.Color.parseColor("#c80923") // Original state of the button (color)
    private val grayColor = android.graphics.Color.parseColor("#757575") // Gray color button when its playing a sound

    private val soundPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            // Will take consistent permission on URI even if the app is reopened.
            try {
                // Only use read permission
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // popup (toast)
                Toast.makeText(this, "Unable to save persistent access to this sound", Toast.LENGTH_SHORT).show()
            }
            
            val position = viewModel.soundToAdd.value ?: return@let
            val name = getFileName(uri) ?: "Sound ${position + 1}"
            viewModel.addSound(position, name, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSoundGrid()
        setupNavigation()
        setupObservers()
        setupFavoritesRecyclerView()
        setupStopButtons()
    }

    private fun setupStopButtons() {
        // Set the stop buttons to the same color as the sound buttons.
        binding.stopButton.setBackgroundColor(buttonColor)
        binding.stopButtonFavorites.setBackgroundColor(buttonColor)
        
        binding.stopButton.setOnClickListener {
            stopPlayingSound()
        }
        
        binding.stopButtonFavorites.setOnClickListener {
            stopPlayingSound()
        }
    }
    
    private fun stopPlayingSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset()
        }
        viewModel.stopPlayingSound() // Notify the ViewModel that the sound has stopped playing.
    }

    private fun setupSoundGrid() {
        val buttonContainers = listOf(
            binding.btn00, binding.btn01, binding.btn02, binding.btn03, binding.btn04,
            binding.btn10, binding.btn11, binding.btn12, binding.btn13, binding.btn14,
            binding.btn20, binding.btn21, binding.btn22, binding.btn23, binding.btn24,
            binding.btn30, binding.btn31, binding.btn32, binding.btn33, binding.btn34
        )
        
        val currentlyPlayingSound = viewModel.playingSound.value

        // First reset all buttons to default state
        buttonContainers.forEachIndexed { index, container ->
            val soundButton = container.root.findViewById<Button>(R.id.soundButton)
            val favoriteButton = container.root.findViewById<ImageButton>(R.id.favoriteButton)
            
            // Set default background color of buttons to red
            soundButton.setBackgroundColor(buttonColor)
            
            soundButton.setOnClickListener {
                viewModel.prepareToAddSound(index)
                soundPicker.launch(arrayOf("audio/*"))
            }
            soundButton.setOnLongClickListener(null)
            soundButton.text = "Tap to add sound"
            
            // Hide favorite button for buttons without sounds added yet.
            favoriteButton.visibility = View.GONE
            favoriteButton.setOnClickListener(null)
        }

        // Updates buttons with assigned sounds
        viewModel.sounds.value?.forEach { sound ->
            if (sound.id < buttonContainers.size) {
                val container = buttonContainers[sound.id]
                val soundButton = container.root.findViewById<Button>(R.id.soundButton)
                val favoriteButton = container.root.findViewById<ImageButton>(R.id.favoriteButton)
                
                soundButton.text = sound.name
                
                // Set gray background if this is the currently playing sound if not, make it red.
                val isPlaying = currentlyPlayingSound != null && currentlyPlayingSound.id == sound.id
                if (isPlaying) {
                    soundButton.setBackgroundColor(grayColor)
                } else {
                    soundButton.setBackgroundColor(buttonColor)
                }
                
                // Click = Play Sound via Click Listener.
                soundButton.setOnClickListener {
                    viewModel.selectSound(sound)
                }
                
                // Long press to replace the sound
                soundButton.setOnLongClickListener {
                    Toast.makeText(this, "Replacing sound: ${sound.name}", Toast.LENGTH_SHORT).show()
                    viewModel.prepareToAddSound(sound.id)
                    soundPicker.launch(arrayOf("audio/*"))
                    true // Return true to start the long click event
                }
                
                // Show and update favorite button
                favoriteButton.visibility = View.VISIBLE
                
                // This makes sure to set the star button to visible if a sound is set as favorite.
                updateFavoriteButtonAppearance(favoriteButton, sound.isFavorite)
                
                // On Click = Changes Favorite Status.
                favoriteButton.setOnClickListener {
                    val newFavoriteStatus = !sound.isFavorite
                    viewModel.toggleFavorite(sound.copy(isFavorite = newFavoriteStatus))
                    // Updates the Star to the other version of itself (filled or hollow) depending on state.
                    updateFavoriteButtonAppearance(favoriteButton, newFavoriteStatus)
                    Toast.makeText(this, 
                        if (newFavoriteStatus) "Added to favorites" else "Removed from favorites", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupNavigation() {
        // Sets the bottom navigation bar icons to match the buttons colors for consistency in aesthetics.
        val iconColor = android.content.res.ColorStateList.valueOf(buttonColor)
        binding.bottomNavigation.itemIconTintList = iconColor
        binding.bottomNavigation.itemTextColor = iconColor
        
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_soundboard -> {
                    binding.soundboardView.visibility = View.VISIBLE
                    binding.favoritesView.visibility = View.GONE
                    true
                }
                R.id.nav_favorites -> {
                    binding.soundboardView.visibility = View.GONE
                    binding.favoritesView.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun setupObservers() {
        viewModel.sounds.observe(this, Observer { sounds ->
            updateSoundButtons(sounds)
        })

        viewModel.selectedSound.observe(this, Observer { sound ->
            sound?.let { playSound(it) }
        })
        
        viewModel.playingSound.observe(this, Observer { sound ->
            // Updates the state of all the buttons at once to check which one is playing a sound.
            updateSoundButtons(viewModel.sounds.value ?: emptyList())
        })
    }

    private lateinit var favoritesAdapter: FavoritesAdapter

    @SuppressLint("NotifyDataSetChanged")
    private fun setupFavoritesRecyclerView() {
        favoritesAdapter = FavoritesAdapter(
            emptyList(),
            onItemClick = { sound ->
                viewModel.selectSound(sound)
            },
            onFavoriteClick = { sound ->
                viewModel.toggleFavorite(sound)
            },
            playingSound = null // Sets so no sounds play initially.
        )

        binding.favoritesRecyclerView.apply {
            // Attemped to make a 2 column grid the same as the original view (Soundpad View).
            val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(this@MainActivity, 2)
            layoutManager = gridLayoutManager

            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    if (position == RecyclerView.NO_POSITION) return
                    
                    val column = position % 2
                    
                    // Apply 4dp horizontal spacing between items on the inner sides of the buttons.
                    outRect.left = if (column == 0) 0 else 4
                    outRect.right = if (column == 1) 0 else 4
                    
                    // Apply 8dp bottom margin to all buttons.
                    outRect.top = 0
                    outRect.bottom = 8
                }
            })
            
            adapter = favoritesAdapter
        }

        viewModel.favorites.observe(this, Observer { favorites ->
            favoritesAdapter.sounds = favorites
            favoritesAdapter.notifyDataSetChanged()
        })
        
        viewModel.playingSound.observe(this, Observer { playingSound ->
            favoritesAdapter.updatePlayingSound(playingSound)
        })
    }

    private fun updateSoundButtons(sounds: List<SoundItem>) {
        val buttonContainers = listOf(
            binding.btn00, binding.btn01, binding.btn02, binding.btn03, binding.btn04,
            binding.btn10, binding.btn11, binding.btn12, binding.btn13, binding.btn14,
            binding.btn20, binding.btn21, binding.btn22, binding.btn23, binding.btn24,
            binding.btn30, binding.btn31, binding.btn32, binding.btn33, binding.btn34
        )
        
        val currentlyPlayingSound = viewModel.playingSound.value

        // Reset all buttons to default state (Same as other view)
        buttonContainers.forEachIndexed { index, container ->
            val soundButton = container.root.findViewById<Button>(R.id.soundButton)
            val favoriteButton = container.root.findViewById<ImageButton>(R.id.favoriteButton)
            
            // Set default background color to red (Same as other view)
            soundButton.setBackgroundColor(buttonColor)
            
            soundButton.setOnClickListener {
                viewModel.prepareToAddSound(index)
                soundPicker.launch(arrayOf("audio/*"))
            }
            soundButton.setOnLongClickListener(null)
            soundButton.text = "Tap to add sound"
            
            // Hide favorite button for buttons without sounds (Same as other view).
            favoriteButton.visibility = View.GONE
            favoriteButton.setOnClickListener(null)
        }

        // Then update buttons with assigned sounds
        sounds.forEach { sound ->
            if (sound.id < buttonContainers.size) {
                val container = buttonContainers[sound.id]
                val soundButton = container.root.findViewById<Button>(R.id.soundButton)
                val favoriteButton = container.root.findViewById<ImageButton>(R.id.favoriteButton)
                
                soundButton.text = sound.name

                // Set gray background if this is the currently playing sound if not, make it red. (Same as other view)
                val isPlaying = currentlyPlayingSound != null && currentlyPlayingSound.id == sound.id
                if (isPlaying) {
                    soundButton.setBackgroundColor(grayColor)
                } else {
                    soundButton.setBackgroundColor(buttonColor)
                }
                
                // Click = Play Sound via OnClick Listener. (Same as other view)
                soundButton.setOnClickListener {
                    viewModel.selectSound(sound)
                }
                
                // Long press to replace the sound (Same as other view)
                soundButton.setOnLongClickListener {
                    Toast.makeText(this, "Replacing sound: ${sound.name}", Toast.LENGTH_SHORT).show()
                    viewModel.prepareToAddSound(sound.id)
                    soundPicker.launch(arrayOf("audio/*"))
                    true // Return true to start the long click event (Same as other view)
                }
                
                // Show and update favorite button - make sure it's visible (Same as other view)
                favoriteButton.visibility = View.VISIBLE
                
                // Make sure to update the star icon based on favorite status (Same as other view)
                updateFavoriteButtonAppearance(favoriteButton, sound.isFavorite)
                
                // Set click listener to toggle favorite status (Same as other view)
                favoriteButton.setOnClickListener {
                    val newFavoriteStatus = !sound.isFavorite
                    viewModel.toggleFavorite(sound.copy(isFavorite = newFavoriteStatus))
                    // Immediately update the appearance of the star (Same as other view)
                    updateFavoriteButtonAppearance(favoriteButton, newFavoriteStatus)
                    Toast.makeText(this, 
                        if (newFavoriteStatus) "Added to favorites" else "Removed from favorites", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun updateFavoriteButtonAppearance(favoriteButton: ImageButton, isFavorite: Boolean) {
        // Check the favorite button state to make sure the right icon is set. (Same as other view)
        favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_star_filled 
            else R.drawable.ic_star_hollow
        )
        
        // Make sure the favorite button is in front of other elements so its not hidden
        favoriteButton.bringToFront()
    }

    private fun playSound(sound: SoundItem) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer()
        
        try {
            val uri = sound.uri.toUri()
            mediaPlayer?.apply {
                setDataSource(this@MainActivity, uri)
                prepare()
                start()
                
                // When playback completes, clear the playing state (When audio ends reset state so button color resets)
                setOnCompletionListener {
                    viewModel.stopPlayingSound()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot play this sound. Long-press to replace it.", Toast.LENGTH_SHORT).show()
            viewModel.stopPlayingSound() // Clear playing state if there is an error.
            
            // If this sound is from a previous session and URI permission was lost then let the user know they can long-press to replace it.
            val buttonContainers = listOf(
                binding.btn00, binding.btn01, binding.btn02, binding.btn03, binding.btn04,
                binding.btn10, binding.btn11, binding.btn12, binding.btn13, binding.btn14,
                binding.btn20, binding.btn21, binding.btn22, binding.btn23, binding.btn24,
                binding.btn30, binding.btn31, binding.btn32, binding.btn33, binding.btn34
            )
            
            if (sound.id < buttonContainers.size) {
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return name?.substringBeforeLast('.')
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}