package com.auralis.app.domain.usecase.playlists

import com.auralis.app.domain.repository.PlaylistRepository
import com.auralis.app.domain.util.PlaylistValidator
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case to rename an existing playlist with validation.
 */
class RenamePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(id: Long, name: String): Result<Unit> {
        val existingPlaylists = playlistRepository.getPlaylists().first()
        val otherNames = existingPlaylists.filter { it.id != id }.map { it.name }

        return when (val validation = PlaylistValidator.validate(name, otherNames)) {
            is PlaylistValidator.ValidationResult.Valid -> {
                playlistRepository.renamePlaylist(id, validation.sanitizedName)
            }
            is PlaylistValidator.ValidationResult.Invalid -> {
                Result.failure(IllegalArgumentException(validation.reason))
            }
        }
    }
}
