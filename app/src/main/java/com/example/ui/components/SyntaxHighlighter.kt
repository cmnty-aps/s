package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.regex.Pattern

data class EditorTheme(
    val name: String,
    val background: Color,
    val foreground: Color,
    val keywordColor: Color,
    val functionColor: Color,
    val stringColor: Color,
    val numberColor: Color,
    val commentColor: Color,
    val operatorColor: Color,
    val typeColor: Color,
    val tagColor: Color,
    val attrColor: Color,
    val bracketColor: Color,
    val isDark: Boolean = true
) {
    companion object {
        val Monokai = EditorTheme(
            name = "Monokai",
            background = Color(0xFF272822),
            foreground = Color(0xFFF8F8F2),
            keywordColor = Color(0xFFF92672), // Pink
            functionColor = Color(0xFF66D9EF), // Blue
            stringColor = Color(0xFFE6DB74), // Yellow
            numberColor = Color(0xFFAE81FF), // Purple
            commentColor = Color(0xFF75715E), // Gray/Green
            operatorColor = Color(0xFFFD971F), // Orange
            typeColor = Color(0xFF66D9EF), // Light Blue
            tagColor = Color(0xFFF92672), // Pink
            attrColor = Color(0xFFA6E22E), // Green
            bracketColor = Color(0xFFF8F8F2),
            isDark = true
        )

        val VSCodeDark = EditorTheme(
            name = "VS Code Dark",
            background = Color(0xFF1E1E1E),
            foreground = Color(0xFFD4D4D4),
            keywordColor = Color(0xFF569CD6), // Blue
            functionColor = Color(0xFFDCDCAA), // Light Yellow
            stringColor = Color(0xFFCE9178), // Orange/Brown
            numberColor = Color(0xFFB5CEA8), // Green/Gray
            commentColor = Color(0xFF6A9955), // Green
            operatorColor = Color(0xFFD4D4D4),
            typeColor = Color(0xFF4EC9B0), // Teal
            tagColor = Color(0xFF569CD6), // Blue
            attrColor = Color(0xFF9CDCFE), // Light Blue
            bracketColor = Color(0xFFF1D700), // Yellow
            isDark = true
        )

        val RetroTerminal = EditorTheme(
            name = "Retro Terminal",
            background = Color(0xFF000000),
            foreground = Color(0xFF33FF33), // Terminal Green
            keywordColor = Color(0xFF00FF00),
            functionColor = Color(0xFF88FF88),
            stringColor = Color(0xFF00FFFF), // Cyan
            numberColor = Color(0xFFFFFF00), // Yellow
            commentColor = Color(0xFF555555), // Dark Gray
            operatorColor = Color(0xFF33FF33),
            typeColor = Color(0xFF88FF88),
            tagColor = Color(0xFF00FF00),
            attrColor = Color(0xFF00FFFF),
            bracketColor = Color(0xFF00FF00),
            isDark = true
        )

        val GithubLight = EditorTheme(
            name = "Github Light",
            background = Color(0xFFFFFFFF),
            foreground = Color(0xFF24292E),
            keywordColor = Color(0xFFD73A49), // Red
            functionColor = Color(0xFF6F42C1), // Purple
            stringColor = Color(0xFF032F62), // Dark Blue
            numberColor = Color(0xFF005CC5), // Light Blue
            commentColor = Color(0xFF6A737D), // Gray
            operatorColor = Color(0xFFD73A49),
            typeColor = Color(0xFFE36209), // Orange
            tagColor = Color(0xFF22863A), // Green
            attrColor = Color(0xFF6F42C1), // Purple
            bracketColor = Color(0xFF24292E),
            isDark = false
        )

        val Nord = EditorTheme(
            name = "Nord",
            background = Color(0xFF2E3440),
            foreground = Color(0xFFD8DEE9),
            keywordColor = Color(0xFF81A1C1),
            functionColor = Color(0xFF88C0D0),
            stringColor = Color(0xFFA3BE8C),
            numberColor = Color(0xFFB48EAD),
            commentColor = Color(0xFF4C566A),
            operatorColor = Color(0xFF81A1C1),
            typeColor = Color(0xFF8FBCBB),
            tagColor = Color(0xFF81A1C1),
            attrColor = Color(0xFFD08770),
            bracketColor = Color(0xFFD8DEE9),
            isDark = true
        )

        val Dracula = EditorTheme(
            name = "Dracula",
            background = Color(0xFF282A36),
            foreground = Color(0xFFF8F8F2),
            keywordColor = Color(0xFFFF79C6),
            functionColor = Color(0xFF50FA7B),
            stringColor = Color(0xFFF1FA8C),
            numberColor = Color(0xFFBD93F9),
            commentColor = Color(0xFF6272A4),
            operatorColor = Color(0xFFFF79C6),
            typeColor = Color(0xFF8BE9FD),
            tagColor = Color(0xFFFF79C6),
            attrColor = Color(0xFF50FA7B),
            bracketColor = Color(0xFFF8F8F2),
            isDark = true
        )

        val OneDark = EditorTheme(
            name = "One Dark",
            background = Color(0xFF282C34),
            foreground = Color(0xFFABB2BF),
            keywordColor = Color(0xFFC678DD),
            functionColor = Color(0xFF61AFEF),
            stringColor = Color(0xFF98C379),
            numberColor = Color(0xFFD19A66),
            commentColor = Color(0xFF5C6370),
            operatorColor = Color(0xFFC678DD),
            typeColor = Color(0xFFE5C07B),
            tagColor = Color(0xFFE06C75),
            attrColor = Color(0xFFD19A66),
            bracketColor = Color(0xFFABB2BF),
            isDark = true
        )

        val SolarizedDark = EditorTheme(
            name = "Solarized Dark",
            background = Color(0xFF002B36),
            foreground = Color(0xFF839496),
            keywordColor = Color(0xFF859900),
            functionColor = Color(0xFF268BD2),
            stringColor = Color(0xFF2AA198),
            numberColor = Color(0xFFD33682),
            commentColor = Color(0xFF586E75),
            operatorColor = Color(0xFF859900),
            typeColor = Color(0xFFB58900),
            tagColor = Color(0xFF268BD2),
            attrColor = Color(0xFFB58900),
            bracketColor = Color(0xFF839496),
            isDark = true
        )

        fun fromName(name: String): EditorTheme {
            return when (name.lowercase()) {
                "monokai" -> Monokai
                "vs code dark" -> VSCodeDark
                "retro terminal" -> RetroTerminal
                "github light" -> GithubLight
                "nord" -> Nord
                "dracula" -> Dracula
                "one dark" -> OneDark
                "solarized dark" -> SolarizedDark
                else -> Monokai
            }
        }
    }
}

