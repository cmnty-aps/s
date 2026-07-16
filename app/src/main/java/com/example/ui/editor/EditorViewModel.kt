package com.example.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.model.CodeFile
import com.example.data.model.Plugin
import com.example.data.repository.EditorRepository
import com.example.ui.components.EditorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import java.util.regex.Matcher

import androidx.documentfile.provider.DocumentFile

data class FileNode(
    val name: String,
    val path: String, // Absolute path or Uri String
    val isDirectory: Boolean,
    val isExpanded: Boolean = false,
    val level: Int = 0,
    val size: Long = 0,
    val children: List<FileNode> = emptyList(),
    val isSaf: Boolean = false
)

class EditorViewModel(private val repository: EditorRepository) : ViewModel() {

    // Workspace Directories and Files state (replaces manual imports with real filesystem)
    private val _workspacePath = MutableStateFlow("Files Explorer")
    val workspacePath: StateFlow<String> = _workspacePath.asStateFlow()

    private val _workspaceFiles = MutableStateFlow<List<FileNode>>(emptyList())
    val workspaceFiles: StateFlow<List<FileNode>> = _workspaceFiles.asStateFlow()

    private val _expandedPaths = mutableSetOf<String>()

    // Local Files from database (remains for backward compatibility if needed)
    val files: StateFlow<List<CodeFile>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _openFiles = MutableStateFlow<List<CodeFile>>(emptyList())
    val openFiles: StateFlow<List<CodeFile>> = _openFiles.asStateFlow()

    private val _selectedFile = MutableStateFlow<CodeFile?>(null)
    val selectedFile: StateFlow<CodeFile?> = _selectedFile.asStateFlow()

    // Third Party Plugins
    val plugins: StateFlow<List<Plugin>> = repository.allPlugins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings map
    private val _settings = MutableStateFlow<Map<String, String>>(emptyMap())
    val settings: StateFlow<Map<String, String>> = _settings.asStateFlow()

    // Terminal State
    private val _terminalOutput = MutableStateFlow(
        """
  ___  _  _  _  _  ___ _ _
 /  _|| \/ || \| ||_ _\ V /
 \___/|_||_||_|\_||___/|_|

Developer Terminal v1.0
Ketik 'shh' untuk akses terminal.

""".trimIndent()
    )
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    // Status loadings
    fun getBundledHtml(context: Context, file: CodeFile): String {
        var html = file.content
        val path = file.githubPath
        if (path.isEmpty()) return html
        
        val parentPath = path.substringBeforeLast("/", "")
        if (parentPath.isEmpty()) return html

        // 1. Resolve CSS
        try {
            val linkPattern = Pattern.compile("<link[^>]+href=[\"']([^\"']+\\.css)[\"'][^>]*>", Pattern.CASE_INSENSITIVE)
            val linkMatcher = linkPattern.matcher(html)
            val htmlWithInjectedCss = StringBuffer()
            while (linkMatcher.find()) {
                val href = linkMatcher.group(1)
                if (!href.startsWith("http") && !href.startsWith("//") && !href.startsWith("data:")) {
                    val cssContent = readFileRelativeTo(context, parentPath, href)
                    if (cssContent != null) {
                        linkMatcher.appendReplacement(htmlWithInjectedCss, Matcher.quoteReplacement("<style>\n$cssContent\n</style>"))
                    } else {
                        linkMatcher.appendReplacement(htmlWithInjectedCss, linkMatcher.group(0))
                    }
                } else {
                    linkMatcher.appendReplacement(htmlWithInjectedCss, linkMatcher.group(0))
                }
            }
            linkMatcher.appendTail(htmlWithInjectedCss)
            html = htmlWithInjectedCss.toString()
        } catch (e: Exception) {}

        // 2. Resolve JS
        try {
            val scriptPattern = Pattern.compile("<script[^>]+src=[\"']([^\"']+\\.js)[\"'][^>]*>\\s*</script>", Pattern.CASE_INSENSITIVE)
            val scriptMatcher = scriptPattern.matcher(html)
            val htmlWithInjectedJs = StringBuffer()
            while (scriptMatcher.find()) {
                val src = scriptMatcher.group(1)
                if (!src.startsWith("http") && !src.startsWith("//") && !src.startsWith("data:")) {
                    val jsContent = readFileRelativeTo(context, parentPath, src)
                    if (jsContent != null) {
                        scriptMatcher.appendReplacement(htmlWithInjectedJs, Matcher.quoteReplacement("<script>\n$jsContent\n</script>"))
                    } else {
                        scriptMatcher.appendReplacement(htmlWithInjectedJs, scriptMatcher.group(0))
                    }
                } else {
                    scriptMatcher.appendReplacement(htmlWithInjectedJs, scriptMatcher.group(0))
                }
            }
            scriptMatcher.appendTail(htmlWithInjectedJs)
            html = htmlWithInjectedJs.toString()
        } catch (e: Exception) {}

        // 3. Resolve Images and other SRCS (Base64)
        try {
            val srcPattern = Pattern.compile("(src|href)=[\"']([^\"']+\\.(png|jpg|jpeg|gif|svg|webp))[\"']", Pattern.CASE_INSENSITIVE)
            val srcMatcher = srcPattern.matcher(html)
            val htmlWithInjectedAssets = StringBuffer()
            while (srcMatcher.find()) {
                val attr = srcMatcher.group(1)
                val src = srcMatcher.group(2)
                val ext = srcMatcher.group(3).lowercase()
                
                if (!src.startsWith("http") && !src.startsWith("//") && !src.startsWith("data:")) {
                    val bytes = readFileBytesRelativeTo(context, parentPath, src)
                    if (bytes != null) {
                        val mimeType = when(ext) {
                            "svg" -> "image/svg+xml"
                            "jpg", "jpeg" -> "image/jpeg"
                            else -> "image/$ext"
                        }
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val dataUri = "data:$mimeType;base64,$base64"
                        srcMatcher.appendReplacement(htmlWithInjectedAssets, Matcher.quoteReplacement("$attr=\"$dataUri\""))
                    } else {
                        srcMatcher.appendReplacement(htmlWithInjectedAssets, srcMatcher.group(0))
                    }
                } else {
                    srcMatcher.appendReplacement(htmlWithInjectedAssets, srcMatcher.group(0))
                }
            }
            srcMatcher.appendTail(htmlWithInjectedAssets)
            html = htmlWithInjectedAssets.toString()
        } catch (e: Exception) {}
        
        return html
    }

