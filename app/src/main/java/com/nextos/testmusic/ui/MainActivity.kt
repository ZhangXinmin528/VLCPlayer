package com.nextos.testmusic.ui

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.DIRECTORY_MUSIC
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.nextos.core.BaseActivity
import com.nextos.module.playermodule.media.MediaUtils
import com.nextos.module.playermodule.service.PlaybackService
import com.nextos.module.playermodule.tools.toast
import com.nextos.testmusic.R
import com.nextos.testmusic.databinding.ActivityMainBinding
import com.zxm.utils.core.dialog.DialogUtil
import com.zxm.utils.core.file.FileUtils
import com.zxm.utils.core.permission.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.videolan.libvlc.MediaPlayer.Event
import java.io.File
import java.util.logging.Logger

open class MainActivity : BaseActivity(), OnClickListener, PlaybackService.Callback {

    private lateinit var mainBinding: ActivityMainBinding
    private var service: PlaybackService? = null
    private lateinit var startedScope: CoroutineScope
    override fun setContentLayout(): Any {
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        return mainBinding.root
    }


    override fun initParamsAndValues() {

    }

    override fun initViews() {
        mainBinding.btnPermission.setOnClickListener(this)
        mainBinding.btnPlayMusic.setOnClickListener(this)

    }

    override fun onStart() {
        super.onStart()
        startedScope = MainScope()
        PlaybackService.start(this)
        PlaybackService.serviceFlow.onEach { onServiceChanged(it) }.launchIn(startedScope)
    }

    private fun setOption() {

    }

    open fun onServiceChanged(service: PlaybackService?) {
        if (service != null) {
            this.service = service

            service.addCallback(this)

            //if (isTalkbackIsEnabled()) overlayDelegate.showOverlayTimeout(OVERLAY_INFINITE)
        } else if (this.service != null) {

        }
    }

    /**
     * check permission
     */
    private fun checkSinglePermission() {
        if (!PermissionChecker.checkPersmission(
                mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            PermissionChecker.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_EXTERNAL
            )
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_permission -> {
                checkSinglePermission()
            }

            R.id.btn_play_music -> {
                if (!isExternalStorageWritable()) {
                    Toast.makeText(this, "权限未授予", Toast.LENGTH_SHORT).show()
                    return
                }
//                val dir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
//                val songPath = File(dir, "f1_bass_41_43.ac3").absolutePath
//                if (!FileUtils.isFileExists(songPath)) {
//                    Toast.makeText(this, "歌曲不存在", Toast.LENGTH_SHORT).show()
//                    return
//                }
//                val songUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/f1_bass_41_43.ac3")
//                val songUri = Uri.parse("file:///android_asset//f1_bass_41_43.ac3")

                //共享目录
                val dir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC)
                val songPath = File(dir, "f1_bass_41_43.ac3").absolutePath
                if (!FileUtils.isFileExists(songPath)) {
                    Toast.makeText(this, "歌曲不存在", Toast.LENGTH_SHORT).show()
                    return
                }

                //私有目录
//                val dir = cacheDir
//                val songPath = File(dir, "f1_bass_41_43.ac3").absolutePath
//                if (!FileUtils.isFileExists(songPath)) {
//                    Toast.makeText(this, "歌曲不存在", Toast.LENGTH_SHORT).show()
//                    return
//                }
                val songUri = Uri.parse(songPath)
                MediaUtils.openMediaNoUi(songUri)

            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (grantResults != null) {
            val size = grantResults.size
            for (i in 0 until size) {
                val grantResult = grantResults[i]
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    val showRequest = ActivityCompat.shouldShowRequestPermissionRationale(
                        this, permissions[i]
                    )
                    if (showRequest) {
                        DialogUtil.showForceDialog(
                            mContext!!, PermissionChecker.matchRequestPermissionRationale(
                                mContext, permissions[i]
                            )
                        ) { dialog, which -> }
                    }
                } else {
                    //do something
                }

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun update() {

    }

    override fun onMediaEvent(event: org.videolan.libvlc.interfaces.IMedia.Event) {

    }

    override fun onMediaPlayerEvent(event: org.videolan.libvlc.MediaPlayer.Event) {
        Log.d("zxm==", "onMediaPlayerEvent..event#type:${event.type}")
        if (event.type == Event.EndReached) {
            Toast.makeText(this, "播放完毕", Toast.LENGTH_SHORT).show()
        }
    }
}

private const val REQUEST_EXTERNAL = 1001

private fun isExternalStorageWritable(): Boolean {
    val state = Environment.getExternalStorageState()
    return Environment.MEDIA_MOUNTED == state
}
