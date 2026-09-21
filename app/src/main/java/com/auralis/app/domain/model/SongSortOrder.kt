package com.auralis.app.domain.model

/**
 * Sort options for the song library.
 */
enum class SongSortOrder(val displayName: String) {
    TITLE("Title (A → Z)"),
    ARTIST("Artist (A → Z)"),
    ALBUM("Album (A → Z)"),
    DATE_ADDED("Recently Added"),
    TITLE_DESC("Title (Z → A)"),
    ARTIST_DESC("Artist (Z → A)"),
    DATE_MODIFIED("Recently Modified"),
    DURATION_ASC("Duration (Shortest)"),
    DURATION_DESC("Duration (Longest)"),
    FILE_SIZE_ASC("File Size (Smallest)"),
    FILE_SIZE_DESC("File Size (Largest)"),
    BITRATE_DESC("Bitrate (Highest)"),
    BITRATE_ASC("Bitrate (Lowest)"),
    MOST_PLAYED("Most Played"),
    RECENTLY_PLAYED("Recently Played")
}
