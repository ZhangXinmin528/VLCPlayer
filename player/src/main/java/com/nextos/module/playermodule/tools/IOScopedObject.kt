package com.nextos.module.playermodule.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

open class IOScopedObject : CoroutineScope {
    override val coroutineContext = Dispatchers.IO
}