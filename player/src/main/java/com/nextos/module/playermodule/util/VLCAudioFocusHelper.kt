package com.nextos.module.playermodule.util

import android.annotation.TargetApi
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.nextos.module.playermodule.BuildConfig
import com.nextos.module.playermodule.service.PlaybackService
import com.nextos.module.playermodule.tools.AUDIO_DUCKING
import com.nextos.module.playermodule.tools.RESUME_PLAYBACK
import org.videolan.libvlc.util.AndroidUtil

private const val TAG = "VLCAudioFocusHelper"
@Suppress("DEPRECATION")
class VLCAudioFocusHelper(private val service: PlaybackService) {

    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest : AudioFocusRequest
    private var hasAudioFocus = false
    @Volatile
    internal var lossTransient = false

    private val audioFocusListener = createOnAudioFocusChangeListener()

    internal fun changeAudioFocus(acquire: Boolean) {
        if (!this::audioManager.isInitialized) audioManager =
            (service.getSystemService(Context.AUDIO_SERVICE) ?: return) as AudioManager

        if (acquire && !service.hasRenderer()) {
            if (!hasAudioFocus) {
                val result = requestAudioFocus()
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioManager.setParameters("bgm_state=true")
                    hasAudioFocus = true
                }
            }
        } else if (hasAudioFocus) {
            abandonAudioFocus()
            audioManager.setParameters("bgm_state=false")
            hasAudioFocus = false
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocus() = if (AndroidUtil.isOOrLater) {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    } else {
        audioManager.abandonAudioFocus(audioFocusListener)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun requestAudioFocus() = if (AndroidUtil.isOOrLater) {
        val attributes = AudioAttributes.Builder()
                .setContentType(if (service.isVideoPlaying) AudioAttributes.CONTENT_TYPE_MOVIE else AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setAudioAttributes(attributes)
                .build()
        audioManager.requestAudioFocus(audioFocusRequest)
    } else {
        audioManager.requestAudioFocus(audioFocusListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun createOnAudioFocusChangeListener(): AudioManager.OnAudioFocusChangeListener {
        return object : AudioManager.OnAudioFocusChangeListener {
            private var lossTransientVolume = -1
            private var wasPlaying = false

            override fun onAudioFocusChange(focusChange: Int) {
                Log.d(TAG, "onAudioFocusChange: $focusChange")
                /*
             * Pause playback during alerts and notifications
             */
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        if (BuildConfig.DEBUG) Log.i(TAG, "AUDIOFOCUS_LOSS")
                        // Pause playback
                        changeAudioFocus(false)
                        service.pause()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        if (BuildConfig.DEBUG) Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
                        // Pause playback
                        pausePlayback()
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        if (BuildConfig.DEBUG) Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                        // Lower the volume
                        if (service.isPlaying) {
                            if (AndroidDevices.isAmazon) {
                                pausePlayback()
                            } else if (service.settings.getBoolean(AUDIO_DUCKING, true)) {
                                val volume = service.volume
                                lossTransientVolume = volume
                                service.setVolume(volume/3)
                            }
                        }
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (BuildConfig.DEBUG) Log.i(TAG, "AUDIOFOCUS_GAIN: ")
                        // Resume playback
                        if (lossTransientVolume != -1) {
                            service.setVolume(lossTransientVolume)
                            lossTransientVolume = -1
                        }
                        if (lossTransient) {
                            if (wasPlaying && service.settings.getBoolean(RESUME_PLAYBACK, true))
                                service.play()
                            lossTransient = false
                        }
                    }
                }
            }

            private fun pausePlayback() {
                if (lossTransient) return
                lossTransient = true
                wasPlaying = service.isPlaying
                if (wasPlaying) service.pause()
            }
        }
    }
}