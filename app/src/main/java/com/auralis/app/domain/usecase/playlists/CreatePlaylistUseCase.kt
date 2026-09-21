package com.auralis.app.domain.usecase.playlists

import com.auralis.app.domain.repository.PlaylistRepository
import com.auralis.app.domain.util.PlaylistValidator
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case to create a new playlist with validation.
 */
class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(name: String): Result<Long> {
        val existingNames = playlistRepository.getPlaylists().first().map { it.name }
        return when (val validation = PlaylistValidator.validate(name, existingNames)) {
            is PlaylistValidator.ValidationResult.Valid -> {
                playlistRepository.createPlaylist(validation.sanitizedName)
            }
            is PlaylistValidator.ValidationResult.Invalid -> {
                Result.failure(IllegalArgumentException(validation.reason))
            }
        }
    }
}
