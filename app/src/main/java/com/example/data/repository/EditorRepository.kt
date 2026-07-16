package com.example.data.repository

import android.util.Base64
import com.example.data.local.CodeFileDao
import com.example.data.local.EditorSettingDao
import com.example.data.local.PluginDao
import com.example.data.model.CodeFile
import com.example.data.model.EditorSetting
import com.example.data.model.Plugin
import com.example.data.network.GitHubApiService
import com.example.data.network.GitHubPutBody
import kotlinx.coroutines.flow.Flow
import java.nio.charset.StandardCharsets

class EditorRepository(
    private val codeFileDao: CodeFileDao,
    private val pluginDao: PluginDao,
    private val settingDao: EditorSettingDao,
    private val gitHubService: GitHubApiService
) {
    val allFiles: Flow<List<CodeFile>> = codeFileDao.getAllFiles()
    val allPlugins: Flow<List<Plugin>> = pluginDao.getAllPlugins()
    val allSettings: Flow<List<EditorSetting>> = settingDao.getAllSettings()

    suspend fun getFileById(id: Int): CodeFile? = codeFileDao.getFileById(id)

    suspend fun insertFile(file: CodeFile): Long = codeFileDao.insertFile(file)

    suspend fun updateFile(file: CodeFile) = codeFileDao.updateFile(file)

    suspend fun deleteFile(file: CodeFile) = codeFileDao.deleteFile(file)

    suspend fun insertPlugin(plugin: Plugin) = pluginDao.insertPlugin(plugin)

    suspend fun updatePlugin(plugin: Plugin) = pluginDao.updatePlugin(plugin)

    suspend fun getSettingValue(key: String): String? = settingDao.getSettingValue(key)

    suspend fun saveSetting(key: String, value: String) {
        settingDao.insertSetting(EditorSetting(key, value))
    }

    suspend fun pushFileToGitHub(
        file: CodeFile,
        token: String,
        owner: String,
        repo: String,
        path: String
    ): Result<CodeFile> {
        return try {
            val authToken = if (token.startsWith("Bearer ") || token.startsWith("token ")) token else "token $token"
            val filePath = path.ifEmpty { file.name }
            
            // 1. Check if the file exists to get its SHA
            var sha: String? = null
            val checkResponse = gitHubService.getFileContent(authToken, owner, repo, filePath)
            if (checkResponse.isSuccessful) {
                sha = checkResponse.body()?.sha
            }

            // 2. Base64 encode the content
            val bytes = file.content.toByteArray(StandardCharsets.UTF_8)
            val base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // 3. Push content
            val pushBody = GitHubPutBody(
                message = "Sync file ${file.name} via Code Editor Android app",
                content = base64Content,
                sha = sha
            )
            val pushResponse = gitHubService.pushFileContent(authToken, owner, repo, filePath, pushBody)

            if (pushResponse.isSuccessful) {
                val updatedFile = file.copy(
                    githubRepo = "$owner/$repo",
                    githubPath = filePath,
                    isSynced = true,
                    lastModified = System.currentTimeMillis()
                )
                codeFileDao.updateFile(updatedFile)
                Result.success(updatedFile)
            } else {
                val errorMsg = pushResponse.errorBody()?.string() ?: "Push failed with code ${pushResponse.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullFileFromGitHub(
        file: CodeFile,
        token: String,
        owner: String,
        repo: String,
        path: String
    ): Result<CodeFile> {
        return try {
            val authToken = if (token.startsWith("Bearer ") || token.startsWith("token ")) token else "token $token"
            val filePath = path.ifEmpty { file.name }

            val response = gitHubService.getFileContent(authToken, owner, repo, filePath)
            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Response body is empty")
                val cleanBase64 = body.content?.replace("\n", "")?.replace("\r", "")?.replace(" ", "") ?: ""
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val decodedContent = String(decodedBytes, StandardCharsets.UTF_8)

                val updatedFile = file.copy(
                    content = decodedContent,
                    githubRepo = "$owner/$repo",
                    githubPath = filePath,
                    isSynced = true,
                    lastModified = System.currentTimeMillis()
                )
                codeFileDao.updateFile(updatedFile)
                Result.success(updatedFile)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Pull failed with code ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
