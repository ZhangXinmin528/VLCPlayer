package com.nextos.module.playermodule.media

import android.net.Uri
import org.videolan.libvlc.interfaces.IMedia

data class MediaItem(var url:String, var type:Int=0, var uri: Uri?) {
    companion object {
        const val TYPE_VIDEO = 0
        const val TYPE_AUDIO = 1
    }
    var location = ""
        get() = url?:uri.toString()
}