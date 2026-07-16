package com.example.ui.editor

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CodeFile
import com.example.ui.components.EditorTheme

@Composable
fun FileExplorerItem(
    node: FileNode,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit,
    onCreateItem: (parentPath: String?, name: String, isFolder: Boolean, defaultContent: String) -> Unit,
    onDeleteItem: () -> Unit,
    onRenameItem: (newName: String) -> Unit,
    onDuplicateItem: () -> Unit,
    editorTheme: EditorTheme
) {
    var showMenu by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createIsFolder by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameNewName by remember { mutableStateOf(node.name) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) editorTheme.keywordColor.copy(alpha = 0.15f) else Color.Transparent)
            .clickable {
                if (node.isDirectory) {
                    onToggleExpand()
                } else {
                    onSelect()
                }
            }
            .padding(start = (12 * node.level).dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.isDirectory) {
            Icon(
                imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = editorTheme.foreground.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Spacer(modifier = Modifier.width(4.dp))

        if (node.isDirectory) {
            Icon(
                imageVector = if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = Color(0xFFFFCA28),
                modifier = Modifier.size(20.dp)
            )
        } else {
            val logoResId = when {
                node.name.endsWith(".html", ignoreCase = true) || node.name.endsWith(".htm", ignoreCase = true) -> R.drawable.ic_html_logo
                node.name.endsWith(".css", ignoreCase = true) -> R.drawable.ic_css_logo
                node.name.endsWith(".js", ignoreCase = true) -> R.drawable.ic_js_logo
                node.name.endsWith(".py", ignoreCase = true) -> R.drawable.ic_python_logo
                node.name.endsWith(".php", ignoreCase = true) -> R.drawable.ic_php_logo
                node.name.endsWith(".json", ignoreCase = true) -> R.drawable.ic_json_logo
                node.name.endsWith(".java", ignoreCase = true) -> R.drawable.ic_java_logo
                node.name.endsWith(".kt", ignoreCase = true) -> R.drawable.ic_kotlin_logo
                node.name.endsWith(".md", ignoreCase = true) -> R.drawable.ic_markdown_logo
                else -> R.drawable.ic_generic_file_logo
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = node.name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) editorTheme.keywordColor else editorTheme.foreground,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Actions",
                    tint = editorTheme.foreground.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (node.isDirectory) {
                    DropdownMenuItem(
                        text = { Text("Buat File Baru") },
                        onClick = {
                            showMenu = false
                            createIsFolder = false
                            newItemName = ""
                            showCreateDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Buat Folder Baru") },
                        onClick = {
                            showMenu = false
                            createIsFolder = true
                            newItemName = ""
                            showCreateDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Duplikat File") },
                        onClick = {
                            showMenu = false
                            onDuplicateItem()
                        },
                        leadingIcon = { Icon(Icons.Default.FileCopy, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Ubah Nama") },
                    onClick = {
                        showMenu = false
                        renameNewName = node.name
                        showRenameDialog = true
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                DropdownMenuItem(
                    text = { Text("Hapus") },
                    onClick = {
                        showMenu = false
                        onDeleteItem()
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Red) }
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (createIsFolder) "Buat Folder Baru" else "Buat File Baru") },
            text = {
                Column {
                    Text("Buat di: ${node.name}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Nama") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.trim().isNotEmpty()) {
                            val defaultCode = when (newItemName.substringAfterLast(".", "").lowercase()) {
                                "py" -> "print(\"Halo Dunia dari Python!\")\n"
                                "js" -> "console.log(\"Halo Dunia dari JS!\");\n"
                                "html" -> "<!DOCTYPE html>\n<html>\n<body>\n  <h1>Halo Dunia</h1>\n</body>\n</html>"
                                "css" -> "body {\n  color: #00ffcc;\n  background: #0c0c14;\n}"
                                else -> ""
                            }
                            onCreateItem(node.path, newItemName.trim(), createIsFolder, defaultCode)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Ubah Nama") },
            text = {
                OutlinedTextField(
                    value = renameNewName,
                    onValueChange = { renameNewName = it },
                    label = { Text("Nama Baru") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameNewName.trim().isNotEmpty() && renameNewName != node.name) {
                            onRenameItem(renameNewName.trim())
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

fun getFlattenedTree(nodes: List<FileNode>): List<FileNode> {
    val result = mutableListOf<FileNode>()
    fun traverse(node: FileNode) {
        result.add(node)
        if (node.isDirectory && node.isExpanded) {
            node.children.forEach { traverse(it) }
        }
    }
    nodes.forEach { traverse(it) }
    return result
}

@Composable
fun SidebarCard(
    editorTheme: EditorTheme,
    workspacePath: String,
    onAttachFolder: () -> Unit,
    showRootNewFileDialog: () -> Unit,
    onRefresh: () -> Unit,
    fileSearchQuery: String,
    onFileSearchQueryChange: (String) -> Unit,
    workspaceFiles: List<FileNode>,
    selectedFile: CodeFile?,
    onSelectNode: (FileNode) -> Unit,
    onToggleExpand: (FileNode) -> Unit,
    onCreateItem: (parentPath: String?, name: String, isFolder: Boolean, defaultContent: String) -> Unit,
    onDeleteItem: (FileNode) -> Unit,
    onRenameItem: (FileNode, String) -> Unit,
    onDuplicateItem: (FileNode) -> Unit,
    onUploadFile: () -> Unit,
    editorThemeObject: EditorTheme = editorTheme
) {
    Card(
        modifier = Modifier
            .fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = editorTheme.background.copy(alpha = 0.9f)),
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // Workspace indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("WORKSPACE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(workspacePath, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = editorTheme.foreground)
                }
                var showSidebarMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showSidebarMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu Workspace",
                            tint = editorTheme.foreground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showSidebarMenu,
                        onDismissRequest = { showSidebarMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Unggah File") },
                            onClick = {
                                showSidebarMenu = false
                                onUploadFile()
                            },
                            leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Hubungkan Folder HP") },
                            onClick = {
                                showSidebarMenu = false
                                onAttachFolder()
                            },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action shortcuts inside Workspace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = showRootNewFileDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("File Baru", fontSize = 11.sp, color = Color.Black)
                }
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Segarkan", fontSize = 11.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Workspace file filter input
            OutlinedTextField(
                value = fileSearchQuery,
                onValueChange = onFileSearchQueryChange,
                placeholder = { Text("Cari file...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = TextStyle(fontSize = 12.sp)
            )

            Spacer(Modifier.height(8.dp))

            // Recursively rendered workspace tree
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (workspaceFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Workspace kosong. Buat file baru atau hubungkan folder HP Anda.", fontSize = 11.sp, color = Color.Gray)
                    }
                } else {
                    val flattenedNodes = getFlattenedTree(workspaceFiles)
                    val filteredNodes = flattenedNodes.filter {
                        fileSearchQuery.isEmpty() || it.name.contains(fileSearchQuery, ignoreCase = true)
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredNodes) { node ->
                            FileExplorerItem(
                                node = node,
                                isSelected = selectedFile?.githubPath == node.path,
                                onSelect = { onSelectNode(node) },
                                onToggleExpand = { onToggleExpand(node) },
                                onCreateItem = onCreateItem,
                                onDeleteItem = { onDeleteItem(node) },
                                onRenameItem = { newName -> onRenameItem(node, newName) },
                                onDuplicateItem = { onDuplicateItem(node) },
                                editorTheme = editorTheme
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperKeyboardAccessoryBar(
    editorTheme: EditorTheme,
    onInsertSymbol: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onMoveCursorLeft: () -> Unit,
    onMoveCursorRight: () -> Unit
) {
    val symbols = listOf("Tab", "{", "}", "[", "]", "(", ")", ";", "=", "\"", "'", "<", ">", "/", "_", "-", "+")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(editorTheme.background.copy(alpha = 0.95f))
            .border(1.dp, Color.Gray.copy(alpha = 0.2f))
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cursor movement keys
        IconButton(
            onClick = onMoveCursorLeft,
            modifier = Modifier
                .size(24.dp)
                .background(Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", tint = if (editorTheme.isDark) Color.White else Color.Black, modifier = Modifier.size(12.dp))
        }
        IconButton(
            onClick = onMoveCursorRight,
            modifier = Modifier
                .size(24.dp)
                .background(Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", tint = if (editorTheme.isDark) Color.White else Color.Black, modifier = Modifier.size(12.dp))
        }

        // Undo/Redo keys
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier
                .size(24.dp)
                .background(if (canUndo) Color.Gray.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
        ) {
            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (canUndo) (if (editorTheme.isDark) Color.White else Color.Black) else Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(12.dp))
        }
        IconButton(
            onClick = onRedo,
            enabled = canRedo,
            modifier = Modifier
                .size(24.dp)
                .background(if (canRedo) Color.Gray.copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
        ) {
            Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (canRedo) (if (editorTheme.isDark) Color.White else Color.Black) else Color.Gray.copy(alpha = 0.3f), modifier = Modifier.size(12.dp))
        }

        HorizontalDivider(modifier = Modifier.height(20.dp).width(1.dp), color = Color.Gray.copy(alpha = 0.3f))

        symbols.forEach { symbol ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.15f))
                    .clickable {
                        if (symbol == "Tab") {
                            onInsertSymbol("    ")
                        } else {
                            onInsertSymbol(symbol)
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = symbol,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (editorTheme.isDark) Color.White else Color.Black
                )
            }
        }
    }
}
