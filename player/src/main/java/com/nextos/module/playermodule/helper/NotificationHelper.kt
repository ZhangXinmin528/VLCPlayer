/*****************************************************************************
 * NotificationHelper.java
 *
 * Copyright © 2017 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package com.nextos.module.playermodule.helper


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.media.session.MediaButtonReceiver
import com.nextos.module.playermodule.R
import com.nextos.module.playermodule.resources.*
import com.nextos.module.playermodule.tools.DrawableCache
import com.nextos.module.playermodule.tools.Settings
import com.nextos.module.playermodule.tools.hasFlag
import com.nextos.module.playermodule.util.AndroidDevices
import com.nextos.module.playermodule.util.TextUtils
import org.videolan.libvlc.util.AndroidUtil
import kotlin.math.abs

private const val MEDIALIBRRARY_CHANNEL_ID = "vlc_medialibrary"
private const val PLAYBACK_SERVICE_CHANNEL_ID = "vlc_playback"
const val MISC_CHANNEL_ID = "misc"
private const val RECOMMENDATION_CHANNEL_ID = "vlc_recommendations"

object NotificationHelper {
    const val TAG = "VLC/NotificationHelper"
    const val VLC_DEBUG_CHANNEL = "vlc_debug"

    private val notificationIntent = Intent()

    fun createPlaybackNotification(ctx: Context, video: Boolean, title: String, artist: String,
                                   album: String, cover: Bitmap?, playing: Boolean, pausable: Boolean,
                                   seekable: Boolean, speed: Float, podcastMode: Boolean,
                                   seekInCompactView: Boolean, enabledActions: Long,
                                   sessionToken: MediaSessionCompat.Token?,
                                   spi: PendingIntent?): Notification {

        Log.d(TAG, "createPlaybackNotification: ")

        val piStop = MediaButtonReceiver.buildMediaButtonPendingIntent(ctx, PlaybackStateCompat.ACTION_STOP)
        val builder = NotificationCompat.Builder(ctx, PLAYBACK_SERVICE_CHANNEL_ID)
        builder.setSmallIcon(if (video) R.mipmap.player_ic_notif_video else R.mipmap.player_ic_notif_audio)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(title)
                .setContentText(TextUtils.separatedString('-', artist, album))
                .setLargeIcon(cover)
                .setTicker(TextUtils.separatedString('-', title, artist))
                .setAutoCancel(!playing)
                .setOngoing(playing)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setDeleteIntent(piStop)
                .setColor(Color.BLACK)
        spi?.let { builder.setContentIntent(it) }
        /* Previous */
        if (podcastMode) {
            val speedIcons = hashMapOf(
                    0.50f to R.drawable.ic_notif_speed_0_50,
                    0.80f to R.drawable.ic_notif_speed_0_80,
                    1.00f to R.drawable.ic_notif_speed_1_00,
                    1.10f to R.drawable.ic_notif_speed_1_10,
                    1.20f to R.drawable.ic_notif_speed_1_20,
                    1.50f to R.drawable.ic_notif_speed_1_50,
                    2.00f to R.drawable.ic_notif_speed_2_00
            )
            val speedResId = speedIcons[speedIcons.keys.minByOrNull { abs(speed - it) }] ?: R.drawable.ic_notif_speed_1_00
            builder.addAction(NotificationCompat.Action(speedResId, "回放速度",
                    buildCustomButtonPendingIntent(ctx, CUSTOM_ACTION_SPEED)
            ))
        } else {
            builder.addAction(NotificationCompat.Action(R.drawable.player_ic_notif_previous, ctx.getString(R.string.player_previous),
                    buildMediaButtonPendingIntent(ctx, enabledActions, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
        }
        /* Rewind */
        builder.addAction(NotificationCompat.Action(
                DrawableCache.getDrawableFromMemCache(ctx, "ic_notif_rewind_${Settings.audioJumpDelay}", R.drawable.player_ic_notif_rewind),
                ctx.getString(R.string.player_playback_rewind),
                buildMediaButtonPendingIntent(ctx, enabledActions, PlaybackStateCompat.ACTION_REWIND, playing)))
        /* Play/Pause or Stop */
        if (pausable) {
            if (playing) builder.addAction(NotificationCompat.Action(R.drawable.player_ic_widget_pause_w, "暂停",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(ctx, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
            else builder.addAction(NotificationCompat.Action(R.drawable.player_ic_widget_play_w, "播放",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(ctx, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
        } else builder.addAction(NotificationCompat.Action(R.drawable.player_ic_widget_close_w, "停止", piStop))
        /* Fast Forward */
        builder.addAction(NotificationCompat.Action(
                DrawableCache.getDrawableFromMemCache(ctx, "ic_notif_forward_${Settings.audioJumpDelay}", R.drawable.player_ic_notif_forward),
                "快进",
                buildMediaButtonPendingIntent(ctx, enabledActions, PlaybackStateCompat.ACTION_FAST_FORWARD, playing)))
        /* Next */
        builder.addAction(NotificationCompat.Action(R.drawable.player_ic_notif_next, "下一首",
            buildMediaButtonPendingIntent(ctx, enabledActions, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
        if (AndroidDevices.showMediaStyle) {
            val showActions = if (podcastMode || (seekable && seekInCompactView)) intArrayOf(1, 2, 3) else intArrayOf(0, 2, 4)
            val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(*showActions)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(piStop)
            sessionToken?.let { mediaStyle.setMediaSession(it) }
            builder.setStyle(mediaStyle)
        }
        return builder.build()
    }

    private fun buildMediaButtonPendingIntent(ctx: Context, enabledActions: Long, action: Long, allowIntent: Boolean = true): PendingIntent? {
        Log.d(TAG, "buildMediaButtonPendingIntent: ")
        return if (allowIntent && enabledActions.hasFlag(action))
            MediaButtonReceiver.buildMediaButtonPendingIntent(ctx, action)
        else null
    }

    private fun buildCustomButtonPendingIntent(ctx: Context, actionId: String): PendingIntent {
        Log.d(TAG, "buildCustomButtonPendingIntent: ")
        val intent = Intent(CUSTOM_ACTION)
        intent.putExtra(EXTRA_CUSTOM_ACTION_ID, actionId)
        return PendingIntent.getBroadcast(ctx, actionId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    fun createNotificationChannels(appCtx: Context) {
        Log.d(TAG, "createNotificationChannels: ")
        if (!AndroidUtil.isOOrLater) return
        val notificationManager = appCtx.getSystemService<NotificationManager>()!!
        val channels = mutableListOf<NotificationChannel>()
        // Playback channel
        if (notificationManager.getNotificationChannel(PLAYBACK_SERVICE_CHANNEL_ID) == null ) {
            val name: CharSequence = appCtx.getString(R.string.player_playback)
            val description = appCtx.getString(R.string.player_playback_controls)
            val channel = NotificationChannel(PLAYBACK_SERVICE_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            channel.description = description
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channels.add(channel)
        }
        // Scan channel
        if (notificationManager.getNotificationChannel(MEDIALIBRRARY_CHANNEL_ID) == null ) {
            val name = appCtx.getString(R.string.player_medialibrary_scan)
            val description = appCtx.getString(R.string.player_Medialibrary_progress)
            val channel = NotificationChannel(MEDIALIBRRARY_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            channel.description = description
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channels.add(channel)
        }

        // Misc channel
        if (notificationManager.getNotificationChannel(MISC_CHANNEL_ID) == null ) {
            val name = appCtx.getString(R.string.player_misc)
            val channel = NotificationChannel(MISC_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channels.add(channel)
        }

        // Recommendations channel
        if (AndroidDevices.isAndroidTv && notificationManager.getNotificationChannel(RECOMMENDATION_CHANNEL_ID) == null) {
            val name = appCtx.getString(R.string.player_recommendations)
            val description = appCtx.getString(R.string.player_recommendations_desc)
            val channel = NotificationChannel(RECOMMENDATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
            channel.description = description
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channels.add(channel)
        }
        if (channels.isNotEmpty()) notificationManager.createNotificationChannels(channels)
    }

}
