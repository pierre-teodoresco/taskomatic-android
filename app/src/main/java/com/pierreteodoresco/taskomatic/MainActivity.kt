package com.pierreteodoresco.taskomatic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pierreteodoresco.taskomatic.ui.TaskomaticApp
import com.pierreteodoresco.taskomatic.ui.TaskomaticTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TaskomaticTheme { TaskomaticApp(viewModel()) } }
    }
}
