package com.example.velodrome.domain.usecase

import com.example.velodrome.domain.repository.ScrobbleRepository
import javax.inject.Inject

class ScrobbleUseCase @Inject constructor(
    private val repository: ScrobbleRepository
) {
    suspend operator fun invoke(trackId: String, time: Long? = null, submission: Boolean = true) {
        repository.scrobble(trackId, time, submission)
    }
}