package com.nextos.module.playermodule.util

import com.nextos.module.playermodule.media.MediaLibraryItem
import com.nextos.module.playermodule.media.MediaWrapper
import com.nextos.module.playermodule.media.Storage
import com.nextos.module.playermodule.service.PlaybackService
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import java.util.*
import kotlin.math.floor

object ModelsHelper {

    fun Long.lengthToCategory(): String {
        val value: Int
        if (this == 0L) return "-"
        if (this < 60000) return "< 1 min"
        if (this < 600000) {
            value = floor((this / 60000).toDouble()).toInt()
            return "$value - ${(value + 1)} min"
        }
        return if (this < 3600000) {
            value = (10 * floor((this / 600000).toDouble())).toInt()
            "$value - ${(value + 10)} min"
        } else {
            value = floor((this / 3600000).toDouble()).toInt()
            "$value - ${(value + 1)} h"
        }
    }

    object EmptyPBSCallback : PlaybackService.Callback {
        override fun update() {}
        override fun onMediaEvent(event: IMedia.Event) {}
        override fun onMediaPlayerEvent(event: MediaPlayer.Event) {}
    }

    interface RefreshModel {
        fun refresh()
    }

    val ascComp by lazy {
        Comparator<MediaLibraryItem> { item1, item2 ->
            if (item1?.itemType == MediaLibraryItem.TYPE_MEDIA) {
                val type1 = (item1 as MediaWrapper).type
                val type2 = (item2 as MediaWrapper).type
                if (type1 == MediaWrapper.TYPE_DIR && type2 != MediaWrapper.TYPE_DIR) return@Comparator -1
                else if (type1 != MediaWrapper.TYPE_DIR && type2 == MediaWrapper.TYPE_DIR) return@Comparator 1
            }
            item1?.title?.lowercase(Locale.getDefault())
                ?.compareTo(item2?.title?.lowercase(Locale.getDefault()) ?: "") ?: -1
        }
    }
    val descComp by lazy {
        Comparator<MediaLibraryItem> { item1, item2 ->
            if (item1?.itemType == MediaLibraryItem.TYPE_MEDIA) {
                val type1 = (item1 as MediaWrapper).type
                val type2 = (item2 as MediaWrapper).type
                if (type1 == MediaWrapper.TYPE_DIR && type2 != MediaWrapper.TYPE_DIR) return@Comparator -1
                else if (type1 != MediaWrapper.TYPE_DIR && type2 == MediaWrapper.TYPE_DIR) return@Comparator 1
            }
            item2?.title?.lowercase(Locale.getDefault())
                ?.compareTo(item1?.title?.lowercase(Locale.getDefault()) ?: "") ?: -1
        }
    }

    val tvAscComp by lazy {
        Comparator<MediaLibraryItem> { item1, item2 ->
            val type1 = (item1 as? MediaWrapper)?.type
            val type2 = (item2 as? MediaWrapper)?.type
            if (type1 == MediaWrapper.TYPE_DIR && type2 != MediaWrapper.TYPE_DIR) return@Comparator -1
            else if (type1 != MediaWrapper.TYPE_DIR && type2 == MediaWrapper.TYPE_DIR) return@Comparator 1
            item1?.title?.lowercase(Locale.getDefault())
                ?.compareTo(item2?.title?.lowercase(Locale.getDefault()) ?: "") ?: -1
        }
    }
    val tvDescComp by lazy {
        Comparator<MediaLibraryItem> { item1, item2 ->
            val type1 = (item1 as? MediaWrapper)?.type
            val type2 = (item2 as? MediaWrapper)?.type
            if (type1 == MediaWrapper.TYPE_DIR && type2 != MediaWrapper.TYPE_DIR) return@Comparator -1
            else if (type1 != MediaWrapper.TYPE_DIR && type2 == MediaWrapper.TYPE_DIR) return@Comparator 1
            item2?.title?.lowercase(Locale.getDefault())
                ?.compareTo(item1?.title?.lowercase(Locale.getDefault()) ?: "") ?: -1
        }
    }

    fun getFilenameAscComp(nbOfDigits: Int): Comparator<MediaLibraryItem> =
        Comparator<MediaLibraryItem> { item1, item2 ->
            val type1 = (item1 as? MediaWrapper)?.type
            val type2 = (item2 as? MediaWrapper)?.type
            if (type1 == MediaWrapper.TYPE_DIR && type2 != MediaWrapper.TYPE_DIR) return@Comparator -1
            else if (type1 != MediaWrapper.TYPE_DIR && type2 == MediaWrapper.TYPE_DIR) return@Comparator 1
            val filename1 = (item1 as? MediaWrapper)?.fileName ?: (item1 as? Storage)?.title
            val filename2 = (item2 as? MediaWrapper)?.fileName ?: (item2 as? Storage)?.title
            filename1?.lowercase(Locale.getDefault()).sanitizeStringForAlphaCompare(nbOfDigits)
                ?.compareTo(
                    filename2?.lowercase(Locale.getDefault())
                        .sanitizeStringForAlphaCompare(nbOfDigits)
                        ?: ""
                ) ?: -1
        }

    fun getFilenameDescComp(nbOfDigits: Int): Comparator<MediaLibraryItem> =
        Comparator<MediaLibraryItem> { item1, item2 ->
            val type1 = (item1 as? MediaWrapper)?.type
            val type2 = (item2 as? MediaWrapper)?.type
            if (type1 == MediaWrapper.TYPE_DIR && type2 != MediaWrapper.TYPE_DIR) return@Comparator -1
            else if (type1 != MediaWrapper.TYPE_DIR && type2 == MediaWrapper.TYPE_DIR) return@Comparator 1
            val filename1 = (item1 as? MediaWrapper)?.fileName ?: (item1 as? Storage)?.title
            val filename2 = (item2 as? MediaWrapper)?.fileName ?: (item2 as? Storage)?.title
            filename2?.lowercase(Locale.getDefault()).sanitizeStringForAlphaCompare(nbOfDigits)
                ?.compareTo(
                    filename1?.lowercase(Locale.getDefault())
                        .sanitizeStringForAlphaCompare(nbOfDigits)
                        ?: ""
                ) ?: -1
        }
}