package com.nextos.eapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.nextos.core.BaseActivity
import com.nextos.eapp.databinding.ActivityMainBinding
import com.nextos.eplayer.media.MLServiceLocator
import com.nextos.eplayer.media.OnPlayStateChangedListener
import com.nextos.eplayer.media.PlayerController
import com.nextos.eplayer.media.media.MediaWrapper
import com.nextos.eplayer.media.Settings
import com.nextos.eplayer.media.VLCInstance
import com.nextos.eplayer.media.VLCOptions
import com.zxm.utils.core.dialog.DialogUtil
import com.zxm.utils.core.file.FileUtils
import com.zxm.utils.core.permission.PermissionChecker
import com.zxm.utils.core.phone.PhoneUtil.isExternalStorageWritable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.FactoryManager
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMediaFactory
import java.io.File

private const val REQUEST_EXTERNAL = 1001

class MainActivity : BaseActivity(), View.OnClickListener, OnPlayStateChangedListener {
    private lateinit var mainBinding: ActivityMainBinding
    private var playerController: PlayerController? = null

    override fun setContentLayout(): Any {
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        return mainBinding.root
    }

    override fun initParamsAndValues() {
        playerController = PlayerController(applicationContext)
        playerController?.setOnPlayStateChangedListener(this)
    }

    override fun initViews() {
        mainBinding.btnPermission.setOnClickListener(this)
        mainBinding.btnDirectPlay.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_permission -> {
                checkSinglePermission()
            }

            R.id.btn_direct_play -> {

                if (!isExternalStorageWritable()) {
                    Toast.makeText(this, "权限未授予", Toast.LENGTH_SHORT).show()
                    return
                }
                //共享目录
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val songPath = File(dir, "f1_bass_41_43.ac3").absolutePath
//                val songPath = File(dir, "父亲写的散文诗-许飞.flac").absolutePath

                lifecycleScope.launch {
                    playerController?.playSound(songPath)
                }
            }
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

    override fun onPlayCompleted() {
        Toast.makeText(this, "播放完成", Toast.LENGTH_SHORT).show()
    }

}
