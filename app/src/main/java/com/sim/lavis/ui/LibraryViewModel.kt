package com.sim.lavis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sim.lavis.LavisApplication
import com.sim.lavis.data.MediaScanner
import com.sim.lavis.data.MusicRepository
import com.sim.lavis.data.db.SingerEntity
import com.sim.lavis.data.db.SingerRow
import com.sim.lavis.data.db.PlaylistRow
import com.sim.lavis.data.db.SongWithSingers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: MusicRepository,
    private val scanner: MediaScanner
) : ViewModel() {

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    val playlists: StateFlow<List<PlaylistRow>> = repository.songDao.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val singers: StateFlow<List<SingerRow>> = repository.singerDao.observeSingers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allSingers: StateFlow<List<SingerEntity>> = repository.singerDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playlistSongs(playlist: String): Flow<List<SongWithSingers>> =
        repository.songDao.observePlaylist(playlist)

    fun songsOfSinger(singerId: Long): Flow<List<SongWithSingers>> =
        repository.songDao.observeSongsOfSinger(singerId)

    fun scan() {
        viewModelScope.launch {
            _scanning.value = true
            try {
                scanner.sync()
            } finally {
                _scanning.value = false
            }
        }
    }

    fun createSinger(name: String) {
        viewModelScope.launch { repository.createSinger(name) }
    }

    fun createSingerAndAssign(name: String, songId: Long) {
        viewModelScope.launch {
            val id = repository.createSinger(name)
            if (id > 0) repository.assignSinger(songId, id)
        }
    }

    fun setSongTitle(songId: Long, title: String) {
        viewModelScope.launch { repository.setSongTitle(songId, title) }
    }

    fun toggleSinger(songId: Long, singerId: Long, assigned: Boolean) {
        viewModelScope.launch {
            if (assigned) repository.unassignSinger(songId, singerId)
            else repository.assignSinger(songId, singerId)
        }
    }

    fun deleteSinger(singerId: Long) {
        viewModelScope.launch { repository.deleteSinger(singerId) }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LavisApplication
                return LibraryViewModel(app.repository, app.mediaScanner) as T
            }
        }
    }
}
