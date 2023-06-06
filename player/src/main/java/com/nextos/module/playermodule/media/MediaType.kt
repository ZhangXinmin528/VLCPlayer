package com.nextos.module.playermodule.media

sealed class MediaType {
    object TypeAudio:MediaType()
    object TypeVideo:MediaType()
    object TypeVideoUrl:MediaType()
    object TypeAudioUrl:MediaType()
}