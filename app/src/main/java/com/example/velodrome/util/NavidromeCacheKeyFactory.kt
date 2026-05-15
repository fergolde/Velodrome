package com.example.velodrome.util

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheKeyFactory

@UnstableApi
class NavidromeCacheKeyFactory : CacheKeyFactory {
    override fun buildCacheKey(dataSpec: DataSpec): String {
        val uri = dataSpec.uri
        // Intentar extraer el parámetro "id" de la URL de Subsonic
        val trackId = uri.getQueryParameter("id")

        // Si existe el ID, la clave será "track_ID", si no, usamos la URL base sin query params
        return if (!trackId.isNullOrBlank()) {
            "navidrome_track_$trackId"
        } else {
            uri.buildUpon().clearQuery().build().toString()
        }
    }
}