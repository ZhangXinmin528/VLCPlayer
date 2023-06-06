package com.nextos.module.playermodule.interfaces

interface AudioListItemAdapter : TvFocusableAdapter {
    fun submitList(pagedList: Any?)
    fun isEmpty() : Boolean
    var focusNext: Int
    fun displaySwitch(inGrid: Boolean)
}
