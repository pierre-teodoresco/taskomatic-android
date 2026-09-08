package com.pierreteodoresco.taskomatic.ui

import androidx.compose.runtime.saveable.Saver
import com.pierreteodoresco.taskomatic.data.TextStateStorage

fun textStateSaver(storage: TextStateStorage, key: String, onFailure: () -> Unit) = Saver<String, String>(
    save = { value ->
        try { storage.save(value, key) }
        catch (_: Exception) { onFailure(); "E" }
    },
    restore = { value ->
        try { storage.restore(value) }
        catch (_: Exception) { onFailure(); null }
    },
)