    private fun readFileRelativeTo(context: Context, parentPath: String, relativePath: String): String? {
        return try {
            if (parentPath.startsWith("content://")) {
                null // SAF fallback
            } else {
                val file = File(parentPath, relativePath)
                if (file.exists()) file.readText() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readFileBytesRelativeTo(context: Context, parentPath: String, relativePath: String): ByteArray? {
        return try {
            if (parentPath.startsWith("content://")) {
                null // SAF fallback
            } else {
                val file = File(parentPath, relativePath)
                if (file.exists()) file.readBytes() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Auto-completion suggestions
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    // Custom background image bitmap state for fast rendering on spec devices
    private val _backgroundImage = MutableStateFlow<Bitmap?>(null)
    val backgroundImage: StateFlow<Bitmap?> = _backgroundImage.asStateFlow()

    private val _backgroundVideoUri = MutableStateFlow<Uri?>(null)
    val backgroundVideoUri: StateFlow<Uri?> = _backgroundVideoUri.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe settings flow from DB
            repository.allSettings.collect { settingsList ->
                val map = settingsList.associate { it.key to it.value }
                _settings.value = map
                
                // Load background image if custom background is saved
                map["bg_image_path"]?.let { path ->
                    loadBackgroundImage(path)
                }
                // Load background video if custom video is saved
                map["bg_video_uri"]?.let { uriStr ->
                    if (uriStr.isNotEmpty()) {
                        _backgroundVideoUri.value = Uri.parse(uriStr)
                    } else {
                        _backgroundVideoUri.value = null
                    }
                }
            }
        }
        
        // Seed default plugins if empty
        viewModelScope.launch {
            repository.allPlugins.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultPlugins()
                }
            }
        }
    }

    private fun seedDefaultPlugins() {
        viewModelScope.launch {
            repository.insertPlugin(Plugin("prettier", "Prettier Formatter", "Format file JavaScript, CSS, dan HTML otomatis saat disimpan.", true, "Formatting"))
            repository.insertPlugin(Plugin("autoclose", "Auto-Close Brackets", "Menutup tanda kurung dan tanda kutip secara otomatis.", true, "Editor"))
            repository.insertPlugin(Plugin("py_linter", "Python Static Linter", "Mendeteksi kesalahan indentasi dan sintaksis Python offline.", false, "Linter"))
        }
    }

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges

    fun selectFile(file: CodeFile) {
        if (!_openFiles.value.any { it.githubPath == file.githubPath }) {
            _openFiles.value = _openFiles.value + file
        }
        _selectedFile.value = file
        _hasUnsavedChanges.value = false
        _suggestions.value = emptyList()
    }

    fun closeFile(file: CodeFile) {
        val currentOpen = _openFiles.value
        val newOpen = currentOpen.filter { it.githubPath != file.githubPath }
        _openFiles.value = newOpen
        
        if (_selectedFile.value?.githubPath == file.githubPath) {
            _selectedFile.value = newOpen.lastOrNull()
        }
    }

    fun uploadFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                val docFile = DocumentFile.fromSingleUri(context, uri)
                val fileName = docFile?.name ?: "uploaded_file"
                
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val workspacePath = _settings.value["local_workspace_path"] ?: context.filesDir.absolutePath
                    val targetFile = File(workspacePath, fileName)
                    targetFile.writeBytes(bytes)
                    
                    withContext(Dispatchers.Main) {
                        scanWorkspace(context)
                        appendTerminalLog("[system] File '$fileName' berhasil diunggah.\n")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendTerminalLog("[error] Gagal mengunggah file: ${e.message}\n")
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // Workspace Folder & File operations (Direct-to-Disk)
    fun scanWorkspace(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val uriStr = _settings.value["saf_workspace_uri"]
            if (!uriStr.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(uriStr)
                    val rootDoc = DocumentFile.fromTreeUri(context, uri)
                    if (rootDoc != null && rootDoc.exists()) {
                        _workspacePath.value = rootDoc.name ?: "Folder HP"
                        val nodes = buildSafFileTree(context, rootDoc, level = 0)
                        _workspaceFiles.value = nodes
                        return@launch
                    }
                } catch (e: Exception) {
                    appendTerminalLog("[warn] Gagal memuat folder HP: ${e.message}. Gunakan Files Explorer.\n")
                }
            }

            // Fallback to internal/external accessible app sandbox
            val root = getWorkspaceRoot(context)
            _workspacePath.value = "Files Explorer"
            val nodes = buildLocalFileTree(root, level = 0)
            _workspaceFiles.value = nodes
        }
    }

    private fun buildLocalFileTree(dir: File, level: Int): List<FileNode> {
        if (!dir.exists()) dir.mkdirs()
        val files = dir.listFiles() ?: return emptyList()
        val sorted = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        return sorted.map { file ->
            val path = file.absolutePath
            val isExpanded = _expandedPaths.contains(path)
            FileNode(
                name = file.name,
                path = path,
                isDirectory = file.isDirectory,
                isExpanded = isExpanded,
                level = level,
                size = if (file.isDirectory) 0 else file.length(),
                children = if (file.isDirectory && isExpanded) buildLocalFileTree(file, level + 1) else emptyList(),
                isSaf = false
            )
        }
    }

    private fun buildSafFileTree(context: Context, doc: DocumentFile, level: Int): List<FileNode> {
        val files = doc.listFiles()
        val sorted = files.sortedWith(compareBy({ !it.isDirectory }, { it.name?.lowercase() ?: "" }))
        return sorted.map { file ->
            val path = file.uri.toString()
            val isExpanded = _expandedPaths.contains(path)
            FileNode(
                name = file.name ?: "Tanpa Nama",
                path = path,
                isDirectory = file.isDirectory,
                isExpanded = isExpanded,
                level = level,
                size = file.length(),
                children = if (file.isDirectory && isExpanded) buildSafFileTree(context, file, level + 1) else emptyList(),
                isSaf = true
            )
        }
    }

    fun getWorkspaceRoot(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "Workspace")
        if (!dir.exists()) {
            dir.mkdirs()
            seedSampleWorkspace(dir)
        }
        return dir
    }

