package com.example.velodrome.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface NavidromeApi {

    companion object {
        const val API_VERSION = "1.16.1"
        const val CLIENT_NAME = "Velodrome"
    }

    // Auth params (u, t, s, v, c) are added automatically by AuthInterceptor
    // No need to include them in method parameters

    @GET("rest/ping.view")
    suspend fun ping(): ResponseBody

    @GET("rest/getArtists.view")
    suspend fun getArtists(
        @Query("size") size: Int = 50,
        @Query("offset") offset: Int = 0
    ): ResponseBody

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(
        @Query("id") albumId: String
    ): ResponseBody

    @GET("rest/stream.view")
    suspend fun getStreamUrl(
        @Query("id") trackId: String,
        @Query("maxBitRate") maxBitRate: Int = 320
    ): String

    @GET("rest/search2.view")
    suspend fun search(
        @Query("query") query: String,
        @Query("size") size: Int = 20
    ): ResponseBody

    // Home screen APIs
    @GET("rest/getIndexes.view")
    suspend fun getIndexes(
        @Query("musicFolderId") musicFolderId: String? = null,
        @Query("ifModifiedSince") ifModifiedSince: Long? = null
    ): ResponseBody

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String, // "newest", "random", "alphabeticalByName", "alphabeticalByArtist", "starred", "frequent", "recent"
        @Query("size") size: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("genre") genre: String? = null
    ): ResponseBody

    @GET("rest/getGenres.view")
    suspend fun getGenres(): ResponseBody

    @GET("rest/getArtist.view")
    suspend fun getArtist(
        @Query("id") artistId: String
    ): ResponseBody
}