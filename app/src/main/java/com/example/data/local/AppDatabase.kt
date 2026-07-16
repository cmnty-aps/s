package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.CodeFile
import com.example.data.model.EditorSetting
import com.example.data.model.Plugin
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeFileDao {
    @Query("SELECT * FROM code_files ORDER BY lastModified DESC")
    fun getAllFiles(): Flow<List<CodeFile>>

    @Query("SELECT * FROM code_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Int): CodeFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: CodeFile): Long

    @Update
    suspend fun updateFile(file: CodeFile)

    @Delete
    suspend fun deleteFile(file: CodeFile)
}

@Dao
interface PluginDao {
    @Query("SELECT * FROM plugins")
    fun getAllPlugins(): Flow<List<Plugin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: Plugin)

    @Update
    suspend fun updatePlugin(plugin: Plugin)
}

@Dao
interface EditorSettingDao {
    @Query("SELECT value FROM editor_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT * FROM editor_settings")
    fun getAllSettings(): Flow<List<EditorSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: EditorSetting)
}

@Database(entities = [CodeFile::class, Plugin::class, EditorSetting::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun codeFileDao(): CodeFileDao
    abstract fun pluginDao(): PluginDao
    abstract fun editorSettingDao(): EditorSettingDao
}