    private fun seedSampleWorkspace(root: File) {
        try {
            val templatesDir = File(root, "templates")
            templatesDir.mkdirs()
            
            File(root, "index.html").writeText(
                """
                <!DOCTYPE html>
                <html lang="id">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Live Web Preview</title>
                    <style>
                        body {
                            background: radial-gradient(circle, #1e1e2f 0%, #0c0c14 100%);
                            color: #ffffff;
                            font-family: system-ui, -apple-system, sans-serif;
                            display: flex;
                            flex-direction: column;
                            align-items: center;
                            justify-content: center;
                            height: 90vh;
                            margin: 0;
                            text-align: center;
                        }
                        .box {
                            background: rgba(255, 255, 255, 0.04);
                            backdrop-filter: blur(12px);
                            border: 1px solid rgba(255, 255, 255, 0.12);
                            border-radius: 20px;
                            padding: 40px;
                            box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);
                            max-width: 380px;
                            transform: scale(1);
                            transition: 0.3s ease;
                        }
                        .box:hover {
                            transform: translateY(-5px);
                            box-shadow: 0 16px 50px rgba(0, 229, 255, 0.2);
                        }
                        h1 {
                            background: linear-gradient(45deg, #00e5ff, #00ff87);
                            -webkit-background-clip: text;
                            -webkit-text-fill-color: transparent;
                            margin-bottom: 10px;
                        }
                        p {
                            color: #90a4ae;
                            font-size: 14px;
                            line-height: 1.6;
                        }
                        .btn {
                            background: linear-gradient(135deg, #00e5ff, #1a237e);
                            color: white;
                            border: none;
                            padding: 12px 24px;
                            border-radius: 10px;
                            cursor: pointer;
                            font-weight: bold;
                            box-shadow: 0 4px 15px rgba(0, 229, 255, 0.4);
                            transition: 0.2s;
                        }
                        .btn:hover {
                            transform: scale(1.05);
                            box-shadow: 0 6px 20px rgba(0, 229, 255, 0.6);
                        }
                    </style>
                </head>
                <body>
                    <div class="box">
                        <h1>Code Editor Live</h1>
                        <p>Halaman ini dirender secara real-time dari editor Anda di atas! Edit file ini untuk melihat perubahannya langsung.</p>
                        <button class="btn" onclick="celebrate()">Klik Saya</button>
                    </div>
                    <script>
                        function celebrate() {
                            alert('Halo dari JavaScript interaktif!');
                        }
                    </script>
                </body>
                </html>
                """.trimIndent()
            )
            
            File(templatesDir, "custom.css").writeText(
                """
                /* Custom Stylesheet */
                .neon-glow {
                    color: #39ff14;
                    text-shadow: 0 0 10px #39ff14;
                }
                """.trimIndent()
            )
            
            File(root, "main.py").writeText(
                """
                # Python Hello World
                def sapa_dunia(nama):
                    print(f"Halo, {nama}! Selamat belajar pemrograman.")
                    print("Terima kasih telah menggunakan Code Editor Android.")
                
                sapa_dunia("Developer")
                """.trimIndent()
            )

            File(root, "script.js").writeText(
                """
                // JavaScript test script
                const multiplier = 5;
                for (let i = 1; i <= multiplier; i++) {
                    console.log(`Hitung: ${'$'}{i} x ${'$'}{multiplier} = ${'$'}{i * multiplier}`);
                }
                """.trimIndent()
            )
        } catch (e: Exception) {
            // Ignore seed error
        }
    }

