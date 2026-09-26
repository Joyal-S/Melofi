package com.auralis.app

import com.auralis.app.data.local.scanner.MediaStoreAudioScanner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating Melofi Music Library filtering rules:
 * - Exclusion of non-music classifications (recordings, notifications, ringtones, alarms, podcasts, audiobooks)
 * - Exclusion of user-configured and default folders (Recordings, WhatsApp Audio, etc.)
 * - Exclusion of user-hidden tracks
 * - Preservation of explicit user imports in Music/Melofy/
 */
class LibraryFilteringTest {

    @Test
    fun standardMusicTrack_isEligible() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 100L,
            contentUri = "content://media/external/audio/media/100",
            rawPath = "/storage/emulated/0/Music/Artist/Album/song.mp3",
            rawRelativePath = "Music/Artist/Album/",
            isMusic = true
        )
        assertTrue("Standard music track should be included in library", eligible)
    }

    @Test
    fun nonMusicClassification_isExcluded() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 101L,
            contentUri = "content://media/external/audio/media/101",
            rawPath = "/storage/emulated/0/Download/clip.mp3",
            rawRelativePath = "Download/",
            isMusic = false
        )
        assertFalse("Audio with isMusic=false should be excluded", eligible)
    }

    @Test
    fun recordingClassification_isExcluded() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 102L,
            contentUri = "content://media/external/audio/media/102",
            rawPath = "/storage/emulated/0/Recordings/call_2024.m4a",
            rawRelativePath = "Recordings/",
            isMusic = true,
            isRecording = true
        )
        assertFalse("Voice recording should be excluded even if isMusic is true", eligible)
    }

    @Test
    fun notificationClassification_isExcluded() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 103L,
            contentUri = "content://media/external/audio/media/103",
            rawPath = "/storage/emulated/0/Notifications/ping.ogg",
            rawRelativePath = "Notifications/",
            isMusic = true,
            isNotification = true
        )
        assertFalse("Notification sound should be excluded", eligible)
    }

    @Test
    fun ringtoneClassification_isExcluded() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 104L,
            contentUri = "content://media/external/audio/media/104",
            rawPath = "/storage/emulated/0/Ringtones/chime.mp3",
            rawRelativePath = "Ringtones/",
            isMusic = true,
            isRingtone = true
        )
        assertFalse("Ringtone should be excluded", eligible)
    }

    @Test
    fun alarmClassification_isExcluded() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 105L,
            contentUri = "content://media/external/audio/media/105",
            rawPath = "/storage/emulated/0/Alarms/rooster.mp3",
            rawRelativePath = "Alarms/",
            isMusic = true,
            isAlarm = true
        )
        assertFalse("Alarm sound should be excluded", eligible)
    }

    @Test
    fun podcastClassification_isExcluded() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 106L,
            contentUri = "content://media/external/audio/media/106",
            rawPath = "/storage/emulated/0/Podcasts/episode1.mp3",
            rawRelativePath = "Podcasts/",
            isMusic = true,
            isPodcast = true
        )
        assertFalse("Podcast episode should be excluded", eligible)
    }

    @Test
    fun audiobookClassification_isExcluded() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 107L,
            contentUri = "content://media/external/audio/media/107",
            rawPath = "/storage/emulated/0/Audiobooks/chapter1.m4b",
            rawRelativePath = "Audiobooks/",
            isMusic = true,
            isAudiobook = true
        )
        assertFalse("Audiobook file should be excluded", eligible)
    }

    @Test
    fun excludedFolderPattern_pathMatches_isExcluded() {
        val excludedFolders = listOf("Recordings/", "Notifications/", "WhatsApp Audio/")

        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 108L,
            contentUri = "content://media/external/audio/media/108",
            rawPath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio/AUD-20240101-WA0001.opus",
            rawRelativePath = "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio/",
            isMusic = true,
            excludedFolderPatterns = excludedFolders
        )
        assertFalse("Audio in excluded WhatsApp folder should be excluded", eligible)
    }

    @Test
    fun excludedFolderPattern_unrelatedPath_isEligible() {
        val excludedFolders = listOf("Recordings/", "Notifications/", "WhatsApp Audio/")

        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 109L,
            contentUri = "content://media/external/audio/media/109",
            rawPath = "/storage/emulated/0/Music/Jazz/track.flac",
            rawRelativePath = "Music/Jazz/",
            isMusic = true,
            excludedFolderPatterns = excludedFolders
        )
        assertTrue("Track in non-excluded folder should be included", eligible)
    }

    @Test
    fun hiddenSongId_isExcluded() {
        val hiddenIds = setOf(200L, 201L)

        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 200L,
            contentUri = "content://media/external/audio/media/200",
            rawPath = "/storage/emulated/0/Music/song.mp3",
            rawRelativePath = "Music/",
            isMusic = true,
            hiddenSongIds = hiddenIds
        )
        assertFalse("User-hidden song ID should be excluded from library", eligible)
    }

    @Test
    fun hiddenContentUri_isExcluded() {
        val hiddenUris = setOf("content://media/external/audio/media/202")

        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 202L,
            contentUri = "content://media/external/audio/media/202",
            rawPath = "/storage/emulated/0/Music/song2.mp3",
            rawRelativePath = "Music/",
            isMusic = true,
            hiddenContentUris = hiddenUris
        )
        assertFalse("User-hidden content URI should be excluded from library", eligible)
    }

    @Test
    fun userImportInMusicMelofy_isPreservedEvenIfClassificationFlagsDiffer() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 300L,
            contentUri = "content://media/external/audio/media/300",
            rawPath = "/storage/emulated/0/Music/Melofy/custom_track.mp3",
            rawRelativePath = "Music/Melofy/",
            isMusic = false,
            isRecording = true,
            excludedFolderPatterns = listOf("Recordings/")
        )
        assertTrue("Deliberately user-imported track in Music/Melofy/ must be preserved", eligible)
    }

    @Test
    fun userImportInMusicMelofy_canStillBeHiddenExplicitly() {
        val eligible = MediaStoreAudioScanner.isEligibleAudioTrack(
            id = 301L,
            contentUri = "content://media/external/audio/media/301",
            rawPath = "/storage/emulated/0/Music/Melofy/hidden_import.mp3",
            rawRelativePath = "Music/Melofy/",
            isMusic = true,
            hiddenSongIds = setOf(301L)
        )
        assertFalse("User-imported track should be hidden if explicitly hidden by user", eligible)
    }
}
