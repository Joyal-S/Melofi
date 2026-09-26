package com.auralis.app.domain.model

/**
 * Thrown or returned when an artist creation or rename attempt conflicts
 * with an existing artist identity having the same normalized name.
 */
class DuplicateArtistException(
    val existingArtist: Artist,
    message: String = "An artist named '${existingArtist.name}' already exists."
) : IllegalArgumentException(message)
