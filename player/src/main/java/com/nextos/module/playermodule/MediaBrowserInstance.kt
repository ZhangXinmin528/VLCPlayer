package com.nextos.module.playermodule

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import com.nextos.module.playermodule.MediaBrowserInstance.init
import com.nextos.module.playermodule.service.PlaybackService
import com.nextos.module.playermodule.tools.SingletonHolder

object MediaBrowserInstance :
    SingletonHolder<MediaBrowserCompat, Context>({ init(it.applicationContext) }) {

    private lateinit var mediaBrowser: MediaBrowserCompat

    fun init(context: Context): MediaBrowserCompat {
        return MediaBrowserCompat(
            context,
            ComponentName(context, PlaybackService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {}

                override fun onConnectionSuspended() {}

                override fun onConnectionFailed() {}
            }, null
        ).apply {
            connect()
        }
    }
}