package com.pierreteodoresco.taskomatic

import android.app.Application
import com.pierreteodoresco.taskomatic.data.TaskStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class TaskomaticApplication : Application() {
    val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val store by lazy { TaskStore(this) }
}
