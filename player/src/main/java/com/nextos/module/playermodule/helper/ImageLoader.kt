package com.nextos.module.playermodule.helper

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build

import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.appcompat.content.res.AppCompatResources

import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.nextos.module.playermodule.media.MediaLibraryItem
import com.nextos.module.playermodule.media.MediaWrapper
import com.nextos.module.playermodule.resources.HEADER_MOVIES
import com.nextos.module.playermodule.resources.HEADER_TV_SHOW


@Volatile
private var defaultImageWidth = 0
private var defaultImageWidthTV = 0
private const val TAG = "ImageLoader"


fun getAudioIconDrawable(context: Context?, type: Int, big: Boolean = false): BitmapDrawable? =
    context?.let {
        when (type) {
            MediaLibraryItem.TYPE_ALBUM -> if (big) UiTools.getDefaultAlbumDrawableBig(it) else UiTools.getDefaultAlbumDrawable(
                it
            )

            MediaLibraryItem.TYPE_ARTIST -> if (big) UiTools.getDefaultArtistDrawableBig(it) else UiTools.getDefaultArtistDrawable(
                it
            )

            MediaLibraryItem.TYPE_MEDIA -> if (big) UiTools.getDefaultAudioDrawableBig(it) else UiTools.getDefaultAudioDrawable(
                it
            )

            else -> null
        }
    }

fun getMediaIconDrawable(context: Context?, type: Int, big: Boolean = false): BitmapDrawable? =
    context?.let {
        when (type) {
            MediaWrapper.TYPE_ALBUM -> if (big) UiTools.getDefaultAlbumDrawableBig(it) else UiTools.getDefaultAlbumDrawable(
                it
            )

            MediaWrapper.TYPE_ARTIST -> if (big) UiTools.getDefaultArtistDrawableBig(it) else UiTools.getDefaultArtistDrawable(
                it
            )

            MediaWrapper.TYPE_AUDIO -> if (big) UiTools.getDefaultAudioDrawableBig(it) else UiTools.getDefaultAudioDrawable(
                it
            )

            MediaWrapper.TYPE_VIDEO -> if (big) UiTools.getDefaultVideoDrawableBig(it) else UiTools.getDefaultAudioDrawable(
                it
            )

            MediaWrapper.TYPE_DIR -> if (big) UiTools.getDefaultFolderDrawableBig(it) else UiTools.getDefaultFolderDrawable(
                it
            )

            else -> null
        }
    }

fun getMoviepediaIconDrawable(
    context: Context?, type: Long, big: Boolean = false
): BitmapDrawable? = context?.let {
    when (type) {
        HEADER_MOVIES -> if (big) UiTools.getDefaultMovieDrawableBig(it) else UiTools.getDefaultMovieDrawable(
            it
        )

        HEADER_TV_SHOW -> if (big) UiTools.getDefaultTvshowDrawableBig(it) else UiTools.getDefaultTvshowDrawable(
            it
        )

        else -> null
    }
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
fun getBitmapFromDrawable(context: Context, @DrawableRes drawableId: Int): Bitmap {
    val drawable = AppCompatResources.getDrawable(context, drawableId)

    return if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else if (drawable is VectorDrawableCompat || drawable is VectorDrawable) {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        bitmap
    } else {
        throw IllegalArgumentException("unsupported drawable type")
    }
}

fun getMediaIconDrawable(context: Context, type: Int): BitmapDrawable? = when (type) {
    MediaWrapper.TYPE_VIDEO -> UiTools.getDefaultVideoDrawable(context)
    else -> UiTools.getDefaultAudioDrawable(context)
}
