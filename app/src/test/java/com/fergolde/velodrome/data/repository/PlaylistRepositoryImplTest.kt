package com.fergolde.velodrome.data.repository

import com.fergolde.velodrome.data.remote.NavidromeApi
import com.fergolde.velodrome.data.remote.dto.ErrorDto
import com.fergolde.velodrome.data.remote.dto.PlaylistDetailDto
import com.fergolde.velodrome.data.remote.dto.SubsonicResponse
import com.fergolde.velodrome.data.remote.dto.SubsonicResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistRepositoryImplTest {

    private val api: NavidromeApi = mockk()
    private val repository = PlaylistRepositoryImpl(api)

    @Test
    fun getPlaylists_subsonicFailed_returnsFailure() = runTest {
        val failed = SubsonicResponse(
            SubsonicResponseDto(status = "failed", error = ErrorDto(code = 40, message = "Token expired"))
        )
        coEvery { api.getPlaylists() } returns failed

        val result = repository.getPlaylists()

        assertTrue(result.isFailure)
    }

    @Test
    fun getPlaylist_subsonicFailed_returnsFailure() = runTest {
        val failed = SubsonicResponse(
            SubsonicResponseDto(status = "failed", error = ErrorDto(code = 40, message = "Token expired"))
        )
        coEvery { api.getPlaylist("1") } returns failed

        val result = repository.getPlaylist("1")

        assertTrue(result.isFailure)
    }
}