    fun toggleFolderExpansion(context: Context, path: String) {
        if (_expandedPaths.contains(path)) {
            _expandedPaths.remove(path)
        } else {
            _expandedPaths.add(path)
        }
        scanWorkspace(context)
    }

    fun selectFileNode(context: Context, node: FileNode) {
        if (node.isDirectory) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                val content = if (node.isSaf) {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(node.path))
                    context.contentResolver.openInputStream(doc!!.uri)?.use { stream ->
                        stream.bufferedReader().use { it.readText() }
                    } ?: ""
                } else {
                    File(node.path).readText()
                }
                
                val language = when {
                    node.name.endsWith(".py", ignoreCase = true) -> "Python"
                    node.name.endsWith(".js", ignoreCase = true) -> "JavaScript"
                    node.name.endsWith(".html", ignoreCase = true) || node.name.endsWith(".htm", ignoreCase = true) -> "HTML"
                    node.name.endsWith(".css", ignoreCase = true) -> "CSS"
                    node.name.endsWith(".kt", ignoreCase = true) -> "Kotlin"
                    node.name.endsWith(".java", ignoreCase = true) -> "Java"
                    else -> "Text"
                }

                // Treat physically selected file as code file with safe transient negative ID
                val codeFile = CodeFile(
                    id = if (node.isSaf) -2 else -1, 
                    name = node.name,
                    content = content,
                    language = language,
                    githubPath = node.path,
                    isSynced = false,
                    lastModified = System.currentTimeMillis()
                )
                _selectedFile.value = codeFile
                _hasUnsavedChanges.value = false
                _suggestions.value = emptyList()
                _isProcessing.value = false
            } catch (e: Exception) {
                _isProcessing.value = false
                appendTerminalLog("[error] Gagal membaca file: ${e.message}\n")
            }
        }
    }

    fun updateFileContentLocally(content: String) {
        val current = _selectedFile.value ?: return
        if (current.content == content) return // Avoid false 'unsaved changes' on load
        
        val updated = current.copy(content = content, lastModified = System.currentTimeMillis())
        _selectedFile.value = updated
        _hasUnsavedChanges.value = true
        calculateSuggestions(content)
    }

    fun saveFile(context: Context) {
        val current = _selectedFile.value ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = current.githubPath
                if (path.isNotEmpty()) {
                    if (current.id == -2) { // SAF
                        val doc = DocumentFile.fromSingleUri(context, Uri.parse(path))
                        context.contentResolver.openOutputStream(doc!!.uri, "w")?.use { stream ->
                            stream.write(current.content.toByteArray(Charsets.UTF_8))
                        }
                    } else if (current.id == -1) { // Local File
                        File(path).writeText(current.content)
                    } else {
                        repository.updateFile(current)
                    }
                }
                _hasUnsavedChanges.value = false
            } catch (e: Exception) {
                // Ignore silent background failures
            }
        }
    }

    fun createNewFileInWorkspace(context: Context, parentPath: String?, name: String, isFolder: Boolean, defaultContent: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                val uriStr = _settings.value["saf_workspace_uri"]
                
                if (!uriStr.isNullOrEmpty() && parentPath?.startsWith("content://") == true) {
                    val parentDoc = DocumentFile.fromTreeUri(context, Uri.parse(parentPath))
                    if (parentDoc != null) {
                        if (isFolder) {
                            parentDoc.createDirectory(name)
                            appendTerminalLog("[explorer] Berhasil membuat folder kustom: '$name'\n")
                        } else {
                            val newDoc = parentDoc.createFile("*/*", name)
                            newDoc?.uri?.let { uri ->
                                context.contentResolver.openOutputStream(uri)?.use { stream ->
                                    stream.write(defaultContent.toByteArray(Charsets.UTF_8))
                                }
                            }
                            appendTerminalLog("[explorer] Berhasil membuat file kustom: '$name'\n")
                        }
                    }
                } else {
                    val parentDir = if (parentPath != null) File(parentPath) else getWorkspaceRoot(context)
                    val newFile = File(parentDir, name)
                    if (isFolder) {
                        newFile.mkdirs()
                        appendTerminalLog("[explorer] Berhasil membuat folder: '$name'\n")
                    } else {
                        newFile.createNewFile()
                        newFile.writeText(defaultContent)
                        appendTerminalLog("[explorer] Berhasil membuat file: '$name'\n")
                    }
                }
                
                scanWorkspace(context)
                _isProcessing.value = false
            } catch (e: Exception) {
                _isProcessing.value = false
                appendTerminalLog("[error] Gagal membuat item: ${e.message}\n")
            }
        }
    }

    fun deleteWorkspaceItem(context: Context, node: FileNode) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                if (node.isSaf) {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(node.path))
                    doc?.delete()
                } else {
                    val file = File(node.path)
                    if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                }
                
                if (_selectedFile.value?.githubPath == node.path) {
                    _selectedFile.value = null
                }
                
                appendTerminalLog("[explorer] Berhasil menghapus '${node.name}'\n")
                scanWorkspace(context)
                _isProcessing.value = false
            } catch (e: Exception) {
                _isProcessing.value = false
                appendTerminalLog("[error] Gagal menghapus item: ${e.message}\n")
            }
        }
    }

    fun renameWorkspaceItem(context: Context, node: FileNode, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                if (node.isSaf) {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(node.path))
                    doc?.renameTo(newName)
                } else {
                    val file = File(node.path)
                    val destination = File(file.parentFile, newName)
                    file.renameTo(destination)
                }
                appendTerminalLog("[explorer] Berhasil mengubah nama '${node.name}' menjadi '$newName'\n")
                scanWorkspace(context)
                _isProcessing.value = false
            } catch (e: Exception) {
                _isProcessing.value = false
                appendTerminalLog("[error] Gagal mengubah nama item: ${e.message}\n")
            }
        }
    }

    fun duplicateFile(context: Context, node: FileNode) {
        if (node.isDirectory) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                val currentName = node.name
                val dotIndex = currentName.lastIndexOf('.')
                val copyName = if (dotIndex != -1) {
                    currentName.substring(0, dotIndex) + "_copy" + currentName.substring(dotIndex)
                } else {
                    currentName + "_copy"
                }

                if (node.isSaf) {
                    val originalDoc = DocumentFile.fromSingleUri(context, Uri.parse(node.path))
                    val parentUri = node.path.substringBeforeLast("/")
                    val parentDoc = DocumentFile.fromTreeUri(context, Uri.parse(parentUri))
                    if (originalDoc != null && parentDoc != null) {
                        val newDoc = parentDoc.createFile("*/*", copyName)
                        if (newDoc != null) {
                            context.contentResolver.openInputStream(originalDoc.uri)?.use { input ->
                                context.contentResolver.openOutputStream(newDoc.uri)?.use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                } else {
                    val originalFile = File(node.path)
                    val parentDir = originalFile.parentFile ?: getWorkspaceRoot(context)
                    val newFile = File(parentDir, copyName)
                    originalFile.copyTo(newFile, overwrite = true)
                }
                appendTerminalLog("[explorer] Berhasil menduplikasi file menjadi: '$copyName'\n")
                scanWorkspace(context)
                _isProcessing.value = false
            } catch (e: Exception) {
                _isProcessing.value = false
                appendTerminalLog("[error] Gagal menduplikasi file: ${e.message}\n")
            }
        }
    }

    fun exportWorkspaceToZip(context: Context, onComplete: (Uri) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isProcessing.value = true
                val workspaceDir = getWorkspaceRoot(context)
                val zipFile = File(context.cacheDir, "codepad_workspace.zip")
                if (zipFile.exists()) zipFile.delete()

                java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                    workspaceDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relativePath = file.relativeTo(workspaceDir).path
                            val entry = java.util.zip.ZipEntry(relativePath)
                            zos.putNextEntry(entry)
                            file.inputStream().use { input ->
                                input.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }

                val publicZip = File(context.getExternalFilesDir(null), "codepad_workspace.zip")
                zipFile.copyTo(publicZip, overwrite = true)
                val uri = Uri.fromFile(publicZip)

                appendTerminalLog("[exporter] Berhasil mengekspor semua file ke ZIP: '${publicZip.absolutePath}'\n")
                _isProcessing.value = false
                viewModelScope.launch(Dispatchers.Main) {
                    onComplete(uri)
                }
            } catch (e: Exception) {
                _isProcessing.value = false
                appendTerminalLog("[error] Gagal mengekspor ZIP: ${e.message}\n")
            }
        }
    }

    fun formatActiveFile(context: Context) {
        val file = _selectedFile.value ?: return
        val formatted = formatCode(file.content, file.language)
        updateFileContentLocally(formatted)
        appendTerminalLog("[formatter] Berhasil menata ulang sintaks file '${file.name}' (${file.language}). Belum tersimpan.\n")
    }

    private fun formatCode(code: String, language: String): String {
        val lang = language.lowercase()
        if (lang.contains("javascript") || lang.contains("js") || lang.contains("html") || lang.contains("css") || lang.contains("java") || lang.contains("kotlin")) {
            val lines = code.lines()
            val result = StringBuilder()
            var indentLevel = 0
            val indentSize = 4
            
            for (rawLine in lines) {
                val trimmed = rawLine.trim()
                if (trimmed.isEmpty()) {
                    result.append("\n")
                    continue
                }
                
                if (trimmed.startsWith("}") || trimmed.startsWith("</") || trimmed.startsWith("]")) {
                    indentLevel = maxOf(0, indentLevel - 1)
                }
                
                val indent = " ".repeat(indentLevel * indentSize)
                result.append(indent).append(trimmed).append("\n")
                
                if (trimmed.endsWith("{") || (trimmed.startsWith("<") && !trimmed.startsWith("</") && !trimmed.endsWith("/>") && trimmed.contains(">") && !trimmed.contains("</")) || trimmed.endsWith("[")) {
                    indentLevel++
                }
            }
            return result.toString().trimEnd() + "\n"
        }
        return code
    }

    // (Removed old automatic update method)

    fun deleteSelectedFile() {
        val current = _selectedFile.value ?: return
        viewModelScope.launch {
            repository.deleteFile(current)
            _selectedFile.value = null
        }
    }

    // GitHub Push Sync
    fun pushCurrentFile(pathInRepo: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val file = _selectedFile.value ?: return onError("Tidak ada file aktif untuk disinkronisasi.")
        val token = _settings.value["github_token"] ?: ""
        val owner = _settings.value["github_owner"] ?: ""
        val repo = _settings.value["github_repo"] ?: ""

        if (token.isEmpty() || owner.isEmpty() || repo.isEmpty()) {
            return onError("Harap konfigurasikan GitHub Token, Username, dan Repo di Settings.")
        }

        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.pushFileToGitHub(file, token, owner, repo, pathInRepo)
            _isProcessing.value = false
            result.onSuccess { updatedFile ->
                _selectedFile.value = updatedFile
                onSuccess("Sinkronisasi GitHub Sukses! Berhasil push ke repository $owner/$repo.")
                appendTerminalLog("\n[git] Berhasil push file '${file.name}' ke branch utama GitHub ($owner/$repo).\n")
            }.onFailure {
                onError("Gagal Push: ${it.localizedMessage ?: it.message}")
                appendTerminalLog("\n[git-error] Gagal push ke GitHub: ${it.message}\n")
            }
        }
    }

    // GitHub Pull Sync
    fun pullCurrentFile(pathInRepo: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val file = _selectedFile.value ?: return onError("Pilih atau buat file terlebih dahulu.")
        val token = _settings.value["github_token"] ?: ""
        val owner = _settings.value["github_owner"] ?: ""
        val repo = _settings.value["github_repo"] ?: ""

        if (token.isEmpty() || owner.isEmpty() || repo.isEmpty()) {
            return onError("Harap konfigurasikan GitHub Token, Username, dan Repo di Settings.")
        }

        _isProcessing.value = true
        viewModelScope.launch {
            val result = repository.pullFileFromGitHub(file, token, owner, repo, pathInRepo)
            _isProcessing.value = false
            result.onSuccess { updatedFile ->
                _selectedFile.value = updatedFile
                onSuccess("Tarik file dari GitHub Sukses!")
                appendTerminalLog("\n[git] Berhasil menarik file terbaru dari GitHub ($owner/$repo).\n")
            }.onFailure {
                onError("Gagal Pull: ${it.localizedMessage ?: it.message}")
                appendTerminalLog("\n[git-error] Gagal pull dari GitHub: ${it.message}\n")
            }
        }
    }

    // Save configuration settings
    fun saveConfigSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
        }
    }

    // Toggle plugin activation
    fun togglePlugin(plugin: Plugin) {
        viewModelScope.launch {
            repository.updatePlugin(plugin.copy(isEnabled = !plugin.isEnabled))
        }
    }

    // Terminal command runner (supports Node.js and Python)
    private var sshSession: com.jcraft.jsch.Session? = null
    private var sshChannel: com.jcraft.jsch.ChannelShell? = null
    private var sshOutputStream: java.io.OutputStream? = null

    private fun findFileNodeByName(nodes: List<FileNode>, name: String): FileNode? {
        for (node in nodes) {
            if (node.name == name) return node
            if (node.isDirectory) {
                val found = findFileNodeByName(node.children, name)
                if (found != null) return found
            }
        }
        return null
    }

    private fun getFileContent(context: Context, fileNode: FileNode): String? {
        return try {
            if (fileNode.isSaf) {
                val doc = DocumentFile.fromSingleUri(context, Uri.parse(fileNode.path))
                context.contentResolver.openInputStream(doc!!.uri)?.use { stream ->
                    stream.bufferedReader().use { it.readText() }
                } ?: ""
            } else {
                java.io.File(fileNode.path).readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun executeTerminalCommand(command: String, context: Context) {
        val trimmedCmd = command.trim()
        if (trimmedCmd.isEmpty()) return

        appendTerminalLog("> $command\n")

        if (trimmedCmd.lowercase() == "clear") {
            _terminalOutput.value = ""
            return
        }
        
        if (sshSession == null || !sshSession!!.isConnected || sshOutputStream == null) {
            if (trimmedCmd.lowercase() == "connect" || trimmedCmd.lowercase() == "ssh") {
                runSshCommand()
            } else {
                appendTerminalLog("[system] Sedang tidak terhubung ke SSH. Menghubungkan secara otomatis...\n")
                runSshCommand()
            }
            return
        }

        if (trimmedCmd.lowercase() == "exit") {
            disconnectSsh()
            return
        }
        
        val parts = trimmedCmd.split("\\s+".toRegex())
        val interceptedFiles = mutableMapOf<String, String>()
        
        if (parts.isNotEmpty()) {
            val cmd = parts[0].lowercase()
            
            // Intercept file based on command arguments (e.g. python main.py)
            if (parts.size >= 2 && (cmd == "python" || cmd == "python3" || cmd == "node" || cmd == "cat" || cmd == "bash" || cmd == "sh" || cmd == "ruby" || cmd == "php")) {
                val filename = parts[1]
                findFileNodeByName(_workspaceFiles.value, filename)?.let { node ->
                    if (!node.isDirectory) {
                        getFileContent(context, node)?.let { interceptedFiles[filename] = it }
                    }
                }
            }
            
            // Special case: npm commands usually need package.json
            if (cmd == "npm") {
                findFileNodeByName(_workspaceFiles.value, "package.json")?.let { node ->
                    getFileContent(context, node)?.let { interceptedFiles["package.json"] = it }
                }
                findFileNodeByName(_workspaceFiles.value, "package-lock.json")?.let { node ->
                    getFileContent(context, node)?.let { interceptedFiles["package-lock.json"] = it }
                }
            }
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Sync intercepted files to remote before executing command
                interceptedFiles.forEach { (name, content) ->
                    val base64Content = android.util.Base64.encodeToString(content.toByteArray(), android.util.Base64.NO_WRAP)
                    val uploadCmd = "echo '$base64Content' | base64 -d > '$name'\n"
                    sshOutputStream?.write(uploadCmd.toByteArray())
                    sshOutputStream?.flush()
                    kotlinx.coroutines.delay(300)
                    withContext(Dispatchers.Main) {
                        appendTerminalLog("[system] File lokal '$name' disinkronkan ke remote.\n")
                    }
                }

                sshOutputStream?.write((command + "\n").toByteArray())
                sshOutputStream?.flush()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendTerminalLog("[error] Failed to send command: ${e.message}\n")
                }
            }
        }
    }

    private fun runSshCommand() {
        val host = _settings.value["ssh_host"] ?: "tokaido.proxy.rlwy.net"
        val portStr = _settings.value["ssh_port"] ?: "28773"
        val port = portStr.toIntOrNull() ?: 28773
        val user = _settings.value["ssh_user"] ?: "root"
        val pass = _settings.value["ssh_pass"] ?: "railway"
        
        appendTerminalLog("[system] Menghubungkan ke $host:$port ($user)...\n")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsch = com.jcraft.jsch.JSch()
                sshSession = jsch.getSession(user, host, port)
                sshSession?.setPassword(pass)
                
                val config = java.util.Properties()
                config.put("StrictHostKeyChecking", "no")
                sshSession?.setConfig(config)
                
                sshSession?.connect(15000)
                
                sshChannel = sshSession?.openChannel("shell") as com.jcraft.jsch.ChannelShell
                sshChannel?.setPty(true)
                sshChannel?.setPtyType("xterm-color")
                
                val inputStream = sshChannel?.inputStream
                sshOutputStream = sshChannel?.outputStream
                
                sshChannel?.connect(5000)
                
                withContext(Dispatchers.Main) {
                    appendTerminalLog("[system] Berhasil terhubung! Ketik 'exit' untuk memutuskan koneksi.\n")
                }
                
                val reader = java.io.InputStreamReader(inputStream)
                val buffer = CharArray(1024)
                var read: Int
                while (sshChannel != null && !sshChannel!!.isClosed) {
                    read = reader.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val text = String(buffer, 0, read)
                        withContext(Dispatchers.Main) {
                            appendTerminalLog(text)
                        }
                    } else if (read == -1) {
                        break
                    }
                }
                
                withContext(Dispatchers.Main) {
                    appendTerminalLog("\n[system] SSH session closed.\n")
                    disconnectSsh()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendTerminalLog("[error] Koneksi SSH gagal: ${e.message}\n")
                    disconnectSsh()
                }
            }
        }
    }

    private fun disconnectSsh() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sshOutputStream?.close()
            } catch (e: Exception) {}
            try {
                sshChannel?.disconnect()
            } catch (e: Exception) {}
            try {
                sshSession?.disconnect()
            } catch (e: Exception) {}
            
            sshOutputStream = null
            sshChannel = null
            sshSession = null
            withContext(Dispatchers.Main) {
                appendTerminalLog("[system] Disconnected from SSH.\n")
            }
        }
    }

    private fun appendTerminalLog(log: String) {
        _terminalOutput.value += log
    }

    // Local Auto-complete Suggestions Logic (super fast and optimized)
    private fun calculateSuggestions(content: String) {
        val file = _selectedFile.value ?: return
        val lines = content.lines()
        if (lines.isEmpty()) return
        
        val lastLine = lines.last()
        val words = lastLine.split("[\\s\\W]".toRegex())
        val lastWord = words.lastOrNull() ?: ""

        if (lastWord.length < 2) {
            _suggestions.value = emptyList()
            return
        }

        val lang = file.language.lowercase()
        val allKeywords = when {
            lang.contains("python") -> listOf("def", "class", "import", "from", "return", "if", "elif", "else", "while", "for", "in", "print", "lambda", "try", "except")
            lang.contains("javascript") || lang.contains("js") -> listOf("const", "let", "var", "function", "return", "if", "else", "for", "while", "import", "export", "require", "console.log")
            lang.contains("kotlin") -> listOf("val", "var", "fun", "class", "interface", "object", "import", "package", "return", "if", "else", "when", "for", "while")
            lang.contains("html") -> listOf("div", "span", "class", "id", "href", "src", "style", "button", "input", "label")
            else -> listOf("if", "else", "for", "while", "return", "class", "import")
        }

        // Find words in current text that match (to suggest variables/functions in current scope!)
        val localWords = mutableSetOf<String>()
        val p = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
        val m = p.matcher(content)
        while (m.find()) {
            val word = m.group()
            if (word.length > 2 && word != lastWord) {
                localWords.add(word)
            }
        }

        val combinedSuggestions = (allKeywords + localWords)
            .filter { it.startsWith(lastWord, ignoreCase = true) }
            .take(5) // Limit to top 5 suggestions to keep screen uncluttered

        _suggestions.value = combinedSuggestions
    }

    fun applySuggestion(suggestion: String) {
        val file = _selectedFile.value ?: return
        val currentContent = file.content
        val lines = currentContent.lines()
        if (lines.isEmpty()) return

        val lastLine = lines.last()
        val words = lastLine.split("[\\s\\W]".toRegex())
        val lastWord = words.lastOrNull() ?: ""

        val updatedLastLine = if (lastWord.isNotEmpty()) {
            lastLine.substring(0, lastLine.lastIndexOf(lastWord)) + suggestion
        } else {
            lastLine + suggestion
        }

        val newContent = lines.dropLast(1).joinToString("\n") + 
                (if (lines.size > 1) "\n" else "") + updatedLastLine

        updateFileContentLocally(newContent)
        _suggestions.value = emptyList()
    }



    // Background custom photo upload processing and storage (Fail-safe copying)
    fun handleBackgroundPhotoSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val filesDir = context.filesDir
                val destinationFile = File(filesDir, "custom_bg.jpg")
                
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // Save path into DB
                val path = destinationFile.absolutePath
                saveConfigSetting("bg_image_path", path)
                loadBackgroundImage(path)
                appendTerminalLog("[editor] Berhasil memperbarui background foto kustom.\n")
            } catch (e: Exception) {
                appendTerminalLog("[error] Gagal memuat background foto: ${e.message}\n")
            }
        }
    }

    fun removeCustomBackground() {
        viewModelScope.launch {
            saveConfigSetting("bg_image_path", "")
            _backgroundImage.value = null
            appendTerminalLog("[editor] Background foto kustom dihapus.\n")
        }
    }

    fun handleBackgroundVideoSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                // Persistent permissions for SAF if needed, but here we'll just save the URI
                // Note: For long term access to external URIs, we need takePersistableUriPermission
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}

                saveConfigSetting("bg_video_uri", uri.toString())
                _backgroundVideoUri.value = uri
                
                // Remove photo if video is set
                removeCustomBackground()
                
                appendTerminalLog("[editor] Berhasil memperbarui background video kustom.\n")
            } catch (e: Exception) {
                appendTerminalLog("[error] Gagal memuat background video: ${e.message}\n")
            }
        }
    }

    fun removeCustomBackgroundVideo() {
        viewModelScope.launch {
            saveConfigSetting("bg_video_uri", "")
            _backgroundVideoUri.value = null
            appendTerminalLog("[editor] Background video kustom dihapus.\n")
        }
    }

    private fun loadBackgroundImage(path: String) {
        if (path.isEmpty()) {
            _backgroundImage.value = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (file.exists()) {
                    // Safe decoding options to prevent OutOfMemory on low-spec devices
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(path, options)
                    
                    // Target max 1080p scaled image
                    options.inSampleSize = calculateInSampleSize(options, 1080, 1920)
                    options.inJustDecodeBounds = false
                    
                    val bitmap = BitmapFactory.decodeFile(path, options)
                    _backgroundImage.value = bitmap
                } else {
                    _backgroundImage.value = null
                }
            } catch (e: Exception) {
                _backgroundImage.value = null
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
