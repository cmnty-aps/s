package com.example.ui.editor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.model.CodeFile
import com.example.data.model.Plugin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import com.example.R
import com.example.ui.components.CodeVisualTransformation
import com.example.ui.components.EditorTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: EditorViewModel) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600
    val scope = rememberCoroutineScope()

    // State bindings from ViewModel
    val workspacePath by viewModel.workspacePath.collectAsState()
    val workspaceFiles by viewModel.workspaceFiles.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val openFiles by viewModel.openFiles.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val plugins by viewModel.plugins.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val fileUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadFile(context, it) }
    }
    val suggestions by viewModel.suggestions.collectAsState()
    val backgroundImage by viewModel.backgroundImage.collectAsState()
    val backgroundVideoUri by viewModel.backgroundVideoUri.collectAsState()

    // UI Interactive States
    var showRootNewFileDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showGitHubSyncDialog by remember { mutableStateOf(false) }
    var isTerminalExpanded by remember { mutableStateOf(false) }
    var terminalInput by remember { mutableStateOf("") }
    
    // Left sidebar explorer collapse state (collapses to save mobile space)
    var isSidebarVisible by remember { mutableStateOf(true) }
    
    // Live Split HTML/CSS Webview preview state
    var isLivePreviewActive by remember { mutableStateOf(false) }

    // Search and Replace panel states
    var isSearchPanelActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    // File tree search query
    var fileSearchQuery by remember { mutableStateOf("") }

    // Editor Text Field State linked with selection
    var editorTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val sharedVerticalScrollState = rememberScrollState()

    // Undo / Redo histories
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    // Helper functions for developer keyboard
    fun insertTextAtCursor(symbol: String) {
        val currentText = editorTextFieldValue.text
        val selectionStart = editorTextFieldValue.selection.start
        val selectionEnd = editorTextFieldValue.selection.end
        val before = currentText.substring(0, selectionStart)
        val after = currentText.substring(selectionEnd)
        val newText = before + symbol + after
        val newCursorPos = selectionStart + symbol.length
        
        if (undoStack.isEmpty() || undoStack.last() != currentText) {
            if (undoStack.size > 50) undoStack.removeAt(0)
            undoStack.add(currentText)
        }
        redoStack.clear()

        editorTextFieldValue = TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursorPos)
        )
        viewModel.updateFileContentLocally(newText)
    }

    // Mobile Top Bar overflow menu
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Custom Editor State settings
    var isWordWrapEnabled by remember { mutableStateOf(false) }
    var isLineNumbersEnabled by remember { mutableStateOf(true) }
    var isMinimapEnabled by remember { mutableStateOf(false) }
    var isReadOnlyMode by remember { mutableStateOf(false) }

    var showFileStatsDialog by remember { mutableStateOf(false) }

    // Theme values
    val currentThemeName = settings["theme_name"] ?: "VS Code Dark"
    val editorTheme = EditorTheme.fromName(currentThemeName)
    val fontSize = (settings["font_size"]?.toIntOrNull() ?: 14).sp

    // Highlight.js state
    var highlightedAnnotatedString by remember { mutableStateOf<AnnotatedString?>(null) }

    val hljsWebView = remember {
        WebView(context).apply {
            this.settings.javaScriptEnabled = true
            this.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            this.settings.domStorageEnabled = false
            this.settings.databaseEnabled = false
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
                    <script>
                        function highlightCodeBase64(base64Str, language) {
                            try {
                                let code = decodeURIComponent(escape(window.atob(base64Str)));
                                let result;
                                if (language && hljs.getLanguage(language)) {
                                    result = hljs.highlight(code, { language: language });
                                } else {
                                    result = hljs.highlightAuto(code);
                                }
                                return result.value;
                            } catch (e) {
                                return "Error: " + e.message;
                            }
                        }
                    </script>
                </head>
                <body></body>
                </html>
            """.trimIndent()
            loadDataWithBaseURL("https://hljs-local", htmlContent, "text/html", "UTF-8", null)
        }
    }

    // Trigger highlight.js when text, selected file, or theme changes
    LaunchedEffect(editorTextFieldValue.text, selectedFile, currentThemeName) {
        val file = selectedFile
        if (file != null) {
            val code = editorTextFieldValue.text
            val language = file.language.lowercase()
            val base64Code = android.util.Base64.encodeToString(code.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            
            hljsWebView.evaluateJavascript("highlightCodeBase64('$base64Code', '$language')") { result ->
                val html = try {
                    if (result != null && result != "null") {
                        org.json.JSONTokener(result).nextValue() as String
                    } else {
                        ""
                    }
                } catch (e: Exception) {
                    ""
                }
                if (html.isNotEmpty() && !html.startsWith("Error")) {
                    highlightedAnnotatedString = com.example.ui.components.SyntaxHighlighter.parseHljsHtml(html, editorTheme)
                }
            }
        } else {
            highlightedAnnotatedString = null
        }
    }

    // Load workspace files on launch
    LaunchedEffect(Unit) {
        viewModel.scanWorkspace(context)
    }

    // Synchronize TextFieldValue when selected file changes
    LaunchedEffect(selectedFile) {
        selectedFile?.let {
            if (it.content != editorTextFieldValue.text) {
                editorTextFieldValue = TextFieldValue(it.content)
            }
            undoStack.clear()
            redoStack.clear()
        } ?: run {
            editorTextFieldValue = TextFieldValue("")
            undoStack.clear()
            redoStack.clear()
        }
    }

    // Auto-scroll when cursor position changes
    LaunchedEffect(editorTextFieldValue.selection) {
        if (editorTextFieldValue.selection.collapsed) {
            val cursorIndex = editorTextFieldValue.selection.start
            val textBeforeCursor = editorTextFieldValue.text.substring(0, cursorIndex)
            val lineIndex = textBeforeCursor.count { it == '\n' }
            
            // Approximate line height based on font size
            val density = context.resources.displayMetrics.density
            val lineHeight = (fontSize.value * density * 1.4f).toInt()
            val targetScroll = lineIndex * lineHeight
            
            // Check if target is out of view
            val currentScroll = sharedVerticalScrollState.value
            val viewportHeight = (configuration.screenHeightDp * density * 0.4f).toInt() // Roughly 40% of screen height
            
            if (targetScroll < currentScroll || targetScroll > (currentScroll + viewportHeight)) {
                sharedVerticalScrollState.animateScrollTo((targetScroll - (viewportHeight / 2)).coerceAtLeast(0))
            }
        }
    }

    // Photo picker launcher for custom backgrounds
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.handleBackgroundPhotoSelected(context, it)
        }
    }

    // Video picker launcher for custom backgrounds
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            viewModel.handleBackgroundVideoSelected(context, it)
        }
    }

    // Custom Workspace tree selection launcher
    val workspaceDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore permission persist errors
            }
            viewModel.saveConfigSetting("saf_workspace_uri", it.toString())
            viewModel.scanWorkspace(context)
            Toast.makeText(context, "Berhasil menghubungkan folder kustom HP!", Toast.LENGTH_SHORT).show()
        }
    }

    // Main Scaffold Layout
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        fun moveCursor(offset: Int) {
            val currentText = editorTextFieldValue.text
            val newSelectionStart = (editorTextFieldValue.selection.start + offset).coerceIn(0, currentText.length)
            editorTextFieldValue = editorTextFieldValue.copy(
                selection = androidx.compose.ui.text.TextRange(newSelectionStart)
            )
        }

        fun handleUndo() {
            if (undoStack.isNotEmpty()) {
                val currentText = editorTextFieldValue.text
                val previousText = undoStack.removeAt(undoStack.lastIndex)
                redoStack.add(currentText)
                editorTextFieldValue = TextFieldValue(
                    text = previousText,
                    selection = androidx.compose.ui.text.TextRange(previousText.length)
                )
                viewModel.updateFileContentLocally(previousText)
            }
        }

        fun handleRedo() {
            if (redoStack.isNotEmpty()) {
                val currentText = editorTextFieldValue.text
                val nextText = redoStack.removeAt(redoStack.lastIndex)
                undoStack.add(currentText)
                editorTextFieldValue = TextFieldValue(
                    text = nextText,
                    selection = androidx.compose.ui.text.TextRange(nextText.length)
                )
                viewModel.updateFileContentLocally(nextText)
            }
        }

        Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigation Icon
                    IconButton(
                        onClick = { isSidebarVisible = !isSidebarVisible },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isSidebarVisible) Icons.Default.MenuOpen else Icons.Default.Menu,
                            contentDescription = "Toggle Sidebar",
                            tint = if (editorTheme.isDark) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Title
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val logoResId = when {
                            selectedFile == null -> R.drawable.ic_cmnty_logo
                            selectedFile!!.name.endsWith(".html", ignoreCase = true) || selectedFile!!.name.endsWith(".htm", ignoreCase = true) -> R.drawable.ic_html_logo
                            selectedFile!!.name.endsWith(".css", ignoreCase = true) -> R.drawable.ic_css_logo
                            selectedFile!!.name.endsWith(".js", ignoreCase = true) -> R.drawable.ic_js_logo
                            selectedFile!!.name.endsWith(".py", ignoreCase = true) -> R.drawable.ic_python_logo
                            selectedFile!!.name.endsWith(".php", ignoreCase = true) -> R.drawable.ic_php_logo
                            selectedFile!!.name.endsWith(".json", ignoreCase = true) -> R.drawable.ic_json_logo
                            selectedFile!!.name.endsWith(".java", ignoreCase = true) -> R.drawable.ic_java_logo
                            selectedFile!!.name.endsWith(".kt", ignoreCase = true) -> R.drawable.ic_kotlin_logo
                            selectedFile!!.name.endsWith(".md", ignoreCase = true) -> R.drawable.ic_markdown_logo
                            else -> R.drawable.ic_generic_file_logo
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color.LightGray.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = logoResId),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        val appTitle = if (selectedFile != null) {
                            selectedFile!!.name + (if (hasUnsavedChanges) " *" else "")
                        } else {
                            "Cmnty Studio"
                        }
                        Text(
                            text = appTitle,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            color = if (hasUnsavedChanges) Color(0xFFFFC107) else (if (editorTheme.isDark) Color.White else Color.Black)
                        )
                    }

                    // Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTablet) {
                            if (selectedFile != null) {
                                IconButton(onClick = { 
                                    viewModel.saveFile(context)
                                    Toast.makeText(context, "File berhasil disimpan", Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Simpan File",
                                        tint = if (hasUnsavedChanges) Color(0xFFFFC107) else (if (editorTheme.isDark) Color.White else Color.Black),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (selectedFile != null && (selectedFile!!.language.lowercase() == "html" || selectedFile!!.language.lowercase() == "css")) {
                                IconButton(onClick = { isLivePreviewActive = !isLivePreviewActive }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = if (isLivePreviewActive) Icons.Default.Phonelink else Icons.Default.Language,
                                        contentDescription = "Toggle Web Preview",
                                        tint = if (isLivePreviewActive) Color(0xFF00FFCC) else (if (editorTheme.isDark) Color.White else Color.Black),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (selectedFile != null) {
                                IconButton(onClick = { isSearchPanelActive = !isSearchPanelActive }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Search, 
                                        contentDescription = "Cari & Ganti",
                                        tint = if (isSearchPanelActive) Color(0xFF00FFCC) else (if (editorTheme.isDark) Color.White else Color.Black),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { viewModel.formatActiveFile(context) }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.FormatAlignLeft, 
                                        contentDescription = "Format Code", 
                                        tint = if (editorTheme.isDark) Color.White else Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { showRootNewFileDialog = true }, modifier = Modifier.testTag("new_file_button").size(36.dp)) {
                                Icon(Icons.Default.AddBox, contentDescription = "Buat File Baru", tint = if (editorTheme.isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                            }
                            var showExpandedOverflowMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showExpandedOverflowMenu = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu Lainnya", tint = if (editorTheme.isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(
                                    expanded = showExpandedOverflowMenu,
                                    onDismissRequest = { showExpandedOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Unggah File") },
                                        onClick = {
                                            showExpandedOverflowMenu = false
                                            fileUploadLauncher.launch("*/*")
                                        },
                                        leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("GitHub Sinkronisasi") },
                                        onClick = {
                                            showExpandedOverflowMenu = false
                                            showGitHubSyncDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null) }
                                    )
                                }
                            }
                            IconButton(onClick = { showGitHubSyncDialog = true }, modifier = Modifier.testTag("github_button").size(36.dp)) {
                                Icon(Icons.Default.CloudSync, contentDescription = "GitHub Sinkronisasi", tint = if (editorTheme.isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { showSettingsDialog = true }, modifier = Modifier.testTag("settings_button").size(36.dp)) {
                                Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = if (editorTheme.isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            if (selectedFile != null) {
                                IconButton(onClick = { 
                                    viewModel.saveFile(context)
                                    Toast.makeText(context, "File berhasil disimpan", Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Simpan File",
                                        tint = if (hasUnsavedChanges) Color(0xFFFFC107) else (if (editorTheme.isDark) Color.White else Color.Black),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (selectedFile != null && (selectedFile!!.language.lowercase() == "html" || selectedFile!!.language.lowercase() == "css")) {
                                IconButton(onClick = { isLivePreviewActive = !isLivePreviewActive }, modifier = Modifier.size(36.dp)) {
                                    Icon(
                                        imageVector = if (isLivePreviewActive) Icons.Default.Phonelink else Icons.Default.Language,
                                        contentDescription = "Toggle Web Preview",
                                        tint = if (isLivePreviewActive) Color(0xFF00FFCC) else (if (editorTheme.isDark) Color.White else Color.Black),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Box {
                                IconButton(onClick = { showOverflowMenu = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu Lainnya", tint = if (editorTheme.isDark) Color.White else Color.Black, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false }
                                ) {
                                    if (selectedFile != null) {
                                        DropdownMenuItem(
                                            text = { Text("Cari & Ganti") },
                                            onClick = {
                                                showOverflowMenu = false
                                                isSearchPanelActive = !isSearchPanelActive
                                            },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Format Code") },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.formatActiveFile(context)
                                        },
                                        leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null) }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("Statistik Detail File") },
                                        onClick = {
                                            showOverflowMenu = false
                                            showFileStatsDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Unggah File") },
                                onClick = {
                                    showOverflowMenu = false
                                    fileUploadLauncher.launch("*/*")
                                },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Buat File Baru") },
                                onClick = {
                                    showOverflowMenu = false
                                    showRootNewFileDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("GitHub Sinkronisasi") },
                                onClick = {
                                    showOverflowMenu = false
                                    showGitHubSyncDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Ekspor ke ZIP") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.exportWorkspaceToZip(context) { uri ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Bagikan Workspace ZIP"))
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Pengaturan") },
                                onClick = {
                                    showOverflowMenu = false
                                    showSettingsDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
    }
},
    bottomBar = {
            Column(modifier = Modifier.imePadding()) {
                // Autocomplete suggestion bar
                AnimatedVisibility(
                    visible = suggestions.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(editorTheme.background.copy(alpha = 0.7f))
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f))
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saran:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        suggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(editorTheme.keywordColor.copy(alpha = 0.15f))
                                    .border(1.dp, editorTheme.keywordColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .clickable {
                                        viewModel.applySuggestion(suggestion)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = editorTheme.foreground
                                )
                            }
                        }
                    }
                }



                // Polished Terminal trigger
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 0.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { isTerminalExpanded = !isTerminalExpanded }
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(
                            if (isTerminalExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.Terminal,
                            contentDescription = "Toggle Terminal",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Terminal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { isTerminalExpanded = !isTerminalExpanded },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isTerminalExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Expand",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background image layer
            if (backgroundImage != null) {
                Image(
                    bitmap = backgroundImage!!.asImageBitmap(),
                    contentDescription = "Custom Background Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (backgroundVideoUri != null) {
                AndroidView(
                    factory = { ctx ->
                        android.widget.VideoView(ctx).apply {
                            setVideoURI(backgroundVideoUri)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                mp.setVolume(0f, 0f)
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        if (view.tag != backgroundVideoUri) {
                            view.setVideoURI(backgroundVideoUri)
                            view.tag = backgroundVideoUri
                        }
                    }
                )
            }

            // Dark tint for background readability
            if (backgroundImage != null || backgroundVideoUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            }

            // Central Workspace Layout with Sidebar & Editor Panel
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Sidebar (Only in side-by-side mode on tablet)
                    if (isTablet) {
                        AnimatedVisibility(
                            visible = isSidebarVisible,
                            enter = slideInHorizontally { -it } + fadeIn(),
                            exit = slideOutHorizontally { -it } + fadeOut()
                        ) {
                            Box(modifier = Modifier.width(280.dp)) {
                                SidebarCard(
                                    editorTheme = editorTheme,
                                    workspacePath = workspacePath,
                                    onAttachFolder = { workspaceDirectoryLauncher.launch(null) },
                                    showRootNewFileDialog = { showRootNewFileDialog = true },
                                    onRefresh = { viewModel.scanWorkspace(context) },
                                    fileSearchQuery = fileSearchQuery,
                                    onFileSearchQueryChange = { query -> fileSearchQuery = query },
                                    workspaceFiles = workspaceFiles,
                                    selectedFile = selectedFile,
                                    onSelectNode = { node ->
                                        viewModel.selectFileNode(context, node)
                                    },
                                    onToggleExpand = { node ->
                                        viewModel.toggleFolderExpansion(context, node.path)
                                    },
                                    onCreateItem = { parent, name, isFolder, content ->
                                        viewModel.createNewFileInWorkspace(context, parent, name, isFolder, content)
                                    },
                                    onDeleteItem = { node ->
                                        viewModel.deleteWorkspaceItem(context, node)
                                    },
                                    onRenameItem = { node, newName ->
                                        viewModel.renameWorkspaceItem(context, node, newName)
                                    },
                                    onDuplicateItem = { node ->
                                        viewModel.duplicateFile(context, node)
                                    },
                                    onUploadFile = { fileUploadLauncher.launch("*/*") }
                                )
                            }
                        }
                        
                        if (isSidebarVisible) {
                            // No spacer for flush look
                        }
                    }

                // Right Panel: Editor Area + Optional Live Split Preview
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Editor Panel Container
                    if (!(isCompact && isLivePreviewActive)) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = editorTheme.background.copy(alpha = 0.6f)),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Editor Tab Bar
                            if (openFiles.isNotEmpty()) {
                                ScrollableTabRow(
                                    selectedTabIndex = openFiles.indexOfFirst { it.githubPath == selectedFile?.githubPath }.coerceAtLeast(0),
                                    containerColor = Color.Black.copy(alpha = 0.3f),
                                    contentColor = Color.White,
                                    edgePadding = 0.dp,
                                    divider = {},
                                    indicator = { tabPositions ->
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[openFiles.indexOfFirst { it.githubPath == selectedFile?.githubPath }.coerceAtLeast(0)]),
                                            color = Color(0xFF38bdf8)
                                        )
                                    }
                                ) {
                                    openFiles.forEach { file ->
                                        Tab(
                                            selected = selectedFile?.githubPath == file.githubPath,
                                            onClick = { viewModel.selectFile(file) },
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            ) {
                                                Text(
                                                    text = file.name,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    color = if (selectedFile?.githubPath == file.githubPath) Color.White else Color.Gray
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = { viewModel.closeFile(file) },
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Close",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }


                            // Search and Replace Panel (Sliding overlay)
                            AnimatedVisibility(
                                visible = isSearchPanelActive,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                label = { Text("Cari Kata", fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                textStyle = TextStyle(fontSize = 12.sp)
                                            )
                                            OutlinedTextField(
                                                value = replaceQuery,
                                                onValueChange = { replaceQuery = it },
                                                label = { Text("Ganti dengan", fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                textStyle = TextStyle(fontSize = 12.sp)
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        if (searchQuery.isNotEmpty()) {
                                                            val currentText = editorTextFieldValue.text
                                                            val index = currentText.indexOf(searchQuery, ignoreCase = true)
                                                            if (index != -1) {
                                                                // Calculate line index for scrolling
                                                                val lineIndex = currentText.substring(0, index).count { it == '\n' }
                                                                scope.launch {
                                                                    // Estimate scroll position (approx 24dp per line for 13sp font + padding)
                                                                    val scrollPos = (lineIndex * 24 * context.resources.displayMetrics.density).toInt()
                                                                    sharedVerticalScrollState.animateScrollTo(scrollPos)
                                                                }
                                                                // Selection highlight
                                                                editorTextFieldValue = editorTextFieldValue.copy(
                                                                    selection = androidx.compose.ui.text.TextRange(index, index + searchQuery.length)
                                                                )
                                                            } else {
                                                                Toast.makeText(context, "Kata tidak ditemukan", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                                ) {
                                                    Text("Cari", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = {
                                                        // Replace first
                                                        if (searchQuery.isNotEmpty()) {
                                                            val currentText = editorTextFieldValue.text
                                                            val updatedText = currentText.replaceFirst(searchQuery, replaceQuery, ignoreCase = true)
                                                            editorTextFieldValue = TextFieldValue(updatedText)
                                                            viewModel.updateFileContentLocally(updatedText)
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Ganti", fontSize = 11.sp)
                                                }
                                                Button(
                                                    onClick = {
                                                        // Replace all
                                                        if (searchQuery.isNotEmpty()) {
                                                            val currentText = editorTextFieldValue.text
                                                            val updatedText = currentText.replace(searchQuery, replaceQuery, ignoreCase = true)
                                                            editorTextFieldValue = TextFieldValue(updatedText)
                                                            viewModel.updateFileContentLocally(updatedText)
                                                            Toast.makeText(context, "Semua kata berhasil diganti!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Ganti Semua", fontSize = 11.sp)
                                                }
                                            }
                                            IconButton(
                                                onClick = { isSearchPanelActive = false },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedFile == null) {
                                // Empty State Workspace Welcome Card
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Terminal, 
                                            contentDescription = "Workspace Active", 
                                            modifier = Modifier.size(64.dp), 
                                            tint = editorTheme.keywordColor
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            "Selamat Datang di Cmnty Studio", 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 16.sp,
                                            color = editorTheme.foreground
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Silakan buka file dari penjelajah di panel sebelah kiri atau klik 'Import Folder' untuk coding langsung dari folder lokal Anda.",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 16.dp),
                                            lineHeight = 18.sp
                                        )
                                        Spacer(Modifier.height(24.dp))
                                        Button(
                                            onClick = { workspaceDirectoryLauncher.launch(null) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Import Folder", color = Color.Black)
                                        }
                                    }
                                }
                            } else {
                                // Active text editor block
                                val horizontalScrollState = rememberScrollState()

                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    // Line Numbers
                                    if (isLineNumbersEnabled) {
                                        val lineCount = editorTextFieldValue.text.lines().size
                                        Column(
                                            modifier = Modifier
                                                .width(42.dp)
                                                .fillMaxHeight()
                                                .background(Color.Black.copy(alpha = 0.15f))
                                                .verticalScroll(sharedVerticalScrollState)
                                                .padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            for (i in 1..lineCount) {
                                                Text(
                                                    text = i.toString(),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = fontSize,
                                                    color = Color.Gray.copy(alpha = 0.5f),
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Real Editable code viewport
                                    Box(
                                        modifier = if (isWordWrapEnabled) {
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .padding(12.dp)
                                                .verticalScroll(sharedVerticalScrollState)
                                        } else {
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .padding(12.dp)
                                                .verticalScroll(sharedVerticalScrollState)
                                                .horizontalScroll(horizontalScrollState)
                                        }
                                    ) {
                                        BasicTextField(
                                            value = editorTextFieldValue,
                                            onValueChange = {
                                                if (!isReadOnlyMode) {
                                                    editorTextFieldValue = it
                                                    viewModel.updateFileContentLocally(it.text)
                                                }
                                            },
                                            readOnly = isReadOnlyMode,
                                            textStyle = TextStyle(
                                                color = editorTheme.foreground,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = fontSize
                                            ),
                                            visualTransformation = CodeVisualTransformation(
                                                selectedFile!!.language,
                                                currentThemeName,
                                                highlightedAnnotatedString
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("code_editor_field"),
                                            cursorBrush = Brush.verticalGradient(listOf(editorTheme.keywordColor, editorTheme.keywordColor))
                                        )
                                    }

                                    // (Minimap removed)
                                }

                                // Bottom Status Info Bar (Stats removed as requested)
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }

                // Optional Live Split HTML preview
                if (selectedFile != null && isLivePreviewActive && (selectedFile!!.language.lowercase() == "html" || selectedFile!!.language.lowercase() == "css")) {
                    Spacer(Modifier.width(0.dp))
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        color = Color.White
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEFEFEF))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("BROWSER PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                }
                                IconButton(
                                    onClick = { isLivePreviewActive = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                                }
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                AndroidView(
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            getSettings().javaScriptEnabled = true
                                            getSettings().cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                                            getSettings().databaseEnabled = false
                                            getSettings().domStorageEnabled = true
                                            webViewClient = WebViewClient()
                                            // Transparent background
                                            setBackgroundColor(0)
                                        }
                                    },
                                     update = { webView ->
                                         val rawHtml = if (selectedFile != null && selectedFile!!.language.lowercase() == "html") {
                                             viewModel.getBundledHtml(context, selectedFile!!)
                                         } else {
                                             selectedFile?.content ?: ""
                                         }
                                         
                                         // Inject a simple CSS reset to avoid gaps
                                         val bundledHtml = if (rawHtml.contains("<head>", ignoreCase = true)) {
                                             rawHtml.replace("<head>", "<head><style>body { margin: 0; padding: 0; }</style>", ignoreCase = true)
                                         } else {
                                             "<style>body { margin: 0; padding: 0; }</style>$rawHtml"
                                         }

                                         webView.loadDataWithBaseURL(
                                             "file:///",
                                             bundledHtml,
                                             "text/html",
                                             "UTF-8",
                                             null
                                         )
                                     },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }

            // Expanding integrated virtual terminal console
            AnimatedVisibility(
                visible = isTerminalExpanded,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0F)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                .background(Color.Black)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, contentDescription = "Terminal Icon", tint = Color(0xFF81D4FA), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("TERMINAL CONSOLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row {
                                IconButton(onClick = { viewModel.executeTerminalCommand("clear", context) }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { isTerminalExpanded = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFF0F0F12))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = com.example.ui.components.TerminalHighlighter.parseAnsiToAnnotatedString(terminalOutput),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFFECEFF1)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$ ", color = Color(0xFF81D4FA), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            BasicTextField(
                                value = terminalInput,
                                onValueChange = { terminalInput = it },
                                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("terminal_input_field"),
                                cursorBrush = Brush.verticalGradient(listOf(Color.White, Color.White))
                            )
                            IconButton(
                                onClick = {
                                    viewModel.executeTerminalCommand(terminalInput, context)
                                    terminalInput = ""
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Kirim", tint = Color(0xFF81D4FA), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Spinning loader for processes
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = editorTheme.keywordColor)
                }
            }
        }
    }

    // On mobile (compact width), if sidebar is visible, render it as a sliding drawer-like overlay!
    if (!isTablet) {
        AnimatedVisibility(
            visible = isSidebarVisible,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
            modifier = Modifier.zIndex(10f)
        ) {
            // Dimmed background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isSidebarVisible = false }
            ) {
                // Sidebar content sliding in from the left
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .clickable(enabled = false) {} // Prevent clicking through the card
                ) {
                    SidebarCard(
                        editorTheme = editorTheme,
                        workspacePath = workspacePath,
                        onAttachFolder = { workspaceDirectoryLauncher.launch(null) },
                        showRootNewFileDialog = { showRootNewFileDialog = true },
                        onRefresh = { viewModel.scanWorkspace(context) },
                        fileSearchQuery = fileSearchQuery,
                        onFileSearchQueryChange = { query -> fileSearchQuery = query },
                        workspaceFiles = workspaceFiles,
                        selectedFile = selectedFile,
                        onSelectNode = { node ->
                            viewModel.selectFileNode(context, node)
                            isSidebarVisible = false // Automatically collapse on selection!
                        },
                        onToggleExpand = { node ->
                            viewModel.toggleFolderExpansion(context, node.path)
                        },
                        onCreateItem = { parent, name, isFolder, content ->
                            viewModel.createNewFileInWorkspace(context, parent, name, isFolder, content)
                        },
                        onDeleteItem = { node ->
                            viewModel.deleteWorkspaceItem(context, node)
                        },
                        onRenameItem = { node, newName ->
                            viewModel.renameWorkspaceItem(context, node, newName)
                        },
                        onDuplicateItem = { node ->
                            viewModel.duplicateFile(context, node)
                        },
                        onUploadFile = { fileUploadLauncher.launch("*/*") }
                    )
                }
            }
        }
    }

    // --- POPUP DIALOGS ---

    // 1. Root New File Dialog
    if (showRootNewFileDialog) {
        var fileName by remember { mutableStateOf("") }
        var isFolder by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showRootNewFileDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text("Buat Item Baru di Workspace Root", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { isFolder = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isFolder) Color.White else Color.Black,
                                contentColor = if (!isFolder) Color.Black else Color.White
                            ),
                            border = if (isFolder) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("File Baru", color = if (!isFolder) Color.Black else Color.White)
                        }
                        Button(
                            onClick = { isFolder = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFolder) Color.White else Color.Black,
                                contentColor = if (isFolder) Color.Black else Color.White
                            ),
                            border = if (!isFolder) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Folder Baru", color = if (isFolder) Color.Black else Color.White)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text(if (isFolder) "Nama Folder (e.g. src)" else "Nama File (e.g. main.py)") },
                        modifier = Modifier.fillMaxWidth().testTag("new_file_name_input")
                    )

                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showRootNewFileDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Batal")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (fileName.trim().isNotEmpty()) {
                                    viewModel.createNewFileInWorkspace(context, null, fileName.trim(), isFolder, "")
                                    showRootNewFileDialog = false
                                } else {
                                    Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text("Buat")
                        }
                    }
                }
            }
        }
    }


    // 1c. File Statistics Detail Dialog
    if (showFileStatsDialog) {
        val fileText = editorTextFieldValue.text
        val charCount = fileText.length
        val lineCount = fileText.lines().size
        val wordCount = fileText.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        val sizeInBytes = fileText.toByteArray(Charsets.UTF_8).size
        val formattedSize = if (sizeInBytes >= 1024) {
            String.format("%.2f KB", sizeInBytes / 1024.0)
        } else {
            "$sizeInBytes Bytes"
        }
        val fileName = selectedFile?.name ?: "N/A"
        val filePath = selectedFile?.githubPath ?: "N/A"
        val fileLang = selectedFile?.language ?: "N/A"

        Dialog(onDismissRequest = { showFileStatsDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Statistik Detail File",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "Nama File" to fileName,
                            "Bahasa/Ekstensi" to fileLang,
                            "Jalur Berkas" to filePath,
                            "Ukuran Berkas" to formattedSize,
                            "Jumlah Baris" to "$lineCount Baris",
                            "Jumlah Kata" to "$wordCount Kata",
                            "Jumlah Karakter" to "$charCount Karakter"
                        ).forEach { (label, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                Text(
                                    value, 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showFileStatsDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }

    // 2. Settings Dialog
    if (showSettingsDialog) {
        val themes = listOf("VS Code Dark", "Nord", "Dracula", "One Dark", "Solarized Dark")
        val githubToken = settings["github_token"] ?: ""
        val githubOwner = settings["github_owner"] ?: ""
        val githubRepo = settings["github_repo"] ?: ""
        val sshHost = settings["ssh_host"] ?: "tokaido.proxy.rlwy.net"
        val sshPort = settings["ssh_port"] ?: "28773"
        val sshUser = settings["ssh_user"] ?: "root"
        val sshPass = settings["ssh_pass"] ?: "railway"
        
        var tokenInput by remember { mutableStateOf(githubToken) }
        var ownerInput by remember { mutableStateOf(githubOwner) }
        var repoInput by remember { mutableStateOf(githubRepo) }
        var sshHostInput by remember { mutableStateOf(sshHost) }
        var sshPortInput by remember { mutableStateOf(sshPort) }
        var sshUserInput by remember { mutableStateOf(sshUser) }
        var sshPassInput by remember { mutableStateOf(sshPass) }
        
        var fontSizeInput by remember { mutableStateOf(settings["font_size"] ?: "14") }

        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Pengaturan Editor", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    Text("Pilih Tema Editor:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(themes) { theme ->
                            val isSelected = currentThemeName == theme
                            val themeIcon = when (theme) {
                                "VS Code Dark" -> Icons.Default.Code
                                "Nord" -> Icons.Default.AcUnit
                                "Dracula" -> Icons.Default.AutoAwesome
                                "One Dark" -> Icons.Default.Architecture
                                "Solarized Dark" -> Icons.Default.WbSunny
                                else -> Icons.Default.Style
                            }
                            Button(
                                onClick = { viewModel.saveConfigSetting("theme_name", theme) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color.White else Color.Black,
                                    contentColor = if (isSelected) Color.Black else Color.White
                                ),
                                border = if (!isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)) else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(themeIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(theme, fontSize = 12.sp, color = if (isSelected) Color.Black else Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Background Media (Foto/Video):", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Foto", fontSize = 12.sp, color = Color.Black)
                        }
                        Button(
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Video", fontSize = 12.sp, color = Color.White)
                        }
                        if (backgroundImage != null || backgroundVideoUri != null) {
                            Button(
                                onClick = { 
                                    viewModel.removeCustomBackground()
                                    viewModel.removeCustomBackgroundVideo()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.5f).height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = fontSizeInput,
                        onValueChange = { fontSizeInput = it },
                        label = { Text("Ukuran Font Editor (dp)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Integrasi GitHub (Memerlukan Internet):", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("GitHub Personal Access Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ownerInput,
                        onValueChange = { ownerInput = it },
                        label = { Text("Username GitHub (Owner)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = { Text("Nama Repository") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("Konfigurasi Tampilan Editor:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Word Wrap (Bungkus Baris)", fontSize = 13.sp)
                        Switch(
                            checked = isWordWrapEnabled,
                            onCheckedChange = { isWordWrapEnabled = it }
                        )
                    }
                    
                    // (Minimap toggle removed)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tampilkan Nomor Baris", fontSize = 13.sp)
                        Switch(
                            checked = isLineNumbersEnabled,
                            onCheckedChange = { isLineNumbersEnabled = it }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mode Baca-Saja (Read-Only)", fontSize = 13.sp)
                        Switch(
                            checked = isReadOnlyMode,
                            onCheckedChange = { isReadOnlyMode = it }
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Konfigurasi SSH Terminal (Memerlukan Internet):", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sshHostInput,
                        onValueChange = { sshHostInput = it },
                        label = { Text("SSH Host") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sshPortInput,
                        onValueChange = { sshPortInput = it },
                        label = { Text("SSH Port") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sshUserInput,
                        onValueChange = { sshUserInput = it },
                        label = { Text("SSH Username") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sshPassInput,
                        onValueChange = { sshPassInput = it },
                        label = { Text("SSH Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                    )

                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Tutup")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                viewModel.saveConfigSetting("github_token", tokenInput)
                                viewModel.saveConfigSetting("github_owner", ownerInput)
                                viewModel.saveConfigSetting("github_repo", repoInput)
                                viewModel.saveConfigSetting("font_size", fontSizeInput)
                                viewModel.saveConfigSetting("ssh_host", sshHostInput)
                                viewModel.saveConfigSetting("ssh_port", sshPortInput)
                                viewModel.saveConfigSetting("ssh_user", sshUserInput)
                                viewModel.saveConfigSetting("ssh_pass", sshPassInput)
                                showSettingsDialog = false
                                Toast.makeText(context, "Pengaturan disimpan!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simpan", color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    // 3. GitHub Sync Dialog
    if (showGitHubSyncDialog) {
        var pathInRepo by remember { mutableStateOf(selectedFile?.githubPath?.substringAfterLast("/") ?: selectedFile?.name ?: "main.py") }
        Dialog(onDismissRequest = { showGitHubSyncDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text("GitHub Sync Manager", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Repository: ${settings["github_owner"] ?: "(belum diisi)"}/${settings["github_repo"] ?: "(belum diisi)"}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pathInRepo,
                        onValueChange = { pathInRepo = it },
                        label = { Text("Path File di Repo (e.g. src/main.py)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.pullCurrentFile(
                                    pathInRepo = pathInRepo,
                                    onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                )
                                showGitHubSyncDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Pull (Unduh)", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                viewModel.pushCurrentFile(
                                    pathInRepo = pathInRepo,
                                    onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                )
                                showGitHubSyncDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("Push (Kirim)", fontSize = 12.sp, color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    } // End of BoxWithConstraints
} // End of EditorScreen
}
