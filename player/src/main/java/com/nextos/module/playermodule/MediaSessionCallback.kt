/**
 * **************************************************************************
 * MediaSessionCallback.kt
 * ****************************************************************************
 * Copyright © 2018 VLC authors and VideoLAN
 * Author: Geoffrey Métais
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
 * ***************************************************************************
 */
package com.nextos.module.playermodule

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import com.nextos.module.playermodule.media.MediaWrapper
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_FAST_FORWARD
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_REPEAT
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_REWIND
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_SHUFFLE
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_SPEED
import com.nextos.module.playermodule.resources.MEDIALIBRARY_PAGE_SIZE
import com.nextos.module.playermodule.service.PlaybackService
import com.nextos.module.playermodule.tools.Settings
import com.nextos.module.playermodule.util.AndroidDevices
import com.nextos.module.playermodule.util.parcelable
import java.security.SecureRandom
import kotlin.math.abs
import kotlin.math.min

private const val TAG = "VLC/MediaSessionCallback"
private const val ONE_SECOND = 1000L

internal class MediaSessionCallback(private val playbackService: PlaybackService) :
    MediaSessionCompat.Callback() {
    private var prevActionSeek = false

    override fun onPlay() {
        if (playbackService.hasMedia()) playbackService.play()
        else if (!AndroidDevices.isAndroidTv) PlaybackService.loadLastAudio(playbackService)
    }

    @SuppressLint("MissingPermission")
    override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
        val keyEvent =
            mediaButtonEvent.parcelable(Intent.EXTRA_KEY_EVENT) as KeyEvent? ?: return false

        if (playbackService.detectHeadset &&
            playbackService.settings.getBoolean("ignore_headset_media_button_presses", false)
        ) {
            // Wired headset
            if (playbackService.headsetInserted && isWiredHeadsetHardKey(keyEvent)) {
                return true
            }

            // Bluetooth headset
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null &&
                BluetoothAdapter.STATE_CONNECTED == bluetoothAdapter.getProfileConnectionState(
                    BluetoothProfile.HEADSET
                ) &&
                isBluetoothHeadsetHardKey(keyEvent)
            ) {
                return true
            }
        }

        if (!playbackService.hasMedia()
            && (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        ) {
            return if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                PlaybackService.loadLastAudio(playbackService)
                true
            } else false
        }
        /**
         * Implement fast forward and rewind behavior by directly handling the previous and next button events.
         * Normally the buttons are triggered on ACTION_DOWN; however, we ignore the ACTION_DOWN event when
         * isAndroidAutoHardKey returns true, and perform the operation on the ACTION_UP event instead. If the previous or
         * next button is held down, a callback occurs with the long press flag set. When a long press is received,
         * invoke the onFastForward() or onRewind() methods, and set the prevActionSeek flag. The ACTION_UP event
         * action is bypassed if the flag is set. The prevActionSeek flag is reset to false for the next invocation.
         */
        if (isAndroidAutoHardKey(keyEvent) && (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT)) {
            when (keyEvent.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (playbackService.isSeekable && keyEvent.isLongPress) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_NEXT -> onFastForward()
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> onRewind()
                        }
                        prevActionSeek = true
                    }
                }

                KeyEvent.ACTION_UP -> {
                    if (!prevActionSeek) {
                        val enabledActions = playbackService.enabledActions
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_NEXT -> if ((enabledActions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0L) onSkipToNext()
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> if ((enabledActions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0L) onSkipToPrevious()
                        }
                    }
                    prevActionSeek = false
                }
            }
            return true
        }
        return super.onMediaButtonEvent(mediaButtonEvent)
    }

    /**
     * The following two functions are based on the following KeyEvent captures. They may need to be updated if the behavior changes in the future.
     *
     * KeyEvent from Media Control UI:
     * {action=ACTION_DOWN, keyCode=KEYCODE_MEDIA_PLAY_PAUSE, scanCode=0, metaState=0, flags=0x0, repeatCount=0, eventTime=0, downTime=0, deviceId=-1, source=0x0, displayId=0}
     *
     * KeyEvent from a wired headset's media button:
     * {action=ACTION_DOWN, keyCode=KEYCODE_MEDIA_PLAY_PAUSE, scanCode=0, metaState=0, flags=0x40000000, repeatCount=0, eventTime=0, downTime=0, deviceId=-1, source=0x0, displayId=0}
     *
     * KeyEvent from a Bluetooth earphone:
     * {action=ACTION_DOWN, keyCode=KEYCODE_MEDIA_PLAY, scanCode=0, metaState=0, flags=0x0, repeatCount=0, eventTime=0, downTime=0, deviceId=-1, source=0x0, displayId=0}
     */
    private fun isWiredHeadsetHardKey(keyEvent: KeyEvent): Boolean {
        return !(keyEvent.deviceId == -1 && keyEvent.flags == 0x0)
    }

    private fun isBluetoothHeadsetHardKey(keyEvent: KeyEvent): Boolean {
        return keyEvent.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && keyEvent.deviceId == -1 && keyEvent.flags == 0x0
    }

    /**
     * This function is based on the following KeyEvent captures. This may need to be updated if the behavior changes in the future.
     *
     * KeyEvent from Media Control UI:
     * {action=ACTION_DOWN, keyCode=KEYCODE_MEDIA_NEXT, scanCode=0, metaState=0, flags=0x0, repeatCount=0, eventTime=0, downTime=0, deviceId=-1, source=0x0, displayId=0}
     *
     * KeyEvent from Android Auto Steering Wheel Control:
     * {action=ACTION_DOWN, keyCode=KEYCODE_MEDIA_NEXT, scanCode=0, metaState=0, flags=0x4, repeatCount=0, eventTime=0, downTime=0, deviceId=0, source=0x0, displayId=0}
     *
     * KeyEvent from Android Auto Steering Wheel Control, Holding Switch (Long Press):
     * {action=ACTION_DOWN, keyCode=KEYCODE_MEDIA_NEXT, scanCode=0, metaState=0, flags=0x84, repeatCount=1, eventTime=0, downTime=0, deviceId=0, source=0x0, displayId=0}
     */
    @SuppressLint("LongLogTag")
    private fun isAndroidAutoHardKey(keyEvent: KeyEvent): Boolean {
        return keyEvent.deviceId == 0 && (keyEvent.flags and KeyEvent.FLAG_KEEP_TOUCH_MODE != 0)
    }

    override fun onCustomAction(actionId: String?, extras: Bundle?) {
        when (actionId) {
            CUSTOM_ACTION_SPEED -> {
                val steps = listOf(0.50f, 0.80f, 1.00f, 1.10f, 1.20f, 1.50f, 2.00f)
                val index = 1 + steps.indexOf(steps.minByOrNull { abs(playbackService.rate - it) })
                playbackService.setRate(steps[index % steps.size], false)
            }

            CUSTOM_ACTION_REWIND -> onRewind()
            CUSTOM_ACTION_FAST_FORWARD -> onFastForward()
            CUSTOM_ACTION_SHUFFLE -> if (playbackService.canShuffle()) playbackService.shuffle()
            CUSTOM_ACTION_REPEAT -> playbackService.repeatType = when (playbackService.repeatType) {
                PlaybackStateCompat.REPEAT_MODE_NONE -> PlaybackStateCompat.REPEAT_MODE_ALL
                PlaybackStateCompat.REPEAT_MODE_ALL -> PlaybackStateCompat.REPEAT_MODE_ONE
                PlaybackStateCompat.REPEAT_MODE_ONE -> PlaybackStateCompat.REPEAT_MODE_NONE
                else -> PlaybackStateCompat.REPEAT_MODE_NONE
            }
        }
    }

    private fun loadMedia(
        mediaList: List<MediaWrapper>?,
        position: Int = 0,
        allowRandom: Boolean = false
    ) {
        mediaList?.let {
            // Pick a random first track if allowRandom is true and shuffle is enabled
            playbackService.load(
                mediaList,
                if (allowRandom && playbackService.isShuffling) SecureRandom().nextInt(
                    min(
                        mediaList.size,
                        MEDIALIBRARY_PAGE_SIZE
                    )
                ) else position
            )
        }
    }

    private fun checkForSeekFailure(forward: Boolean) {
        if (playbackService.playlistManager.player.lastPosition == 0.0f && (forward || playbackService.getTime() > 0))
            playbackService.displayPlaybackMessage(R.string.unseekable_stream)
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) = playbackService.loadUri(uri)

    override fun onSetShuffleMode(shuffleMode: Int) {
        playbackService.shuffleType = shuffleMode
    }

    override fun onSetRepeatMode(repeatMode: Int) {
        playbackService.repeatType = repeatMode
    }

    override fun onPause() = playbackService.pause()

    override fun onStop() = playbackService.stop()

    override fun onSkipToNext() = playbackService.next()

    override fun onSkipToPrevious() = playbackService.previous(false)

    override fun onSeekTo(pos: Long) =
        playbackService.seek(if (pos < 0) playbackService.getTime() + pos else pos, fromUser = true)

    override fun onFastForward() {
        playbackService.seek(
            (playbackService.getTime() + Settings.audioJumpDelay * ONE_SECOND).coerceAtMost(
                playbackService.length
            ), fromUser = true
        )
        checkForSeekFailure(forward = true)
    }

    override fun onRewind() {
        playbackService.seek(
            (playbackService.getTime() - Settings.audioJumpDelay * ONE_SECOND).coerceAtLeast(
                0
            ), fromUser = true
        )
        checkForSeekFailure(forward = false)
    }

    override fun onSkipToQueueItem(id: Long) =
        playbackService.playIndexOrLoadLastPlaylist(id.toInt())
}