object SyntaxHighlighter {

    fun highlight(text: String, language: String, themeName: String): AnnotatedString {
        val theme = EditorTheme.fromName(themeName)
        val builder = buildAnnotatedString {
            // Start with base text styling
            append(text)
            addStyle(SpanStyle(color = theme.foreground, fontFamily = FontFamily.Monospace), 0, text.length)

            val lang = language.lowercase()

            // 1. Highlight Brackets/Parentheses (least priority, overridden by operators/keywords/strings/comments)
            val bracketPatternStr = "[\\[\\]{}()]"
            applyRegex(text, bracketPatternStr, SpanStyle(color = theme.bracketColor))

            // 2. Highlight Operators
            val operatorPatternStr = "[-+*/%=<>!&|^~?:]"
            applyRegex(text, operatorPatternStr, SpanStyle(color = theme.operatorColor))

            // 3. Highlight Numbers
            val numberPatternStr = "\\b\\d+(\\.\\d+)?\\b"
            applyRegex(text, numberPatternStr, SpanStyle(color = theme.numberColor))

            // 4. Highlight Types
            val typePatternStr = when (lang) {
                "kotlin", "java" -> "\\b(String|Int|Long|Double|Float|Boolean|Char|Byte|Short|Any|Unit|Void|List|Map|Set|Array|Collection|Sequence|ViewModel|StateFlow|MutableStateFlow|Context|Uri|Activity|Intent|Toast|Button|Text|Column|Row|Box|Card|Modifier|Color|Exception|Throwable|Override)\\b"
                "python" -> "\\b(str|int|float|bool|list|dict|set|tuple|object|None|self|cls|Exception|TypeError|ValueError)\\b"
                "javascript", "js", "node.js", "node" -> "\\b(String|Number|Boolean|Array|Object|Function|Promise|Symbol|Map|Set|Error|undefined|JSON|Math|Date|RegExp|Proxy)\\b"
                "c", "cpp", "c++" -> "\\b(int|float|double|char|void|size_t|bool|string|vector|map|set|std|uint8_t|uint16_t|uint32_t|uint64_t|int8_t|int16_t|int32_t|int64_t)\\b"
                "rust" -> "\\b(i8|i16|i32|i64|i128|u8|u16|u32|u64|u128|f32|f64|str|String|bool|char|Option|Result|Vec|HashMap|Box|Self|usize|isize)\\b"
                "go" -> "\\b(string|int|int8|int16|int32|int64|uint|uint8|uint16|uint32|uint64|float32|float64|bool|byte|rune|error|any)\\b"
                else -> ""
            }
            if (typePatternStr.isNotEmpty()) {
                applyRegex(text, typePatternStr, SpanStyle(color = theme.typeColor, fontWeight = FontWeight.Medium))
            }

            // 5. Highlight Functions
            val functionPatternStr = "\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\)"
            applyRegex(text, functionPatternStr, SpanStyle(color = theme.functionColor, fontWeight = FontWeight.SemiBold))

            // 6. Highlight Keywords or specific patterns depending on language
            if (lang == "html" || lang == "xml") {
                // XML/HTML tags
                val htmlTagPatternStr = "(?<=</?)[a-zA-Z0-9:-]+"
                applyRegex(text, htmlTagPatternStr, SpanStyle(color = theme.tagColor, fontWeight = FontWeight.Bold))

                // XML/HTML attributes
                val htmlAttrPatternStr = "\\b[a-zA-Z0-9:-]+(?=\\s*=)"
                applyRegex(text, htmlAttrPatternStr, SpanStyle(color = theme.attrColor))
            } else if (lang == "json") {
                // JSON Keys
                val jsonKeyPatternStr = "\"[^\"]+\"(?=\\s*:)"
                applyRegex(text, jsonKeyPatternStr, SpanStyle(color = theme.attrColor, fontWeight = FontWeight.Bold))
            } else if (lang == "markdown" || lang == "md") {
                // Headers: # Header
                val mdHeaderPatternStr = "(?m)^#{1,6}\\s+.*$"
                applyRegex(text, mdHeaderPatternStr, SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold))

                // Lists: - list item or 1. list item
                val mdListPatternStr = "(?m)^\\s*([-*+]|\\d+\\.)\\s"
                applyRegex(text, mdListPatternStr, SpanStyle(color = theme.functionColor, fontWeight = FontWeight.Bold))

                // Bold
                val mdBoldPatternStr = "\\*\\*[^*\\n]+\\*\\*"
                applyRegex(text, mdBoldPatternStr, SpanStyle(fontWeight = FontWeight.Bold))

                // Italic
                val mdItalicPatternStr = "\\*[^*\\n]+\\*"
                applyRegex(text, mdItalicPatternStr, SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))

                // Inline code: `code`
                val mdCodePatternStr = "`[^`\\n]+`"
                applyRegex(text, mdCodePatternStr, SpanStyle(color = theme.stringColor, fontFamily = FontFamily.Monospace))
            } else {
                val keywordPatternStr = when (lang) {
                    "python" -> "\\b(def|class|import|from|return|if|elif|else|while|for|in|try|except|print|as|with|lambda|and|or|not|True|False|None|assert|break|continue|del|global|nonlocal|pass|raise|yield)\\b"
                    "javascript", "js", "node.js", "node" -> "\\b(const|let|var|function|return|if|else|for|while|import|export|from|require|class|try|catch|console|async|await|new|this|typeof|instanceof|true|false|null|undefined|break|continue|default|do|switch|case|throw|extends|super|debugger|in|of|yield)\\b"
                    "kotlin" -> "\\b(val|var|fun|class|interface|object|import|package|return|if|else|when|for|while|try|catch|finally|true|false|null|this|super|private|protected|public|internal|break|continue|constructor|delegate|init|as|is|in|throw|out|companion|by|get|set|field|suspend|coroutineScope|launch|async|flow|collect|withContext)\\b"
                    "java" -> "\\b(class|interface|enum|extends|implements|import|package|public|private|protected|static|final|abstract|native|synchronized|transient|volatile|strictfp|return|if|else|switch|case|default|while|do|for|break|continue|try|catch|finally|throw|throws|new|this|super|instanceof|true|false|null|void|boolean|char|byte|short|int|long|float|double)\\b"
                    "css" -> "\\b(color|background|margin|padding|border|display|position|width|height|font|text|align|justify|align|items|flex|grid|box|shadow|border|radius|opacity|transition|animation|z|index|top|left|right|bottom|overflow|float|clear|cursor|visibility|pointer|events|none|important|media|keyframes)\\b"
                    "sql" -> "(?i)\\b(select|from|where|join|left|right|inner|outer|on|insert|into|values|update|set|delete|create|table|index|drop|alter|add|column|primary|key|foreign|references|unique|not|null|default|check|constraint|and|or|not|in|between|like|is|as|group|by|having|order|limit|offset|count|sum|avg|min|max|union|all|distinct)\\b"
                    "c", "cpp", "c++" -> "\\b(if|else|while|for|do|return|break|continue|switch|case|default|class|struct|union|enum|typedef|public|private|protected|virtual|override|friend|inline|const|static|volatile|mutable|register|sizeof|new|delete|try|catch|throw|namespace|using|template|typename|operator|explicit|extern|goto)\\b"
                    "rust" -> "\\b(fn|let|mut|impl|struct|enum|match|use|mod|pub|return|if|else|while|for|in|as|const|static|type|trait|unsafe|loop|break|continue|crate|extern|move|ref|self|where|dyn|async|await|macro_rules)\\b"
                    "go" -> "\\b(func|package|import|var|const|type|struct|interface|return|if|else|for|range|switch|case|default|go|chan|map|select|defer|fallthrough|goto|type|package|import|interface)\\b"
                    else -> "\\b(if|else|while|for|return|class|import|function|def|let|const|var|public|private|val|fun)\\b"
                }
                applyRegex(text, keywordPatternStr, SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold))

