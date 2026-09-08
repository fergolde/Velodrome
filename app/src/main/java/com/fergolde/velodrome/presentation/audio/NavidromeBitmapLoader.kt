package com.fergolde.velodrome.presentation.audio

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.BitmapImage
import coil3.executeBlocking
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import javax.inject.Inject

/**
 * BitmapLoader for Media3 notifications that loads artwork URIs through Coil.
 *
 * [AudioPlayerService] stores an auth-less cover-art URI in [MediaMetadata.artworkUri]
 * (no rotating token/salt). This loader delegates to the app's singleton Coil
 * [ImageLoader], whose [com.fergolde.velodrome.util.NavidromeImageInterceptor]
 * injects authentication on the fly. That keeps credentials out of the metadata
 * while still allowing the system notification to fetch the image.
 */
@UnstableApi
class NavidromeBitmapLoader @Inject constructor(
    private val context: Context
) : BitmapLoader {

    override fun supportsMimeType(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        future.setException(UnsupportedOperationException("decodeBitmap is not supported"))
        return future
    }

    @WorkerThread
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        val request = ImageRequest.Builder(context)
            .data(uri.toString())
            .build()

        val result = context.imageLoader.executeBlocking(request)
        when (result) {
            is SuccessResult -> {
                val bitmap = (result.image as? BitmapImage)?.bitmap
                if (bitmap != null) {
                    future.set(bitmap)
                } else {
                    future.setException(IllegalStateException("Loaded image is not a bitmap"))
                }
            }
            is ErrorResult -> future.setException(result.throwable)
        }
        return future
    }
}
