package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "code_files")
data class CodeFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val content: String,
    val language: String,
    val lastModified: Long = System.currentTimeMillis(),
    val githubRepo: String = "",
    val githubPath: String = "",
    val isSynced: Boolean = false
)

@Entity(tableName = "plugins")
data class Plugin(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean = false,
    val category: String = "Formatting"
)

@Entity(tableName = "editor_settings")
data class EditorSetting(
    @PrimaryKey val key: String,
    val value: String
)
