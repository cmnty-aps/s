package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.network.GitHubRetrofitClient
import com.example.data.repository.EditorRepository
import com.example.ui.editor.EditorScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: EditorRepository
    private lateinit var viewModel: EditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SQLite Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "code_editor_db"
        )
        .fallbackToDestructiveMigration() // safe migration strategy for low-spec sandbox devices
        .build()

        repository = EditorRepository(
            codeFileDao = database.codeFileDao(),
            pluginDao = database.pluginDao(),
            settingDao = database.editorSettingDao(),
            gitHubService = GitHubRetrofitClient.service
        )

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(repository) as T
            }
        }

        viewModel = ViewModelProvider(this, factory)[EditorViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                EditorScreen(viewModel = viewModel)
            }
        }
    }
}

