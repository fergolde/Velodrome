package com.fergolde.velodrome.data.remote

import com.fergolde.velodrome.data.remote.dto.SubsonicResponse
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubsonicResponseDtoTest {

    private val adapter = Moshi.Builder().build().adapter(SubsonicResponse::class.java)

    @Test
    fun `parses Navidrome server metadata attributes`() {
        val json = """
            {"subsonic-response":{"status":"ok","version":"1.16.1","type":"navidrome","serverVersion":"0.64.0","openSubsonic":true}}
        """.trimIndent()

        val dto = adapter.fromJson(json)!!.response

        assertEquals("1.16.1", dto.version)
        assertEquals("navidrome", dto.type)
        assertEquals("0.64.0", dto.serverVersion)
        assertEquals(true, dto.openSubsonic)
    }

    @Test
    fun `server metadata attributes are optional`() {
        val json = """{"subsonic-response":{"status":"ok"}}"""

        val dto = adapter.fromJson(json)!!.response

        assertEquals("ok", dto.status)
        assertNull(dto.version)
        assertNull(dto.type)
        assertNull(dto.serverVersion)
        assertNull(dto.openSubsonic)
    }
}
