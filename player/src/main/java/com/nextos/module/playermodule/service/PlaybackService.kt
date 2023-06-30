/*****************************************************************************
 * PlaybackService.kt
 * Copyright © 2011-2018 VLC authors and VideoLAN
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

package com.nextos.module.playermodule.service

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.res.Configuration
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.nextos.module.playermodule.BuildConfig
import com.nextos.module.playermodule.MediaBrowserInstance
import com.nextos.module.playermodule.MediaSessionCallback
import com.nextos.module.playermodule.NO_LENGTH_PROGRESS_MAX
import com.nextos.module.playermodule.PlayerController
import com.nextos.module.playermodule.PlaylistManager
import com.nextos.module.playermodule.R
import com.nextos.module.playermodule.helper.AudioUtil
import com.nextos.module.playermodule.helper.NotificationHelper
import com.nextos.module.playermodule.media.MediaSessionBrowser
import com.nextos.module.playermodule.media.MediaUtils
import com.nextos.module.playermodule.media.MediaWrapper
import com.nextos.module.playermodule.resources.ACTION_PLAY_FROM_SEARCH
import com.nextos.module.playermodule.resources.ACTION_REMOTE_BACKWARD
import com.nextos.module.playermodule.resources.ACTION_REMOTE_FORWARD
import com.nextos.module.playermodule.resources.ACTION_REMOTE_GENERIC
import com.nextos.module.playermodule.resources.ACTION_REMOTE_LAST_PLAYLIST
import com.nextos.module.playermodule.resources.ACTION_REMOTE_PLAY
import com.nextos.module.playermodule.resources.ACTION_REMOTE_PLAYPAUSE
import com.nextos.module.playermodule.resources.ACTION_REMOTE_SEEK_BACKWARD
import com.nextos.module.playermodule.resources.ACTION_REMOTE_SEEK_FORWARD
import com.nextos.module.playermodule.resources.ACTION_REMOTE_STOP
import com.nextos.module.playermodule.resources.ACTION_REMOTE_SWITCH_VIDEO
import com.nextos.module.playermodule.resources.AppContextProvider
import com.nextos.module.playermodule.resources.CUSTOM_ACTION
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_BOOKMARK
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_FAST_FORWARD
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_REPEAT
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_REWIND
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_SHUFFLE
import com.nextos.module.playermodule.resources.CUSTOM_ACTION_SPEED
import com.nextos.module.playermodule.resources.EXTRA_CUSTOM_ACTION_ID
import com.nextos.module.playermodule.resources.EXTRA_MEDIA_SEARCH_SUPPORTED
import com.nextos.module.playermodule.resources.EXTRA_SEARCH_BUNDLE
import com.nextos.module.playermodule.resources.EXTRA_SEEK_DELAY
import com.nextos.module.playermodule.resources.PLAYBACK_SLOT_RESERVATION_SKIP_TO_NEXT
import com.nextos.module.playermodule.resources.PLAYBACK_SLOT_RESERVATION_SKIP_TO_PREV
import com.nextos.module.playermodule.resources.PLAYLIST_TYPE_AUDIO
import com.nextos.module.playermodule.resources.VLCInstance
import com.nextos.module.playermodule.resources.VLCOptions
import com.nextos.module.playermodule.resources.WEARABLE_RESERVE_SLOT_SKIP_TO_NEXT
import com.nextos.module.playermodule.resources.WEARABLE_RESERVE_SLOT_SKIP_TO_PREV
import com.nextos.module.playermodule.resources.WEARABLE_SHOW_CUSTOM_ACTION
import com.nextos.module.playermodule.resources.util.VLCCrashHandler
import com.nextos.module.playermodule.tools.AUDIO_RESUME_PLAYBACK
import com.nextos.module.playermodule.tools.DrawableCache
import com.nextos.module.playermodule.tools.LOCKSCREEN_COVER
import com.nextos.module.playermodule.tools.LiveEvent
import com.nextos.module.playermodule.tools.POSITION_IN_AUDIO_LIST
import com.nextos.module.playermodule.tools.POSITION_IN_SONG
import com.nextos.module.playermodule.tools.SHOW_SEEK_IN_COMPACT_NOTIFICATION
import com.nextos.module.playermodule.tools.Settings
import com.nextos.module.playermodule.tools.WeakHandler
import com.nextos.module.playermodule.tools.getContextWithLocale
import com.nextos.module.playermodule.tools.readableSize
import com.nextos.module.playermodule.util.AccessControl
import com.nextos.module.playermodule.util.AndroidDevices
import com.nextos.module.playermodule.util.Permissions
import com.nextos.module.playermodule.util.RendererLiveData
import com.nextos.module.playermodule.util.TextUtils
import com.nextos.module.playermodule.util.Util
import com.nextos.module.playermodule.util.VLCAudioFocusHelper
import com.nextos.module.playermodule.util.getBitmapFromDrawable
import com.nextos.module.playermodule.util.isSchemeHttpOrHttps
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.MutableStateFlow
import org.videolan.libvlc.FactoryManager
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.RendererItem
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.interfaces.IMediaFactory
import org.videolan.libvlc.interfaces.IVLCVout
import org.videolan.libvlc.util.AndroidUtil
import com.nextos.module.playermodule.util.launchForeground
import java.util.*
import kotlin.math.abs

private const val TAG = "VLC/PlaybackService"

class PlaybackService : MediaBrowserServiceCompat(), LifecycleOwner, CoroutineScope {
    override val coroutineContext = Dispatchers.IO + SupervisorJob()

    private var position: Long = -1L
    private val dispatcher = ServiceLifecycleDispatcher(this)
    internal var enabledActions = PLAYBACK_BASE_ACTIONS
    lateinit var playlistManager: PlaylistManager
        private set
    val mediaplayer: MediaPlayer
        get() = playlistManager.player.mediaplayer
    private lateinit var keyguardManager: KeyguardManager
    internal lateinit var settings: SharedPreferences
    private val binder = LocalBinder()

    private val callbacks = mutableListOf<Callback>()
    private val subtitleMessage = ArrayDeque<String>(1)

    private lateinit var cbActor: SendChannel<CbAction>
    var detectHeadset = true
    var headsetInserted = false
    private lateinit var wakeLock: PowerManager.WakeLock
    private val audioFocusHelper by lazy { VLCAudioFocusHelper(this) }
    var sleepTimerJob: Job? = null
    var waitForMediaEnd = false
    private var mediaEndReached = false

    // Playback management
    internal lateinit var mediaSession: MediaSessionCompat

    @Volatile
    private var notificationShowing = false
    private var prevUpdateInCarMode = true
    private var lastTime = 0L
    private var lastLength = 0L
    private var lastChapter = 0
    private var lastChaptersCount = 0
    private var lastParentId = ""
    private var widget = 0

    /**
     * Last widget position update timestamp
     */
    private var widgetPositionTimestamp = System.currentTimeMillis()

    private val mediaFactory = FactoryManager.getFactory(IMediaFactory.factoryId) as IMediaFactory

    /**
     * Binds a [MediaBrowserCompat] to the service to allow receiving the
     * [MediaSessionCompat.Callback] callbacks even if the service is killed
     */
    lateinit var mediaBrowserCompat: MediaBrowserCompat

    private val receiver = object : BroadcastReceiver() {
        private var wasPlaying = false
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            val state = intent.getIntExtra("state", 0)


            // skip all headsets events if there is a call
            if ((context.getSystemService(AUDIO_SERVICE) as AudioManager).mode == AudioManager.MODE_IN_CALL) return

            /*
             * Launch the activity if needed
             */
            if (action.startsWith(ACTION_REMOTE_GENERIC) && !isPlaying && !playlistManager.hasCurrentMedia()) {
                packageManager.getLaunchIntentForPackage(packageName)
                    ?.let { context.startActivity(it) }
            }

            /*
             * Remote / headset control events
             */
            when (action) {
                CUSTOM_ACTION -> intent.getStringExtra(EXTRA_CUSTOM_ACTION_ID)?.let { actionId ->
                    mediaSession.controller.transportControls.sendCustomAction(actionId, null)
                    executeUpdate()
                    showNotification()
                }

                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> if (detectHeadset) {
                    if (BuildConfig.DEBUG) Log.i(TAG, "Becoming noisy")
                    headsetInserted = false
                    wasPlaying = isPlaying
                    if (wasPlaying && playlistManager.hasCurrentMedia()) pause()
                }

                Intent.ACTION_HEADSET_PLUG -> if (detectHeadset && state != 0) {
                    if (BuildConfig.DEBUG) Log.i(TAG, "Headset Inserted.")
                    headsetInserted = true
                    if (wasPlaying && playlistManager.hasCurrentMedia() && settings.getBoolean(
                            "enable_play_on_headset_insertion", false
                        )
                    ) play()
                }
            }/*
             * headset plug events
             */
        }
    }

    private val mediaPlayerListener = MediaPlayer.EventListener { event ->
        when (event.type) {
            MediaPlayer.Event.Playing -> {
                if (BuildConfig.DEBUG) Log.i(TAG, "MediaPlayer.Event.Playing")
                executeUpdate(true)
                lastTime = getTime()
                audioFocusHelper.changeAudioFocus(true)
                if (!wakeLock.isHeld) wakeLock.acquire()
                showNotification()
                handler.nbErrors = 0
            }

            MediaPlayer.Event.Paused -> {
                if (BuildConfig.DEBUG) Log.i(TAG, "MediaPlayer.Event.Paused")
                executeUpdate(true)
                showNotification()
                if (wakeLock.isHeld) wakeLock.release()
            }

            MediaPlayer.Event.EncounteredError -> executeUpdate()
            MediaPlayer.Event.LengthChanged -> {
                lastChaptersCount = getChapters(-1)?.size ?: 0
                if (lastLength == 0L) {
                    executeUpdate(true)
                }
            }

            MediaPlayer.Event.PositionChanged -> {
                if (length == 0L) position =
                    (NO_LENGTH_PROGRESS_MAX.toLong() * event.positionChanged).toLong()
                if (getTime() < 1000L && getTime() < lastTime) publishState()
                lastTime = getTime()
                val curChapter = chapterIdx
                if (lastChapter != curChapter) {
                    executeUpdate()
                    showNotification()
                }
                lastChapter = curChapter
            }

            MediaPlayer.Event.ESAdded -> if (event.esChangedType == IMedia.Track.Type.Video && (playlistManager.videoBackground || !playlistManager.switchToVideo())) {/* CbAction notification content intent: resume video or resume audio activity */
                updateMetadata()
            }

            MediaPlayer.Event.MediaChanged -> if (BuildConfig.DEBUG) Log.d(
                TAG, "onEvent: MediaChanged"
            )

            MediaPlayer.Event.EndReached -> mediaEndReached = true
        }
        Log.d("zxm==", "PlaybackService..mediaPlayerListener..onEvent()..type:${event.type}")
        cbActor.trySend(CbMediaPlayerEvent(event))
    }

    private val handler = PlaybackServiceHandler(this)


    @Volatile
    private var isForeground = false

    private var currentWidgetCover: String? = null

    val isPodcastMode: Boolean
        @MainThread get() = playlistManager.getMediaListSize() == 1 && playlistManager.getCurrentMedia()?.isPodcast == true

    val speed: Float
        @MainThread get() = playlistManager.player.speed.value ?: 1.0F

    val isPlaying: Boolean
        @MainThread get() = playlistManager.player.isPlaying()

    val isSeekable: Boolean
        @MainThread get() = playlistManager.player.seekable

    val isPausable: Boolean
        @MainThread get() = playlistManager.player.pausable

    val isShuffling: Boolean
        @MainThread get() = playlistManager.shuffling

    var shuffleType: Int
        @MainThread get() = if (playlistManager.shuffling) PlaybackStateCompat.SHUFFLE_MODE_ALL else PlaybackStateCompat.SHUFFLE_MODE_NONE
        @MainThread set(shuffleType) {
            when {
                shuffleType == PlaybackStateCompat.SHUFFLE_MODE_ALL && !isShuffling -> shuffle()
                shuffleType == PlaybackStateCompat.SHUFFLE_MODE_NONE && isShuffling -> shuffle()
                shuffleType == PlaybackStateCompat.SHUFFLE_MODE_GROUP && !isShuffling -> shuffle()
                shuffleType == PlaybackStateCompat.SHUFFLE_MODE_GROUP && isShuffling -> publishState()
            }
        }

    var repeatType: Int
        @MainThread get() = playlistManager.repeating
        @MainThread set(repeatType) {
            playlistManager.setRepeatType(if (repeatType == PlaybackStateCompat.REPEAT_MODE_GROUP) PlaybackStateCompat.REPEAT_MODE_ALL else repeatType)
            publishState()
        }

    val isVideoPlaying: Boolean
        @MainThread get() = playlistManager.player.isVideoPlaying()

    val album: String?
        @MainThread get() {
            val media = playlistManager.getCurrentMedia()
            return if (media != null) MediaUtils.getMediaAlbum(
                this@PlaybackService, media
            ) else null
        }

    val albumPrev: String?
        @MainThread get() {
            val prev = playlistManager.getPrevMedia()
            return if (prev != null) MediaUtils.getMediaAlbum(this@PlaybackService, prev) else null
        }

    val albumNext: String?
        @MainThread get() {
            val next = playlistManager.getNextMedia()
            return if (next != null) MediaUtils.getMediaAlbum(this@PlaybackService, next) else null
        }

    val artist: String?
        @MainThread get() {
            val media = playlistManager.getCurrentMedia()
            return if (media != null) MediaUtils.getMediaArtist(
                this@PlaybackService, media
            ) else null
        }

    val artistPrev: String?
        @MainThread get() {
            val prev = playlistManager.getPrevMedia()
            return if (prev != null) MediaUtils.getMediaArtist(this@PlaybackService, prev) else null
        }

    val artistNext: String?
        @MainThread get() {
            val next = playlistManager.getNextMedia()
            return if (next != null) MediaUtils.getMediaArtist(this@PlaybackService, next) else null
        }

    val title: String?
        @MainThread get() {
            val media = playlistManager.getCurrentMedia()
            return if (media != null) if (media.nowPlaying != null) media.nowPlaying else media.title else null
        }

    val titlePrev: String?
        @MainThread get() {
            val prev = playlistManager.getPrevMedia()
            return prev?.title
        }

    val titleNext: String?
        @MainThread get() {
            val next = playlistManager.getNextMedia()
            return next?.title
        }

    val coverArt: String?
        @MainThread get() {
            val media = playlistManager.getCurrentMedia()
            return media?.artworkMrl
        }

    val prevCoverArt: String?
        @MainThread get() {
            val prev = playlistManager.getPrevMedia()
            return prev?.artworkMrl
        }

    val nextCoverArt: String?
        @MainThread get() {
            val next = playlistManager.getNextMedia()
            return next?.artworkMrl
        }

    fun getCurrentChapter(): String? {
        return getChapters(-1)?.let { chapters ->
            val curChapter = chapterIdx
            if (curChapter >= 0 && chapters.isNotEmpty()) {
                TextUtils.formatChapterTitle(this, curChapter + 1, chapters[curChapter].name)
            } else null
        }
    }

    fun IMedia.AudioTrack.formatTrackInfoString(context: Context): String {
        val trackInfo = mutableListOf<String>()
        if (bitrate > 0) trackInfo.add(
            context.getString(
                R.string.track_bitrate_info, bitrate.toLong().readableSize()
            )
        )
        trackInfo.add(context.getString(R.string.track_codec_info, codec))
        trackInfo.add(context.getString(R.string.track_samplerate_info, rate))
        return TextUtils.separatedString(trackInfo.toTypedArray()).replace("\n", "")
    }


    val length: Long
        @MainThread get() = playlistManager.player.getLength()

    val lastStats: IMedia.Stats?
        get() = playlistManager.player.previousMediaStats


    val mediaListSize: Int
        get() = playlistManager.getMediaListSize()

    val media: List<MediaWrapper>
        @MainThread get() = playlistManager.getMediaList()

    val previousTotalTime
        @MainThread get() = playlistManager.previousTotalTime()

    val mediaLocations: List<String>
        @MainThread get() {
            return mutableListOf<String>().apply { for (mw in playlistManager.getMediaList()) add(mw.location) }
        }

    val currentMediaLocation: String?
        @MainThread get() = playlistManager.getCurrentMedia()?.location

    val currentMediaPosition: Int
        @MainThread get() = playlistManager.currentIndex

    val currentMediaWrapper: MediaWrapper?
        @MainThread get() = this@PlaybackService.playlistManager.getCurrentMedia()

    val rate: Float
        @MainThread get() = playlistManager.player.getRate()

    val titles: Array<out MediaPlayer.Title>?
        @MainThread get() = playlistManager.player.getTitles()

    var chapterIdx: Int
        @MainThread get() = playlistManager.player.getChapterIdx()
        @MainThread set(chapter) {
            playlistManager.player.setChapterIdx(chapter)
            getChapters(-1)?.let {
                publishState(it[chapter].timeOffset)
            }
        }

    var titleIdx: Int
        @MainThread get() = playlistManager.player.getTitleIdx()
        @MainThread set(title) = playlistManager.player.setTitleIdx(title)

    val volume: Int
        @MainThread get() = playlistManager.player.getVolume()


    val videoTracksCount: Int
        @MainThread get() = if (hasMedia()) playlistManager.player.getVideoTracksCount() else 0

    val spuTracksCount: Int
        @MainThread get() = playlistManager.player.getSpuTracksCount()

    val audioDelay: Long
        @MainThread get() = playlistManager.player.getAudioDelay()

    val spuDelay: Long
        @MainThread get() = playlistManager.player.getSpuDelay()

    interface Callback {
        fun update()
        fun onMediaEvent(event: IMedia.Event)
        fun onMediaPlayerEvent(event: MediaPlayer.Event)
    }

    private inner class LocalBinder : Binder() {
        internal val service: PlaybackService
            get() = this@PlaybackService
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.getContextWithLocale(AppContextProvider.locale))
    }

    override fun getApplicationContext(): Context {
        return super.getApplicationContext().getContextWithLocale(AppContextProvider.locale)
    }

    @SuppressLint("InvalidWakeLockTag")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        setupScope()
        forceForeground()
        super.onCreate()
        NotificationHelper.createNotificationChannels(applicationContext)
        settings = Settings.getInstance(this)
        playlistManager = PlaylistManager(this)
        Util.checkCpuCompatibility(this)


        detectHeadset = settings.getBoolean("enable_headset_detection", true)

        // Make sure the audio player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        val pm = applicationContext.getSystemService<PowerManager>()!!
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)

        if (!this::mediaSession.isInitialized) initMediaSession()


        keyguardManager = getSystemService()!!
        renderer.observe(this, Observer { setRenderer(it) })
        restartPlayer.observe(this, Observer { restartPlaylistManager() })
        headSetDetection.observe(this, Observer { detectHeadset(it) })
        equalizer.observe(this, Observer { setEqualizer(it) })
        serviceFlow.value = this
        mediaBrowserCompat = MediaBrowserInstance.getInstance(this)
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun setupScope() {
        cbActor = lifecycleScope.actor(capacity = Channel.UNLIMITED) {
            for (update in channel) when (update) {
                CbUpdate -> for (callback in callbacks) callback.update()
                is CbMediaEvent -> for (callback in callbacks) callback.onMediaEvent(update.event)
                is CbMediaPlayerEvent -> for (callback in callbacks) callback.onMediaPlayerEvent(
                    update.event
                )

                is CbRemove -> callbacks.remove(update.cb)
                is CbAdd -> callbacks.add(update.cb)
                ShowNotification -> showNotificationInternal()
                is HideNotification -> hideNotificationInternal(update.remove)

                else -> {}
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        forceForeground(intent?.extras?.getBoolean("foreground", false) ?: false)
        dispatcher.onServicePreSuperOnStart()
        setupScope()
        when (intent?.action) {
            Intent.ACTION_MEDIA_BUTTON -> {
                if (AndroidDevices.hasTsp || AndroidDevices.hasPlayServices) MediaButtonReceiver.handleIntent(
                    mediaSession, intent
                )
            }

            ACTION_REMOTE_PLAYPAUSE, ACTION_REMOTE_PLAY, ACTION_REMOTE_LAST_PLAYLIST -> {
                if (playlistManager.hasCurrentMedia()) {
                    if (isPlaying) pause()
                    else play()
                } else loadLastAudioPlaylist()
            }

            ACTION_REMOTE_BACKWARD -> previous(false)
            ACTION_REMOTE_FORWARD -> next()
            ACTION_REMOTE_STOP -> stop()
            ACTION_REMOTE_SEEK_FORWARD -> seek(
                getTime() + intent.getLongExtra(
                    EXTRA_SEEK_DELAY, 0L
                ) * 1000L
            )

            ACTION_REMOTE_SEEK_BACKWARD -> seek(
                getTime() - intent.getLongExtra(
                    EXTRA_SEEK_DELAY, 0L
                ) * 1000L
            )

            ACTION_PLAY_FROM_SEARCH -> {
                if (!this::mediaSession.isInitialized) initMediaSession()
                intent.getBundleExtra(EXTRA_SEARCH_BUNDLE)?.let {
                    mediaSession.controller.transportControls.playFromSearch(
                        it.getString(
                            SearchManager.QUERY
                        ), it
                    )
                }
            }

            ACTION_REMOTE_SWITCH_VIDEO -> {

                if (hasMedia()) {
                    currentMediaWrapper!!.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO)
                    playlistManager.switchToVideo()
                }
            }
        }
        return Service.START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        if (settings.getBoolean("audio_task_removed", false)) stop()
    }

    override fun onGetRoot(
        clientPackageName: String, clientUid: Int, rootHints: Bundle?
    ): BrowserRoot? {
        AccessControl.logCaller(clientUid, clientPackageName)
        if (!Permissions.canReadStorage(this@PlaybackService)) {
            Log.w(
                TAG,
                "Returning null MediaBrowserService root. READ_EXTERNAL_STORAGE permission not granted."
            )
            return null
        }
        return when {
            rootHints?.containsKey(BrowserRoot.EXTRA_SUGGESTED) == true -> BrowserRoot(
                MediaSessionBrowser.ID_SUGGESTED, null
            )

            else -> {
                val rootId = when (clientPackageName) {
                    "com.google.android.googlequicksearchbox" -> MediaSessionBrowser.ID_ROOT_NO_TABS
                    else -> MediaSessionBrowser.ID_ROOT
                }
                val extras = MediaSessionBrowser.getContentStyle().apply {
                    putBoolean(EXTRA_MEDIA_SEARCH_SUPPORTED, true)
                }
                BrowserRoot(rootId, extras)
            }
        }
    }

    override fun onLoadChildren(
        parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.detach()
        val reload = parentId != lastParentId
        lastParentId = parentId
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            lifecycleScope.launch(Dispatchers.IO) {}
        }
    }

    override fun onDestroy() {
        serviceFlow.value = null
        dispatcher.onServicePreSuperOnDestroy()
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (!settings.getBoolean(AUDIO_RESUME_PLAYBACK, true)) (getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager).cancel(3)
        if (this::mediaSession.isInitialized) mediaSession.release()
        //Call it once mediaSession is null, to not publish playback state
        stop(systemExit = true)

        unregisterReceiver(receiver)
        playlistManager.onServiceDestroyed()
    }

    override fun onBind(intent: Intent): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return if (SERVICE_INTERFACE == intent.action) super.onBind(intent) else binder
    }

    val vout: IVLCVout?
        get() {
            return playlistManager.player.getVout()
        }

    @TargetApi(Build.VERSION_CODES.O)
    private fun forceForeground(launchedInForeground: Boolean = false) {
        if (!AndroidUtil.isOOrLater || isForeground) return
        val ctx = applicationContext
        val stopped =
            PlayerController.playbackState == PlaybackStateCompat.STATE_STOPPED || PlayerController.playbackState == PlaybackStateCompat.STATE_NONE
        if (stopped && !launchedInForeground) {
            if (BuildConfig.DEBUG) Log.i(
                "PlaybackService",
                "Service not in foreground and player is stopped. Skipping the notification"
            )
            return
        }
        val notification = if (this::notification.isInitialized && !stopped) notification
        else {

            NotificationHelper.createPlaybackNotification(
                ctx,
                false,
                ctx.resources.getString(R.string.loading),
                "",
                "",
                null,
                false,
                true,
                true,
                speed,
                isPodcastMode,
                false,
                enabledActions,
                null,
                null
            )
        }
        startForeground(3, notification)
        isForeground = true
        if (stopped) lifecycleScope.launch { hideNotification(true) }
    }

    private fun sendStartSessionIdIntent() {
        val sessionId = VLCOptions.audiotrackSessionId
        if (sessionId == 0) return

        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        if (isVideoPlaying) intent.putExtra(
            AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE
        )
        else intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        sendBroadcast(intent)
    }

    private fun sendStopSessionIdIntent() {
        val sessionId = VLCOptions.audiotrackSessionId
        if (sessionId == 0) return

        val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        sendBroadcast(intent)
    }

    fun setBenchmark() {
        playlistManager.isBenchmark = true
    }

    fun setHardware() {
        playlistManager.isHardware = true
    }

    fun setTime(time: Long, fast: Boolean = false) {
        val shouldFast =
            fast || (!playlistManager.isBenchmark && settings.getBoolean("always_fast_seek", false))
        playlistManager.player.setTime(time, shouldFast)
        publishState(time)
    }

    fun getTime() = playlistManager.player.getCurrentTime()

    fun onMediaPlayerEvent(event: MediaPlayer.Event) = mediaPlayerListener.onEvent(event)

    fun onPlaybackStopped(systemExit: Boolean) {
        if (!systemExit) hideNotification(isForeground)

        if (wakeLock.isHeld) wakeLock.release()
        audioFocusHelper.changeAudioFocus(false)
        // We must publish state before resetting mCurrentIndex
        publishState()
        executeUpdate()
    }

    private fun canSwitchToVideo() = playlistManager.player.canSwitchToVideo()

    fun onMediaEvent(event: IMedia.Event) = cbActor.trySend(CbMediaEvent(event))

    fun executeUpdate(pubState: Boolean = false) {
        cbActor.trySend(CbUpdate)

        updateMetadata()
        broadcastMetadata()
        if (pubState) publishState()
    }

    private class PlaybackServiceHandler(owner: PlaybackService) :
        WeakHandler<PlaybackService>(owner) {

        var currentToast: Toast? = null
        var lastErrorTime = 0L
        var nbErrors = 0

        @SuppressLint("ShowToast")
        override fun handleMessage(msg: Message) {
            val service = owner ?: return
            when (msg.what) {
                SHOW_TOAST -> {
                    val bundle = msg.data
                    var text = bundle.getString("text")
                    val duration = bundle.getInt("duration")
                    val isError = bundle.getBoolean("isError")
                    if (isError) {
                        when {
                            nbErrors > 2 && System.currentTimeMillis() - lastErrorTime < 500 -> return
                            nbErrors >= 2 -> text =
                                service.getString(R.string.playback_multiple_errors)
                        }
                        currentToast?.cancel()
                        nbErrors++
                        lastErrorTime = System.currentTimeMillis()
                    }
                    currentToast = Toast.makeText(AppContextProvider.appContext, text, duration)
                    currentToast?.show()
                }

                END_MEDIASESSION -> if (service::mediaSession.isInitialized) service.mediaSession.isActive =
                    false
            }
        }
    }

    fun showNotification(): Boolean {
        notificationShowing = true
        return cbActor.trySend(ShowNotification).isSuccess
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun showNotificationInternal() {
        if (!AndroidDevices.isAndroidTv && Settings.showTvUi) return

        val mw = playlistManager.getCurrentMedia()
        if (mw != null) {
            val coverOnLockscreen = settings.getBoolean(LOCKSCREEN_COVER, true)
            val seekInCompactView = settings.getBoolean(SHOW_SEEK_IN_COMPACT_NOTIFICATION, false)
            val playing = isPlaying
            val sessionToken = mediaSession.sessionToken
            val ctx = this@PlaybackService
            val metaData = mediaSession.controller.metadata
            lifecycleScope.launch(Dispatchers.Default) {
                delay(100)

                try {
                    val title =
                        if (metaData == null) mw.title else metaData.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                    val artist =
                        if (metaData == null) mw.artist else metaData.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST)
                    val album =
                        if (metaData == null) mw.album else metaData.getString(MediaMetadataCompat.METADATA_KEY_ALBUM)
                    var cover = if (coverOnLockscreen && metaData != null) metaData.getBitmap(
                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART
                    ) else null
                    if (coverOnLockscreen && cover == null) cover =
                        AudioUtil.readCoverBitmap(Uri.decode(mw.artworkMrl), 256)
                    if (cover == null || cover.isRecycled) cover =
                        ctx.getBitmapFromDrawable(R.drawable.ic_no_media)

                    notification = NotificationHelper.createPlaybackNotification(
                        ctx,
                        canSwitchToVideo(),
                        title,
                        artist,
                        album,
                        cover,
                        playing,
                        isPausable,
                        isSeekable,
                        speed,
                        isPodcastMode,
                        seekInCompactView,
                        enabledActions,
                        sessionToken,
                        null
                    )
                    if (!AndroidUtil.isLolliPopOrLater || playing || audioFocusHelper.lossTransient) {
                        if (!isForeground) {
                            ctx.launchForeground(Intent(ctx, PlaybackService::class.java)) {
                                ctx.startForeground(3, notification)
                                isForeground = true
                            }
                        } else NotificationManagerCompat.from(ctx).notify(3, notification)
                    } else {
                        if (isForeground) {
                            ServiceCompat.stopForeground(ctx, ServiceCompat.STOP_FOREGROUND_DETACH)
                            isForeground = false
                        }
                        NotificationManagerCompat.from(ctx).notify(3, notification)
                    }
                } catch (e: IllegalArgumentException) {
                    // On somme crappy firmwares, shit can happen
                    Log.e(TAG, "Failed to display notification", e)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Failed to display notification", e)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Failed to display notification", e)
                } catch (e: ArrayIndexOutOfBoundsException) {
                    // Happens on Android 7.0 (Xperia L1 (G3312))
                    Log.e(TAG, "Failed to display notification", e)
                }
            }
        }
    }

    private lateinit var notification: Notification

    private fun currentMediaHasFlag(flag: Int): Boolean {
        val mw = playlistManager.getCurrentMedia()
        return mw != null && mw.hasFlag(flag)
    }

    private fun hideNotification(remove: Boolean): Boolean {
        notificationShowing = false
        return if (::cbActor.isInitialized) cbActor.trySend(HideNotification(remove)).isSuccess else false
    }

    private fun hideNotificationInternal(remove: Boolean) {
        if (isForeground) {
            ServiceCompat.stopForeground(
                this@PlaybackService,
                if (remove) ServiceCompat.STOP_FOREGROUND_REMOVE else ServiceCompat.STOP_FOREGROUND_DETACH
            )
            isForeground = false
        }
        NotificationManagerCompat.from(this@PlaybackService).cancel(3)
    }


    fun onPlaylistLoaded() {
        notifyTrackChanged()

    }

    @MainThread
    fun pause() = playlistManager.pause()

    @MainThread
    fun play() = playlistManager.play()

    @MainThread
    @JvmOverloads
    fun stop(systemExit: Boolean = false, video: Boolean = false) {
        playlistManager.stop(systemExit, video)
    }

    private fun initMediaSession() {
        val mbrIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_PLAY_PAUSE
        )
        val mbrName = ComponentName(this, MediaButtonReceiver::class.java)
        val playbackState = PlaybackStateCompat.Builder().setActions(enabledActions)
            .setState(PlaybackStateCompat.STATE_NONE, 0, 0f).build()
        mediaSession = MediaSessionCompat(this, "VLC", mbrName, mbrIntent).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(MediaSessionCallback(this@PlaybackService))
            setPlaybackState(playbackState)
        }
        try {
            mediaSession.isActive = true
        } catch (e: NullPointerException) {
            // Some versions of KitKat do not support AudioManager.registerMediaButtonIntent
            // with a PendingIntent. They will throw a NullPointerException, in which case
            // they should be able to activate a MediaSessionCompat with only transport
            // controls.
            mediaSession.isActive = false
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            mediaSession.isActive = true
        }

        sessionToken = mediaSession.sessionToken
    }

    private fun updateMetadata() {
        cbActor.trySend(UpdateMeta)
    }

    private fun publishState(position: Long? = null) {
        if (!this::mediaSession.isInitialized) return
        if (AndroidDevices.isAndroidTv) handler.removeMessages(END_MEDIASESSION)
        val pscb = PlaybackStateCompat.Builder()
        var actions = PLAYBACK_BASE_ACTIONS
        val hasMedia = playlistManager.hasCurrentMedia()
        var time = position ?: getTime()
        var state = PlayerController.playbackState
        when (state) {
            PlaybackStateCompat.STATE_PLAYING -> actions =
                actions or (PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP)

            PlaybackStateCompat.STATE_PAUSED -> actions =
                actions or (PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_STOP)

            else -> {
                actions = actions or PlaybackStateCompat.ACTION_PLAY
                val media =
                    if (AndroidDevices.isAndroidTv && !AndroidUtil.isOOrLater && hasMedia) playlistManager.getCurrentMedia() else null
                if (media != null) { // Hack to show a now paying card on Android TV
                    val length = media.length
                    time = media.time
                    val progress = if (length <= 0L) 0f else time / length.toFloat()
                    if (progress < 0.95f) {
                        state = PlaybackStateCompat.STATE_PAUSED
                        handler.sendEmptyMessageDelayed(END_MEDIASESSION, 900_000L)
                    }
                }
            }
        }
        pscb.setState(state, time, playlistManager.player.getRate())
        pscb.setActiveQueueItemId(playlistManager.currentIndex.toLong())
        val repeatType = playlistManager.repeating
        val podcastMode =
            playlistManager.getMediaListSize() == 1 && playlistManager.getCurrentMedia()?.isPodcast == true
        if (repeatType != PlaybackStateCompat.REPEAT_MODE_NONE || hasNext()) actions =
            actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        if (repeatType != PlaybackStateCompat.REPEAT_MODE_NONE || hasPrevious() || (isSeekable && !podcastMode)) actions =
            actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS

        when {
            podcastMode -> {
                addCustomSeekActions(pscb)
                addCustomSpeedActions(pscb)
                pscb.addCustomAction(
                    CUSTOM_ACTION_BOOKMARK,
                    getString(R.string.add_bookmark),
                    R.drawable.ic_bookmark_add
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                addCustomSeekActions(pscb)
            }

            else -> {
                actions = manageAutoActions(actions, pscb, repeatType)
            }
        }
        actions =
            actions or PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND or PlaybackStateCompat.ACTION_SEEK_TO
        pscb.setActions(actions)
        mediaSession.setRepeatMode(repeatType)
        mediaSession.setShuffleMode(if (isShuffling) PlaybackStateCompat.SHUFFLE_MODE_ALL else PlaybackStateCompat.SHUFFLE_MODE_NONE)
        mediaSession.setExtras(Bundle().apply {
            putBoolean(WEARABLE_RESERVE_SLOT_SKIP_TO_NEXT, !podcastMode)
            putBoolean(WEARABLE_RESERVE_SLOT_SKIP_TO_PREV, !podcastMode)
            putBoolean(PLAYBACK_SLOT_RESERVATION_SKIP_TO_NEXT, !podcastMode)
            putBoolean(PLAYBACK_SLOT_RESERVATION_SKIP_TO_PREV, !podcastMode)
        })
        val mediaIsActive = state != PlaybackStateCompat.STATE_STOPPED
        val update = mediaSession.isActive != mediaIsActive

        mediaSession.setPlaybackState(pscb.build())
        enabledActions = actions
        mediaSession.isActive = mediaIsActive
        mediaSession.setQueueTitle(getString(R.string.music_now_playing))
        if (update) {
            if (mediaIsActive) sendStartSessionIdIntent()
            else sendStopSessionIdIntent()
        }
    }

    private fun manageAutoActions(
        actions: Long, pscb: PlaybackStateCompat.Builder, repeatType: Int
    ): Long {
        var resultActions = actions
        if (playlistManager.canRepeat()) resultActions =
            resultActions or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
        if (playlistManager.canShuffle()) resultActions =
            resultActions or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE/* Always add the icons, regardless of the allowed actions */
        val shuffleResId = when {
            isShuffling -> R.drawable.ic_auto_shuffle_enabled
            else -> R.drawable.ic_auto_shuffle_disabled
        }
        pscb.addCustomAction(CUSTOM_ACTION_SHUFFLE, getString(R.string.shuffle_title), shuffleResId)
        val repeatResId = when (repeatType) {
            PlaybackStateCompat.REPEAT_MODE_ALL -> R.drawable.ic_auto_repeat_pressed
            PlaybackStateCompat.REPEAT_MODE_ONE -> R.drawable.ic_auto_repeat_one_pressed
            else -> R.drawable.ic_auto_repeat_normal
        }
        pscb.addCustomAction(CUSTOM_ACTION_REPEAT, getString(R.string.repeat_title), repeatResId)
        addCustomSpeedActions(pscb, settings.getBoolean("enable_android_auto_speed_buttons", false))
        addCustomSeekActions(pscb, settings.getBoolean("enable_android_auto_seek_buttons", false))
        return resultActions
    }

    private fun addCustomSeekActions(
        pscb: PlaybackStateCompat.Builder, showSeekActions: Boolean = true
    ) {
        if (!showSeekActions) return
        val ctx = applicationContext/* Rewind */
        pscb.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_REWIND,
                getString(R.string.playback_rewind),
                DrawableCache.getDrawableFromMemCache(
                    ctx, "ic_auto_rewind_${Settings.audioJumpDelay}", R.drawable.ic_auto_rewind
                )
            ).setExtras(Bundle().apply { putBoolean(WEARABLE_SHOW_CUSTOM_ACTION, true) }).build()
        )/* Fast Forward */
        pscb.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_FAST_FORWARD,
                getString(R.string.playback_forward),
                DrawableCache.getDrawableFromMemCache(
                    ctx, "ic_auto_forward_${Settings.audioJumpDelay}", R.drawable.ic_auto_forward
                )
            ).setExtras(Bundle().apply { putBoolean(WEARABLE_SHOW_CUSTOM_ACTION, true) }).build()
        )
    }

    private fun addCustomSpeedActions(
        pscb: PlaybackStateCompat.Builder, showSpeedActions: Boolean = true
    ) {
        if (speed != 1.0F || showSpeedActions) {
            val speedIcons = hashMapOf(
                0.50f to R.drawable.ic_auto_speed_0_50,
                0.80f to R.drawable.ic_auto_speed_0_80,
                1.00f to R.drawable.ic_auto_speed_1_00,
                1.10f to R.drawable.ic_auto_speed_1_10,
                1.20f to R.drawable.ic_auto_speed_1_20,
                1.50f to R.drawable.ic_auto_speed_1_50,
                2.00f to R.drawable.ic_auto_speed_2_00
            )
            val speedResId = speedIcons[speedIcons.keys.minByOrNull { abs(speed - it) }]
                ?: R.drawable.ic_auto_speed
            pscb.addCustomAction(
                CUSTOM_ACTION_SPEED, getString(R.string.playback_speed), speedResId
            )
        }
    }

    fun notifyTrackChanged() {
        updateMetadata()
        broadcastMetadata()
    }

    fun onMediaListChanged() {
        executeUpdate()
    }

    @MainThread
    fun next(force: Boolean = true) = playlistManager.next(force)

    @MainThread
    fun previous(force: Boolean) = playlistManager.previous(force)

    @MainThread
    fun shuffle() {
        playlistManager.shuffle()
        publishState()
    }

    private fun broadcastMetadata() {
        val media = playlistManager.getCurrentMedia()
        if (isVideoPlaying) return
        if (lifecycleScope.isActive) lifecycleScope.launch(Dispatchers.Default) {
            sendBroadcast(Intent("com.android.music.metachanged").putExtra(
                "track",
                media?.nowPlaying ?: media?.title
            ).putExtra(
                "artist", if (media != null) MediaUtils.getMediaArtist(
                    this@PlaybackService, media
                ) else null
            ).putExtra(
                "album", if (media != null) MediaUtils.getMediaAlbum(
                    this@PlaybackService, media
                ) else null
            ).putExtra("duration", media?.length ?: 0).putExtra("playing", isPlaying)
                .putExtra("package", "org.videolan.vlc").apply {
                    if (lastChaptersCount > 0) getCurrentChapter()?.let {
                        putExtra(
                            "chapter", it
                        )
                    }
                })
        }
    }

    private fun loadLastAudioPlaylist() {
        if (!AndroidDevices.isAndroidTv) loadLastPlaylist(PLAYLIST_TYPE_AUDIO)
    }

    fun loadLastPlaylist(type: Int) {
        forceForeground(true)
        if (!playlistManager.loadLastPlaylist(type)) {
            Toast.makeText(this, getString(R.string.resume_playback_error), Toast.LENGTH_LONG)
                .show()
            stopService(Intent(applicationContext, PlaybackService::class.java))
        }
    }

    fun showToast(text: String, duration: Int, isError: Boolean = false) {
        val msg = handler.obtainMessage().apply {
            what = SHOW_TOAST
            data = bundleOf("text" to text, "duration" to duration, "isError" to isError)
        }
        handler.removeMessages(SHOW_TOAST)
        handler.sendMessage(msg)
    }

    @MainThread
    fun canShuffle() = playlistManager.canShuffle()

    @MainThread
    fun hasMedia() = PlaylistManager.hasMedia()

    @MainThread
    fun hasPlaylist() = playlistManager.hasPlaylist()

    @MainThread
    fun addCallback(cb: Callback) = cbActor.trySend(CbAdd(cb))

    @MainThread
    fun removeCallback(cb: Callback) = cbActor.trySend(CbRemove(cb))

    private fun restartPlaylistManager() = playlistManager.restart()
    fun restartMediaPlayer() = playlistManager.player.restart()

    fun isValidIndex(positionInPlaylist: Int) = playlistManager.isValidPosition(positionInPlaylist)

    /**
     * Loads a selection of files (a non-user-supplied collection of media)
     * into the primary or "currently playing" playlist.
     *
     * @param mediaPathList A list of locations to load
     * @param position The position to start playing at
     */
    @MainThread
    private fun loadLocations(mediaPathList: List<String>, position: Int) =
        playlistManager.loadLocations(mediaPathList, position)

    @MainThread
    fun loadUri(uri: Uri?) = loadLocation(uri!!.toString())

    @MainThread
    fun loadLocation(mediaPath: String) = loadLocations(listOf(mediaPath), 0)

    @MainThread
    fun load(mediaList: Array<MediaWrapper>?, position: Int) {
        mediaList?.let { load(it.toList(), position) }
    }

    @MainThread
    fun load(mediaList: List<MediaWrapper>, position: Int) =
        lifecycleScope.launch { playlistManager.load(mediaList, position) }

    fun displayPlaybackError(@StringRes resId: Int) {
        if (!this@PlaybackService::mediaSession.isInitialized) initMediaSession()
        val ctx = this@PlaybackService
        if (isPlaying) {
            stop()
        }
        val playbackState =
            PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
                .setErrorMessage(PlaybackStateCompat.ERROR_CODE_NOT_SUPPORTED, ctx.getString(resId))
                .build()
        mediaSession.setPlaybackState(playbackState)
    }

    fun displayPlaybackMessage(@StringRes resId: Int, vararg formatArgs: String) {
        val ctx = this@PlaybackService
        subtitleMessage.push(ctx.getString(resId, *formatArgs))
        updateMetadata()
    }

    @MainThread
    fun load(media: MediaWrapper, position: Int = 0) = load(listOf(media), position)

    /**
     * Play a media from the media list (playlist)
     *
     * @param index The index of the media
     * @param flags LibVLC.MEDIA_* flags
     */
    @JvmOverloads
    fun playIndex(index: Int, flags: Int = 0) {
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playlistManager.playIndex(
                index, flags
            )
        }
    }

    fun playIndexOrLoadLastPlaylist(index: Int) {
        if (hasMedia()) playIndex(index)
        else {
            settings.edit {
                putLong(POSITION_IN_SONG, 0L)
                putInt(POSITION_IN_AUDIO_LIST, index)
            }
            loadLastPlaylist(PLAYLIST_TYPE_AUDIO)
        }
    }

    @MainThread
    fun flush() {/* HACK: flush when activating a video track. This will force an
         * I-Frame to be displayed right away. */
        if (isSeekable) {
            val time = getTime()
            if (time > 0) seek(time)
        }
    }

    /**
     * Use this function to show an URI in the audio interface WITHOUT
     * interrupting the stream.
     *
     * Mainly used by VideoPlayerActivity in response to loss of video track.
     */

    @MainThread
    fun showWithoutParse(index: Int, forPopup: Boolean = false) {
        playlistManager.setVideoTrackEnabled(false)
        val media = playlistManager.getMedia(index) ?: return
        // Show an URI without interrupting/losing the current stream
        if (BuildConfig.DEBUG) Log.v(
            TAG, "Showing index " + index + " with playing URI " + media.uri
        )
        playlistManager.currentIndex = index
        notifyTrackChanged()
        PlaylistManager.showAudioPlayer.value = !isVideoPlaying && !forPopup
        showNotification()
    }

    fun setVideoTrackEnabled(enabled: Boolean) = playlistManager.setVideoTrackEnabled(enabled)

    fun switchToVideo() = playlistManager.switchToVideo()


    /**
     * Append to the current existing playlist
     */

    @MainThread
    fun append(mediaList: Array<MediaWrapper>, index: Int = 0) = append(mediaList.toList(), index)

    @MainThread
    fun append(mediaList: List<MediaWrapper>, index: Int = 0) =
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            playlistManager.append(mediaList, index)
            onMediaListChanged()
        }

    @MainThread
    fun append(media: MediaWrapper) = append(listOf(media))

    /**
     * Insert into the current existing playlist
     */

    @MainThread
    fun insertNext(mediaList: Array<MediaWrapper>) = insertNext(mediaList.toList())

    @MainThread
    private fun insertNext(mediaList: List<MediaWrapper>) {
        playlistManager.insertNext(mediaList)
        onMediaListChanged()
    }

    @MainThread
    fun insertNext(media: MediaWrapper) = insertNext(listOf(media))

    /**
     * Move an item inside the playlist.
     */
    @MainThread
    fun moveItem(positionStart: Int, positionEnd: Int) =
        playlistManager.moveItem(positionStart, positionEnd)

    @MainThread
    fun insertItem(position: Int, mw: MediaWrapper) = playlistManager.insertItem(position, mw)

    @MainThread
    fun remove(position: Int) = playlistManager.remove(position)

    @MainThread
    fun removeLocation(location: String) = playlistManager.removeLocation(location)

    @MainThread
    operator fun hasNext() = playlistManager.hasNext()

    @MainThread
    fun hasPrevious() = playlistManager.hasPrevious()

    @MainThread
    fun detectHeadset(enable: Boolean) {
        detectHeadset = enable
    }

    @MainThread
    fun setRate(rate: Float, save: Boolean) {
        playlistManager.player.setRate(rate, save)
        publishState()
    }

    @MainThread
    fun increaseRate() {
        if (rate < 4) setRate(rate + 0.2F, true)
    }

    @MainThread
    fun decreaseRate() {
        if (rate > 0.4) setRate(rate - 0.2F, true)
    }

    @MainThread
    fun resetRate() {
        setRate(1F, true)
    }

    @MainThread
    fun navigate(where: Int) = playlistManager.player.navigate(where)

    @MainThread
    fun getChapters(title: Int) = playlistManager.player.getChapters(title)

    @MainThread
    fun setVolume(volume: Int) = playlistManager.player.setVolume(volume)

    @MainThread
    @JvmOverloads
    fun seek(
        time: Long,
        length: Double = this.length.toDouble(),
        fromUser: Boolean = false,
        fast: Boolean = false
    ) {
        if (length > 0.0) this.setTime(
            time, fast
        ) else setPosition((time.toFloat() / NO_LENGTH_PROGRESS_MAX.toFloat()))
        if (fromUser) {
            publishState(time)
        }
    }

    @MainThread
    fun updateViewpoint(
        yaw: Float, pitch: Float, roll: Float, fov: Float, absolute: Boolean
    ): Boolean {
        return playlistManager.player.updateViewpoint(yaw, pitch, roll, fov, absolute)
    }

    @MainThread
    fun saveStartTime(time: Long) {
        playlistManager.savedTime = time
    }

    @MainThread
    private fun setPosition(pos: Float) = playlistManager.player.setPosition(pos)


    @MainThread
    fun setAudioDigitalOutputEnabled(enabled: Boolean) =
        playlistManager.player.setAudioDigitalOutputEnabled(enabled)


    @MainThread
    fun addSubtitleTrack(path: String, select: Boolean) =
        playlistManager.player.addSubtitleTrack(path, select)

    @MainThread
    fun addSubtitleTrack(uri: Uri, select: Boolean) =
        playlistManager.player.addSubtitleTrack(uri, select)

    @MainThread
    fun setSpuTrack(index: String) = playlistManager.setSpuTrack(index)

    @MainThread
    fun setAudioDelay(delay: Long) = playlistManager.setAudioDelay(delay)

    @MainThread
    fun setSpuDelay(delay: Long) = playlistManager.setSpuDelay(delay)

    @MainThread
    fun hasRenderer() = playlistManager.player.hasRenderer

    @MainThread
    fun setRenderer(item: RendererItem?) {
        val wasOnRenderer = hasRenderer()
        playlistManager.setRenderer(item)
        if (!wasOnRenderer && item != null) audioFocusHelper.changeAudioFocus(false)
        else if (wasOnRenderer && item == null && isPlaying) audioFocusHelper.changeAudioFocus(true)
    }

    @MainThread
    fun setEqualizer(equalizer: MediaPlayer.Equalizer?) =
        playlistManager.player.setEqualizer(equalizer)

    @MainThread
    fun setVideoScale(scale: Float) = playlistManager.player.setVideoScale(scale)

    @MainThread
    fun setVideoAspectRatio(aspect: String?) = playlistManager.player.setVideoAspectRatio(aspect)


    /**
     * Start the loop that checks for the sleep timer consumption
     */
    private fun startSleepTimerJob() {
        stopSleepTimerJob()
        sleepTimerJob = launch {
            while (isActive) {
                playerSleepTime.value?.let {
                    val timerExpired = System.currentTimeMillis() > it.timeInMillis
                    val shouldStop =
                        if (waitForMediaEnd) timerExpired && mediaEndReached else timerExpired
                    if (shouldStop) {
                        withContext(Dispatchers.Main) {
                            if (isPlaying) stop() else setSleepTimer(
                                null
                            )
                        }
                    }
                }
                if (mediaEndReached) mediaEndReached = false
                delay(1000)
            }
        }
    }

    private fun stopSleepTimerJob() {
        if (BuildConfig.DEBUG) Log.d("SleepTimer", "stopSleepTimerJob")
        sleepTimerJob?.cancel()
        sleepTimerJob = null
    }

    /**
     * Change the sleep timer time
     * @param time a [Calendar] object for the new sleep timer time. Set to null to cancel the sleep timer
     */
    fun setSleepTimer(time: Calendar?) {
        if (time != null && time.timeInMillis < System.currentTimeMillis()) return
        playerSleepTime.value = time
        if (time == null) stopSleepTimerJob() else startSleepTimerJob()
    }

    companion object {
        val serviceFlow = MutableStateFlow<PlaybackService?>(null)
        val instance: PlaybackService?
            get() = serviceFlow.value

        val renderer = RendererLiveData()
        val restartPlayer = LiveEvent<Boolean>()
        val headSetDetection = LiveEvent<Boolean>()
        val equalizer = LiveEvent<MediaPlayer.Equalizer>()

        private const val SHOW_TOAST = 1
        private const val END_MEDIASESSION = 2

        val playerSleepTime by lazy(LazyThreadSafetyMode.NONE) {
            MutableLiveData<Calendar?>().apply {
                value = null
            }
        }

        fun start(context: Context) {
            if (instance != null) return
            val serviceIntent = Intent(context, PlaybackService::class.java)
            context.launchForeground(serviceIntent)
        }

        fun loadLastAudio(context: Context) {
            val i = Intent(ACTION_REMOTE_LAST_PLAYLIST, null, context, PlaybackService::class.java)
            context.launchForeground(i)
        }

        fun hasRenderer() = renderer.value != null

        private const val PLAYBACK_BASE_ACTIONS =
            (PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or PlaybackStateCompat.ACTION_PLAY_FROM_URI or PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)
    }

    fun getTime(realTime: Long): Int {
        playlistManager.abRepeat.value?.let {
            if (it.start != -1L && it.stop != -1L) return when {
                playlistManager.abRepeatOn.value!! -> {
                    val start = it.start
                    val end = it.stop
                    when {
                        start != -1L && realTime < start -> {
                            start.toInt()
                        }

                        end != -1L && realTime > it.stop -> {
                            end.toInt()
                        }

                        else -> realTime.toInt()
                    }
                }

                else -> realTime.toInt()
            }
        }

        return if (length == 0L) position.toInt() else realTime.toInt()
    }

    override fun getLifecycle(): Lifecycle {
        return dispatcher.lifecycle
    }

}

