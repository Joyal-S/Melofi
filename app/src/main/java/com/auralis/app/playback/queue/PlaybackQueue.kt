package com.auralis.app.playback.queue

import com.auralis.app.domain.model.Song
import com.auralis.app.playback.model.PlaybackRepeatMode

/**
 * Thread-safe in-memory queue abstraction for active playback sessions.
 */
class PlaybackQueue {

    private val items = mutableListOf<Song>()
    private var currentIndex: Int = -1

    @Synchronized
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        items.clear()
        items.addAll(songs)
        currentIndex = if (items.isNotEmpty()) {
            startIndex.coerceIn(0, items.lastIndex)
        } else {
            -1
        }
    }

    @Synchronized
    fun addToQueue(song: Song) {
        items.add(song)
        if (currentIndex == -1 && items.isNotEmpty()) {
            currentIndex = 0
        }
    }

    @Synchronized
    fun addToQueue(songs: List<Song>) {
        items.addAll(songs)
        if (currentIndex == -1 && items.isNotEmpty()) {
            currentIndex = 0
        }
    }

    @Synchronized
    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        val insertIndex = if (currentIndex in items.indices) currentIndex + 1 else items.size
        items.addAll(insertIndex, songs)
        if (currentIndex == -1 && items.isNotEmpty()) {
            currentIndex = 0
        }
    }

    @Synchronized
    fun playNext(song: Song) {
        playNext(listOf(song))
    }

    @Synchronized
    fun hasNext(repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF): Boolean {
        if (items.isEmpty()) return false
        return when (repeatMode) {
            PlaybackRepeatMode.ONE -> true
            PlaybackRepeatMode.ALL -> true
            PlaybackRepeatMode.OFF -> currentIndex + 1 < items.size
        }
    }

    @Synchronized
    fun removeFromQueue(index: Int): Boolean {
        if (index !in items.indices) return false
        items.removeAt(index)
        when {
            items.isEmpty() -> currentIndex = -1
            currentIndex >= items.size -> currentIndex = items.lastIndex
            index < currentIndex -> currentIndex--
        }
        return true
    }

    @Synchronized
    fun clearQueue() {
        items.clear()
        currentIndex = -1
    }

    @Synchronized
    fun currentSong(): Song? {
        return if (currentIndex in items.indices) items[currentIndex] else null
    }

    @Synchronized
    fun currentIndex(): Int = currentIndex

    @Synchronized
    fun setCurrentIndex(index: Int) {
        currentIndex = if (items.isNotEmpty()) {
            index.coerceIn(0, items.lastIndex)
        } else {
            -1
        }
    }

    @Synchronized
    fun nextSong(repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF): Song? {
        if (items.isEmpty()) return null
        if (repeatMode == PlaybackRepeatMode.ONE) return currentSong()

        return if (currentIndex + 1 < items.size) {
            currentIndex++
            items[currentIndex]
        } else if (repeatMode == PlaybackRepeatMode.ALL) {
            currentIndex = 0
            items[0]
        } else {
            null
        }
    }

    @Synchronized
    fun previousSong(repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF): Song? {
        if (items.isEmpty()) return null
        if (repeatMode == PlaybackRepeatMode.ONE) return currentSong()

        return if (currentIndex > 0) {
            currentIndex--
            items[currentIndex]
        } else if (repeatMode == PlaybackRepeatMode.ALL) {
            currentIndex = items.lastIndex
            items[currentIndex]
        } else {
            items[0]
        }
    }

    @Synchronized
    fun getSongs(): List<Song> = items.toList()

    @Synchronized
    fun size(): Int = items.size

    @Synchronized
    fun isEmpty(): Boolean = items.isEmpty()

    @Synchronized
    fun findSongById(id: Long): Song? = items.firstOrNull { it.id == id }

    @Synchronized
    fun indexOfSongId(id: Long): Int = items.indexOfFirst { it.id == id }
}
