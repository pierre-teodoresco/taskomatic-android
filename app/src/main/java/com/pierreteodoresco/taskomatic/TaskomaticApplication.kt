package com.pierreteodoresco.taskomatic

import android.app.Application
import com.pierreteodoresco.taskomatic.data.TaskStore

class TaskomaticApplication : Application() {
    val store by lazy { TaskStore(this) }
}
