package com.example.velodrome.data.remote

import com.example.velodrome.data.remote.dto.SubsonicResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NavidromeApi {

    companion object {
        const val API_VERSION = "1.16.1"
        const val CLIENT_NAME = "Velodrome"
    }

    // Auth params (u, t, s, v, c, f=json) are added automatically by AuthInterceptor

    @GET("rest/ping.view")
    suspend fun ping(): SubsonicResponse

    @GET("rest/getArtists.view")
    suspend fun getArtists(
        @Query("size") size: Int = 50,
        @Query("offset") offset: Int = 0
    ): SubsonicResponse

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(
        @Query("id") albumId: String
    ): SubsonicResponse

    @GET("rest/search2.view")
    suspend fun search(
        @Query("query") query: String,
        @Query("size") size: Int = 20
    ): SubsonicResponse

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("albumCount") albumCount: Int = 10,
        @Query("artistCount") artistCount: Int = 10,
        @Query("songCount") songCount: Int = 25
    ): SubsonicResponse

    // Home screen APIs
    @GET("rest/getIndexes.view")
    suspend fun getIndexes(
        @Query("musicFolderId") musicFolderId: String? = null,
        @Query("ifModifiedSince") ifModifiedSince: Long? = null
    ): SubsonicResponse

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String,
        @Query("size") size: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("genre") genre: String? = null
    ): SubsonicResponse

    @GET("rest/getGenres.view")
    suspend fun getGenres(): SubsonicResponse

    @GET("rest/getArtist.view")
    suspend fun getArtist(
        @Query("id") artistId: String
    ): SubsonicResponse

    @GET("rest/scrobble.view")
    suspend fun scrobble(
        @Query("id") trackId: String,
        @Query("time") time: Long? = null,
        @Query("submission") submission: Boolean = true
    ): SubsonicResponse

    // Genre-based song retrieval
    @GET("rest/getSongsByGenre.view")
    suspend fun getSongsByGenre(
        @Query("genre") genre: String,
        @Query("count") count: Int = 50,
        @Query("offset") offset: Int = 0
    ): SubsonicResponse

    @GET("rest/getRandomSongs.view")
    suspend fun getRandomSongs(
        @Query("size") size: Int = 50,
        @Query("genre") genre: String? = null
    ): SubsonicResponse

    @GET("rest/getMusicDirectory.view")
    suspend fun getMusicDirectory(
        @Query("id") id: String
    ): SubsonicResponse
}
