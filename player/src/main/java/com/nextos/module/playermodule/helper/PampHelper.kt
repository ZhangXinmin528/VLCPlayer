package com.nextos.module.playermodule.helper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*


class PampHelper internal constructor(var mContext: Context, callback: Callback?) {
    var mService: Messenger? = null
    var mReplyToMessenger: Messenger? = null

    interface Callback {
        fun updateStatusFromBundle(bundle: Bundle?)
        fun updateStatusFromArg(type: Int, value: Int)
    }

    fun setCallback(callback: Callback?) {
        mCallback = mCallback
    }

    fun bringUpService() {
        if (mReplyToMessenger == null) {
            mReplyToMessenger = Messenger(mIncomingHandler)
        }
        val intent = Intent()
        val compName = ComponentName(PAMPCRTL_PKG, PAMPCRTL_SERVICE)
        intent.component = compName
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    fun putDownService() {
        mContext.unbindService(mConnection)
        mReplyToMessenger = null
    }

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = Messenger(service)
            sendCommand(GET_STATUS, 0)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
        }
    }

    fun sendCommand(what: Int, arg: Int) {
        if (mCallback == null) {
            return
        }
        if (mService != null) {
            val msg = Message.obtain()
            msg.what = what
            msg.arg1 = arg
            msg.replyTo = mReplyToMessenger
            try {
                mService!!.send(msg)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }
    }

    init {
        mCallback = callback
    }

    companion object {
        //////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////
        private const val COMMAND_START = 100
        const val GET_EQMODE = COMMAND_START + 2
        const val GET_MAINVOL = COMMAND_START + 10
        const val GET_STATUS = COMMAND_START + 7
        const val SET_MAINVOL = COMMAND_START + 11
        const val SET_EQMODE = COMMAND_START + 3
        const val SET_BASSVOL = COMMAND_START + 100
        const val SET_TREBLEVOL = COMMAND_START + 101
        const val SET_CENTERVOL = COMMAND_START + 102
        const val SET_SIDEVOL = COMMAND_START + 103
        const val SET_POWERON = COMMAND_START
        const val DO_UPGRADE = COMMAND_START + 6
        const val SET_RESET = COMMAND_START + 4
        const val SET_SOURCE = COMMAND_START + 9
        const val SET_MUTE = COMMAND_START + 13
        const val SET_UPMIX = COMMAND_START + 15

        // default values
        const val DEFAULT_EQ_MODE = 1
        const val DEFAULT_MAIN_VOL = 50
        const val DEFAULT_EQ_VOL = 6

        //not support yet
        const val SET_STANDBY_TIME = COMMAND_START + 16
        const val KEY_EQ_MODE = "eq_mode"
        const val KEY_VOL_MAIN = "volume_main"
        const val KEY_VOL_BASS = "volume_bass"
        const val KEY_VOL_CENTER = "volume_center"
        const val KEY_VOL_SIDE = "volume_side"
        const val KEY_VOL_TREBLE = "volume_treble"

        const val KEY_POWERON = "power_on"
        const val KEY_SOURCE = "source"
        const val KEY_MCU_VER = "fireware_version"
        const val KEY_MUTEON = "mute_on"
        const val KEY_UPMIXON = "upmix_on"

        private const val PAMPCRTL_PKG = "com.android.pampcontroller"
        private const val PAMPCRTL_SERVICE = "com.android.pampcontroller.PampService"
        private const val TAG = "Main"
        var mCallback: Callback? = null
        var mIncomingHandler: Handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    GET_STATUS -> {
                        val bundle = msg.data
                        if (bundle != null) {
                            mCallback!!.updateStatusFromBundle(bundle)
                        }
                    }
                    GET_EQMODE, GET_MAINVOL, SET_MUTE, SET_EQMODE, SET_UPMIX, SET_MAINVOL, SET_SOURCE, SET_BASSVOL, SET_TREBLEVOL, SET_CENTERVOL, SET_SIDEVOL -> mCallback!!.updateStatusFromArg(
                        msg.what,
                        msg.arg1
                    )
                    else -> {}
                }
            }
        }
    }
}