// Actor actions sealed classes
private sealed class CbAction

private object CbUpdate : CbAction()
private class CbMediaEvent(val event: IMedia.Event) : CbAction()
private class CbMediaPlayerEvent(val event: MediaPlayer.Event) : CbAction()
private class CbAdd(val cb: PlaybackService.Callback) : CbAction()
private class CbRemove(val cb: PlaybackService.Callback) : CbAction()
private object ShowNotification : CbAction()
private class HideNotification(val remove: Boolean) : CbAction()
private object UpdateMeta : CbAction()

fun PlaybackService.manageAbRepeatStep(
    abRepeatReset: View, abRepeatStop: View, abRepeatContainer: View, abRepeatAddMarker: TextView
) {
    when {
        playlistManager.abRepeatOn.value != true -> {
            abRepeatReset.visibility = View.GONE
            abRepeatStop.visibility = View.GONE
            abRepeatContainer.visibility = View.GONE
        }

        playlistManager.abRepeat.value?.start != -1L && playlistManager.abRepeat.value?.stop != -1L -> {
            abRepeatReset.visibility = View.VISIBLE
            abRepeatStop.visibility = View.VISIBLE
            abRepeatContainer.visibility = View.GONE
        }

        playlistManager.abRepeat.value?.start == -1L && playlistManager.abRepeat.value?.stop == -1L -> {
            abRepeatContainer.visibility = View.VISIBLE
            abRepeatAddMarker.text = getString(R.string.abrepeat_add_first_marker)
            abRepeatReset.visibility = View.GONE
            abRepeatStop.visibility = View.GONE
        }

        playlistManager.abRepeat.value?.start == -1L || playlistManager.abRepeat.value?.stop == -1L -> {
            abRepeatAddMarker.text = getString(R.string.abrepeat_add_second_marker)
            abRepeatContainer.visibility = View.VISIBLE
            abRepeatReset.visibility = View.GONE
            abRepeatStop.visibility = View.GONE
        }
    }
}

