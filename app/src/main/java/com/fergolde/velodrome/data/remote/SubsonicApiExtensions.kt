package com.fergolde.velodrome.data.remote

import com.fergolde.velodrome.data.remote.dto.ErrorDto
import com.fergolde.velodrome.data.remote.dto.SubsonicResponse

/**
 * Thrown when the Subsonic server responds with HTTP 200 but `status != "ok"`.
 * Carries the server error code/message so callers can surface it to the user.
 */
class SubsonicApiException(
    val error: ErrorDto
) : Exception(error.message)

/**
 * Verifies that the Subsonic response status is "ok".
 * Throws [SubsonicApiException] when the server reports a failure, so that
 * [runCatching] blocks in repositories treat it as a [Result.failure].
 */
fun SubsonicResponse.requireOk() {
    if (response.status != "ok") {
        throw SubsonicApiException(
            response.error ?: ErrorDto(code = 0, message = "Unknown Subsonic error")
        )
    }
}