                // Extra annotations for Kotlin/Java/Python
                if (lang == "kotlin" || lang == "java") {
                    val annotationPatternStr = "@[a-zA-Z_][a-zA-Z0-9_]*"
                    applyRegex(text, annotationPatternStr, SpanStyle(color = theme.operatorColor, fontWeight = FontWeight.Medium))
                } else if (lang == "python") {
                    val decoratorPatternStr = "@[a-zA-Z_][a-zA-Z0-9_]*"
                    applyRegex(text, decoratorPatternStr, SpanStyle(color = theme.operatorColor, fontWeight = FontWeight.Medium))
                }
            }

            // 7. Highlight Strings (overrides previous highlighting)
            val stringPatternStr = when (lang) {
                "python" -> "\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"[^\"]*\"|'[^']*'"
                "json" -> "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\""
                else -> "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'|`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`"
            }
            if (lang != "json" && lang != "markdown" && lang != "md") {
                applyRegex(text, stringPatternStr, SpanStyle(color = theme.stringColor))
            } else if (lang == "json") {
                // Apply string styling to values only (not the keys, which were styled above)
                applyRegex(text, stringPatternStr, SpanStyle(color = theme.stringColor))
                // Re-apply key styling to key strings so they take precedence
                val jsonKeyPatternStr = "\"[^\"]+\"(?=\\s*:)"
                applyRegex(text, jsonKeyPatternStr, SpanStyle(color = theme.attrColor, fontWeight = FontWeight.Bold))
            }

            // 8. Highlight Comments (highest precedence to override strings/keywords/numbers)
            val commentPatternStr = when (lang) {
                "python" -> "#.*"
                "html", "xml", "markdown", "md" -> "<!--[\\s\\S]*?-->"
                "css" -> "/\\*[\\s\\S]*?\\*/"
                "sql" -> "--.*|/\\*[\\s\\S]*?\\*/"
                else -> "//.*|/\\*[\\s\\S]*?\\*/"
            }
            applyRegex(text, commentPatternStr, SpanStyle(color = theme.commentColor, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
        }
        return builder
    }

    fun parseHljsHtml(html: String, theme: EditorTheme): AnnotatedString {
        val builder = AnnotatedString.Builder()
        var i = 0
        val n = html.length
        
        val activeRanges = mutableListOf<Triple<Int, SpanStyle, String>>() // start index, style, classname
        val cleanText = StringBuilder()
        
        while (i < n) {
            if (html.startsWith("<span ", i)) {
                val classStart = html.indexOf("class=\"", i + 6)
                if (classStart != -1) {
                    val classEnd = html.indexOf("\"", classStart + 7)
                    if (classEnd != -1) {
                        val className = html.substring(classStart + 7, classEnd)
                        val style = getStyleForClass(className, theme)
                        activeRanges.add(Triple(cleanText.length, style, className))
                    }
                }
                val tagEnd = html.indexOf(">", i)
                if (tagEnd != -1) {
                    i = tagEnd + 1
                } else {
                    i += 6
                }
            } else if (html.startsWith("</span>", i)) {
                if (activeRanges.isNotEmpty()) {
                    val lastRange = activeRanges.removeAt(activeRanges.lastIndex)
                    builder.addStyle(lastRange.second, lastRange.first, cleanText.length)
                }
                i += 7
            } else if (html[i] == '&') {
                if (html.startsWith("&lt;", i)) {
                    cleanText.append('<')
                    i += 4
                } else if (html.startsWith("&gt;", i)) {
                    cleanText.append('>')
                    i += 4
                } else if (html.startsWith("&amp;", i)) {
                    cleanText.append('&')
                    i += 5
                } else if (html.startsWith("&quot;", i)) {
                    cleanText.append('"')
                    i += 6
                } else if (html.startsWith("&#x27;", i) || html.startsWith("&#39;", i)) {
                    cleanText.append('\'')
                    i += 6
                } else {
                    cleanText.append('&')
                    i++
                }
            } else {
                cleanText.append(html[i])
                i++
            }
        }
        
        builder.append(cleanText.toString())
        for (range in activeRanges) {
            builder.addStyle(range.second, range.first, cleanText.length)
        }
        return builder.toAnnotatedString()
    }

    private fun getStyleForClass(className: String, theme: EditorTheme): SpanStyle {
        return when {
            className.contains("keyword") || className.contains("built_in") || className.contains("selector-tag") -> {
                SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold)
            }
            className.contains("title function") || className.contains("function") || className.contains("title") -> {
                SpanStyle(color = theme.functionColor, fontWeight = FontWeight.SemiBold)
            }
            className.contains("string") || className.contains("regexp") || className.contains("meta") -> {
                SpanStyle(color = theme.stringColor)
            }
            className.contains("number") || className.contains("literal") || className.contains("variable") || className.contains("params") -> {
                SpanStyle(color = theme.numberColor)
            }
            className.contains("comment") || className.contains("quote") -> {
                SpanStyle(color = theme.commentColor, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
            className.contains("operator") || className.contains("punctuation") -> {
                SpanStyle(color = theme.operatorColor)
            }
            className.contains("type") || className.contains("class") -> {
                SpanStyle(color = theme.typeColor, fontWeight = FontWeight.Medium)
            }
            className.contains("tag") || className.contains("name") -> {
                SpanStyle(color = theme.tagColor, fontWeight = FontWeight.Bold)
            }
            className.contains("attr") || className.contains("attribute") -> {
                SpanStyle(color = theme.attrColor)
            }
            else -> SpanStyle(color = theme.foreground)
        }
    }

    private fun AnnotatedString.Builder.applyRegex(text: String, patternStr: String, style: SpanStyle) {
        if (patternStr.isEmpty()) return
        try {
            val pattern = Pattern.compile(patternStr)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                addStyle(style, matcher.start(), matcher.end())
            }
        } catch (e: Exception) {
            // Silent catch to prevent regex crash on typing half-open comments/strings
        }
    }
}

class CodeVisualTransformation(
    private val language: String,
    private val themeName: String,
    private val highlightedString: AnnotatedString?
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (highlightedString != null && highlightedString.text == text.text) {
            return TransformedText(highlightedString, OffsetMapping.Identity)
        }
        val highlighted = SyntaxHighlighter.highlight(text.text, language, themeName)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
