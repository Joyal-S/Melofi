package com.auralis.app.domain.usecase

import com.auralis.app.domain.model.ScanResult
import com.auralis.app.domain.repository.SongRepository
import javax.inject.Inject

/**
 * Use case to trigger a library rescan against device MediaStore.
 */
class ScanLibraryUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(): Result<ScanResult> =
        songRepository.scanLibrary()
}
