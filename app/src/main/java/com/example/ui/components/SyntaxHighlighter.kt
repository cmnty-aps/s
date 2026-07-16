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
                "vs code dark" -> VSCodeDark
                "nord" -> Nord
                "dracula" -> Dracula
                "one dark" -> OneDark
                "solarized dark" -> SolarizedDark
                else -> VSCodeDark
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
            val numberPatternStr = "\\b(0x[0-9a-fA-F]+|0b[01]+|\\d+(\\.\\d+)?([eE][+-]?\\d+)?)\\b"
            applyRegex(text, numberPatternStr, SpanStyle(color = theme.numberColor))

            // 4. Highlight Types
            val typePatternStr = when (lang) {
                "kotlin", "java" -> "\\b(String|Int|Long|Double|Float|Boolean|Char|Byte|Short|Any|Unit|Void|List|Map|Set|Array|Collection|Sequence|ViewModel|StateFlow|MutableStateFlow|Context|Uri|Activity|Intent|Toast|Button|Text|Column|Row|Box|Card|Modifier|Color|Exception|Throwable|Override|it|this|super|field|value|null|true|false|Result|Pair|Triple|Optional|Stream|List|ArrayList|LinkedList|HashMap|HashSet|TreeMap|TreeSet|Queue|Deque|Stack|Thread|Runnable|Callable|Future|CompletableFuture)\\b"
                "python" -> "\\b(str|int|float|bool|list|dict|set|tuple|object|None|self|cls|Exception|TypeError|ValueError|IndexError|KeyError|AttributeError|StopIteration|Generator|Iterator|Iterable|Mapping|Sequence|Set|AbstractContextManager|ContextManager|Coroutine|Awaitable|Callable|Any|Union|Optional|List|Dict|Tuple|Set|Type|NoReturn|ClassVar|Final|Literal|Protocol|TypedDict)\\b"
                "javascript", "js", "node.js", "node", "typescript", "ts" -> "\\b(String|Number|Boolean|Array|Object|Function|Promise|Symbol|Map|Set|Error|undefined|JSON|Math|Date|RegExp|Proxy|Any|Void|Never|Unknown|BigInt|Int8Array|Uint8Array|Uint16Array|Int16Array|Int32Array|Uint32Array|Float32Array|Float64Array|BigInt64Array|BigUint64Array|Buffer|process|window|document|console|global|module|exports|require|URL|URLSearchParams|Blob|File|FormData|ReadableStream|WritableStream|AbortController|Event|CustomEvent|Storage|Location|History|Navigator|Performance|Screen|HTMLDocument|HTMLElement|HTMLInputElement|HTMLButtonElement|HTMLDivElement|HTMLSpanElement|HTMLImageElement|HTMLCanvasElement|HTMLAnchorElement|HTMLFormElement|HTMLSelectElement|HTMLOptionElement|HTMLTextAreaElement|HTMLVideoElement|HTMLAudioElement)\\b"
                "c", "cpp", "c++" -> "\\b(int|float|double|char|void|size_t|bool|string|vector|map|set|std|uint8_t|uint16_t|uint32_t|uint64_t|int8_t|int16_t|int32_t|int64_t|auto|nullptr|FILE|DIR|time_t|clock_t|ssize_t|off_t|mode_t|pid_t|uid_t|gid_t|size_t|uintptr_t|intptr_t|ptrdiff_t|wchar_t|char16_t|char32_t|complex|imaginary)\\b"
                "rust" -> "\\b(i8|i16|i32|i64|i128|u8|u16|u32|u64|u128|f32|f64|str|String|bool|char|Option|Result|Vec|HashMap|Box|Self|usize|isize|Option|Some|None|Ok|Err|Arc|Rc|RefCell|Mutex|RwLock|Duration|Instant|BTreeMap|BTreeSet|LinkedList|VecDeque|BinaryHeap|HashIndex|RawPtr|Pinned|Future|Stream|Send|Sync|Unpin|Clone|Copy|Debug|Default|Display|Error|From|Into|AsRef|AsMut|PartialEq|Eq|PartialOrd|Ord|Hash|Drop|Sized|Fn|FnMut|FnOnce)\\b"
                "go" -> "\\b(string|int|int8|int16|int32|int64|uint|uint8|uint16|uint32|uint64|float32|float64|bool|byte|rune|error|any|complex64|complex128|uintptr|chan|map|interface|func|struct|Context|Reader|Writer|Closer|ReadWriter|Buffer|Error|Time|Duration|Channel|Slice|Map|Interface)\\b"
                "php" -> "\\b(int|float|string|bool|array|object|callable|iterable|resource|null|void|mixed|never|false|true|stdClass|Exception|Error|Throwable|ArrayObject|DateTime|DateTimeImmutable|DateTimeZone|DateInterval|DatePeriod|Closure|Generator|WeakReference|WeakMap|Stringable|Iterator|Aggregate|ArrayAccess|Serializable|Countable|OuterIterator|RecursiveIterator|SeekableIterator|SplObserver|SplSubject)\\b"
                "ruby" -> "\\b(String|Integer|Float|Array|Hash|Symbol|Range|Regexp|TrueClass|FalseClass|NilClass|Class|Module|Object|Kernel|Binding|Proc|Method|UnboundMethod|Thread|ThreadGroup|Mutex|ConditionVariable|Queue|SizedQueue|Exception|StandardError|RuntimeError|Struct|Data|File|Dir|IO|Math|Process|Signal|Time|Random|Range|Enumerable|Comparable)\\b"
                "swift" -> "\\b(String|Int|Float|Double|Bool|Character|Array|Dictionary|Set|Optional|Any|AnyObject|Void|Int8|Int16|Int32|Int64|UInt|UInt8|UInt16|UInt32|UInt64|Float80|CGFloat|Error|Protocol|Type|Self|self|static|class|struct|enum|extension|func|var|let|if|else|switch|case|default|for|while|repeat|do|try|catch|throw|return|break|continue|fallthrough|guard|defer|import|public|internal|private|fileprivate|open|final|override|required|optional|lazy|weak|unowned|typealias|associatedtype|generic|where|dynamic|convenience|mutating|nonmutating|infix|prefix|postfix|operator|precedencegroup|associatedtype|none|some|any)\\b"
                "csharp", "cs" -> "\\b(string|int|long|short|byte|sbyte|uint|ulong|ushort|float|double|decimal|bool|char|object|dynamic|void|List|Dictionary|IEnumerable|ICollection|IList|Task|Exception|DateTime|TimeSpan|Guid|Console|Math|Array|Int32|Int64|UInt32|UInt64|Boolean|String|Object|Type|Enum|Delegate|EventHandler|Action|Func|Predicate|Tuple|ValueTuple|Nullable|Span|ReadOnlySpan|Memory|ReadOnlyMemory|CancellationToken|Stream|File|Directory|Path|Socket|HttpClient|Thread|TaskCompletionSource)\\b"
                else -> ""
            }
            if (typePatternStr.isNotEmpty()) {
                applyRegex(text, typePatternStr, SpanStyle(color = theme.typeColor, fontWeight = FontWeight.Medium))
            }

            // 5. Highlight Functions
            val functionPatternStr = "\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\()"
            applyRegex(text, functionPatternStr, SpanStyle(color = theme.functionColor, fontWeight = FontWeight.SemiBold))

            // 6. Highlight Keywords or specific patterns depending on language
            if (lang == "html" || lang == "xml") {
                // XML/HTML Doctype
                val htmlDoctypePattern = "<![dD][oO][cC][tT][yY][pP][eE][^>]*>"
                applyRegex(text, htmlDoctypePattern, SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold))

                // XML/HTML Brackets
                val htmlBracketPatternStr = "</?|/?>"
                applyRegex(text, htmlBracketPatternStr, SpanStyle(color = theme.tagColor))

                // XML/HTML tags
                val htmlTagPatternStr = "(?<=</?)[a-zA-Z0-9:-]+"
                applyRegex(text, htmlTagPatternStr, SpanStyle(color = theme.tagColor, fontWeight = FontWeight.Bold))

                // XML/HTML attributes
                val htmlAttrPatternStr = "\\b[a-zA-Z0-9:-]+(?=\\s*=)"
                applyRegex(text, htmlAttrPatternStr, SpanStyle(color = theme.attrColor))

                // HTML Entities
                val htmlEntityPattern = "&[a-zA-Z0-9#]+;"
                applyRegex(text, htmlEntityPattern, SpanStyle(color = theme.functionColor))

                // CSS in Style Tags (simplified but richer)
                val cssPropertyPattern = "(?<=[:{;])\\s*[a-zA-Z-]+(?=\\s*:)"
                applyRegex(text, cssPropertyPattern, SpanStyle(color = theme.attrColor))

                // CSS class selectors inside style blocks (e.g. .class-name {)
                val htmlCssClassPattern = "\\.[a-zA-Z0-9_-]+(?=\\s*\\{)"
                applyRegex(text, htmlCssClassPattern, SpanStyle(color = theme.functionColor))

                // CSS id selectors inside style blocks (e.g. #id-name {)
                val htmlCssIdPattern = "#[a-zA-Z0-9_-]+(?=\\s*\\{)"
                applyRegex(text, htmlCssIdPattern, SpanStyle(color = theme.typeColor, fontWeight = FontWeight.SemiBold))

                // CSS hex colors
                val htmlCssHexPattern = "#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\\b"
                applyRegex(text, htmlCssHexPattern, SpanStyle(color = theme.numberColor))

                // CSS units inside style tags
                val htmlCssUnitPattern = "(?<=\\d)(px|rem|em|vh|vw|%|deg|s|ms)\\b"
                applyRegex(text, htmlCssUnitPattern, SpanStyle(color = theme.keywordColor))
            } else if (lang == "css") {
                // 1. Selector Tags (HTML elements)
                val cssTagPattern = "\\b(html|body|div|span|p|a|ul|ol|li|h1|h2|h3|h4|h5|h6|img|button|input|textarea|select|form|header|footer|section|article|aside|nav|main|canvas|style|script|svg|path|g|iframe|hr|br|table|thead|tbody|tfoot|tr|th|td|audio|video|picture|object|embed|figure|figcaption|details|summary|dialog|meta|link|title|head)\\b"
                applyRegex(text, cssTagPattern, SpanStyle(color = theme.tagColor, fontWeight = FontWeight.Bold))

                // 2. Class Selectors: .class-name
                val cssClassPattern = "\\.[a-zA-Z_][a-zA-Z0-9_-]*"
                applyRegex(text, cssClassPattern, SpanStyle(color = theme.functionColor))

                // 3. ID Selectors: #id-name
                val cssIdPattern = "#[a-zA-Z_][a-zA-Z0-9_-]*"
                applyRegex(text, cssIdPattern, SpanStyle(color = theme.typeColor, fontWeight = FontWeight.SemiBold))

                // 4. Pseudo-classes & Pseudo-elements: :hover, :active, ::before, :nth-child
                val cssPseudoPattern = "::?[a-zA-Z0-9_-]+(\\([^)]*\\))?"
                applyRegex(text, cssPseudoPattern, SpanStyle(color = theme.keywordColor))

                // 5. CSS Properties: color, background-color, etc. (followed by :)
                val cssPropertyPattern = "[a-zA-Z0-9_-]+(?=\\s*:)"
                applyRegex(text, cssPropertyPattern, SpanStyle(color = theme.attrColor))

                // 6. CSS values and keywords
                val cssValueKeywords = "\\b(block|inline-block|inline|flex|grid|none|absolute|relative|fixed|sticky|static|inherit|initial|unset|auto|center|left|right|justify|uppercase|lowercase|capitalize|bold|normal|italic|sans-serif|serif|monospace|pointer|default|border-box|content-box|wrap|nowrap|row|column|both|hidden|visible|scroll|transparent|solid|dashed|dotted|double|none|important)\\b"
                applyRegex(text, cssValueKeywords, SpanStyle(color = theme.keywordColor))

                // 7. CSS Hex Colors
                val cssHexColorPattern = "#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\\b"
                applyRegex(text, cssHexColorPattern, SpanStyle(color = theme.numberColor))

                // 8. CSS Custom Properties / Variables: --primary-color
                val cssVarPattern = "--[a-zA-Z0-9_-]+"
                applyRegex(text, cssVarPattern, SpanStyle(color = theme.typeColor))

                // 9. CSS Units (px, em, rem, %, vh, vw, etc.)
                val cssUnitPattern = "(?<=\\d)(px|rem|em|vh|vw|%|deg|s|ms|pt|pc|ex|ch|fr)\\b"
                applyRegex(text, cssUnitPattern, SpanStyle(color = theme.keywordColor))

                // 10. CSS At-rules: @media, @import, @keyframes
                val cssAtRulePattern = "@[a-zA-Z0-9_-]+"
                applyRegex(text, cssAtRulePattern, SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold))
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
                    "python" -> "\\b(def|class|import|from|return|if|elif|else|while|for|in|try|except|finally|print|as|with|lambda|and|or|not|True|False|None|assert|break|continue|del|global|nonlocal|pass|raise|yield|is|type|len|range|open|list|dict|set|tuple|enumerate|zip|map|filter|all|any|str|int|float|bool|input|abs|round|min|max|sum|sorted|reversed|help|dir|vars|globals|locals|eval|exec)\\b"
                    "javascript", "js", "node.js", "node", "typescript", "ts" -> "\\b(const|let|var|function|return|if|else|for|while|import|export|from|require|class|try|catch|console|async|await|new|this|typeof|instanceof|true|false|null|undefined|break|continue|default|do|switch|case|throw|extends|super|debugger|in|of|yield|interface|enum|type|namespace|as|get|set|static|private|protected|public|readonly|module|declare|abstract|implements|keyof|readonly|infer)\\b"
                    "kotlin" -> "\\b(val|var|fun|class|interface|object|import|package|return|if|else|when|for|while|try|catch|finally|true|false|null|this|super|private|protected|public|internal|break|continue|constructor|delegate|init|as|is|in|throw|out|companion|by|get|set|field|suspend|coroutineScope|launch|async|flow|collect|withContext|inline|noinline|crossinline|external|tailrec|operator|infix|sealed|data|annotation|tailrec|lateinit|inner|abstract|open|final|override|reified|vararg|expect|actual)\\b"
                    "java" -> "\\b(class|interface|enum|extends|implements|import|package|public|private|protected|static|final|abstract|native|synchronized|transient|volatile|strictfp|return|if|else|switch|case|default|while|do|for|break|continue|try|catch|finally|throw|throws|new|this|super|instanceof|true|false|null|void|boolean|char|byte|short|int|long|float|double|record|sealed|permits|non-sealed|yield|var|transient)\\b"
                    "sql" -> "(?i)\\b(select|from|where|join|left|right|inner|outer|on|insert|into|values|update|set|delete|create|table|index|drop|alter|add|column|primary|key|foreign|references|unique|not|null|default|check|constraint|and|or|not|in|between|like|is|as|group|by|having|order|limit|offset|count|sum|avg|min|max|union|all|distinct|exists|any|all|some|case|when|then|else|end|cast|coalesce|nullif|between|interval|returning|with|recursive|pivot|unpivot|window|over|partition|rank|dense_rank|row_number|lag|lead|first_value|last_value)\\b"
                    "c", "cpp", "c++" -> "\\b(if|else|while|for|do|return|break|continue|switch|case|default|class|struct|union|enum|typedef|public|private|protected|virtual|override|friend|inline|const|static|volatile|mutable|register|sizeof|new|delete|try|catch|throw|namespace|using|template|typename|operator|explicit|extern|goto|nullptr|alignas|alignof|asm|atomic|char16_t|char32_t|concept|consteval|constexpr|constinit|decltype|explicit|export|inline|mutable|noexcept|requires|static_assert|thread_local|typeid)\\b"
                    "rust" -> "\\b(fn|let|mut|impl|struct|enum|match|use|mod|pub|return|if|else|while|for|in|as|const|static|type|trait|unsafe|loop|break|continue|crate|extern|move|ref|self|where|dyn|async|await|macro_rules|Box|Option|Result|Some|None|Ok|Err|panic|vec|String|str|u8|u16|u32|u64|u128|usize|i8|i16|i32|i64|i128|isize|f32|f64|bool|char|Self|yield|super|macro)\\b"
                    "go" -> "\\b(func|var|const|type|struct|interface|package|import|return|if|else|switch|case|default|for|range|break|continue|go|select|chan|defer|map|iota|nil|true|false|make|new|len|cap|append|copy|delete|panic|recover|print|println|error)\\b"
                    "php" -> "\\b(function|class|interface|trait|extends|implements|namespace|use|return|if|else|elseif|while|for|foreach|as|do|switch|case|default|break|continue|try|catch|finally|throw|new|public|protected|private|static|final|abstract|const|echo|print|include|require|include_once|require_once|global|static|var|list|array|empty|isset|unset|null|true|false|static|parent|self|void|int|float|string|bool|callable|iterable|object|mixed|never|enum|readonly)\\b"
                    "ruby" -> "\\b(def|class|module|if|else|elsif|unless|while|until|for|in|begin|end|rescue|ensure|retry|yield|super|self|nil|true|false|and|or|not|next|break|redo|retry|alias|undef|defined?|do|then|case|when|module_function|public|protected|private|attr_accessor|attr_reader|attr_writer|extend|include|prepend|require|require_relative|load)\\b"
                    "shell", "sh", "bash" -> "\\b(if|then|else|elif|fi|case|esac|for|while|until|do|done|in|function|local|return|exit|break|continue|read|echo|printf|set|export|unset|alias|unalias|source|eval|exec|true|false|shift|test|trap|wait|umask)\\b"
                    "swift" -> "\\b(func|class|struct|enum|extension|protocol|init|deinit|var|let|if|else|switch|case|default|for|while|repeat|do|try|catch|throw|return|break|continue|fallthrough|guard|defer|import|public|internal|private|fileprivate|open|static|final|override|required|optional|lazy|weak|unowned|self|Self|nil|true|false|typealias|associatedtype|generic|where|dynamic|convenience|mutating|nonmutating|infix|prefix|postfix|operator|precedencegroup|associatedtype|none|some|any)\\b"
                    "csharp", "cs" -> "\\b(abstract|as|base|bool|break|byte|case|catch|char|checked|class|const|continue|decimal|default|delegate|do|double|else|enum|event|explicit|extern|false|finally|fixed|float|for|foreach|goto|if|implicit|in|int|interface|internal|is|lock|long|namespace|new|null|object|operator|out|override|params|private|protected|public|readonly|ref|return|sbyte|sealed|short|sizeof|stackalloc|static|string|struct|switch|this|throw|true|try|typeof|uint|ulong|unchecked|unsafe|ushort|using|virtual|void|volatile|while|add|alias|ascending|async|await|by|descending|dynamic|equals|from|get|global|group|into|join|let|nameof|on|orderby|partial|remove|select|set|unmanaged|value|var|when|where|with|yield)\\b"
                    else -> "\\b(if|else|while|for|return|class|import|function|def|let|const|var|public|private|val|fun)\\b"
                }
                applyRegex(text, keywordPatternStr, SpanStyle(color = theme.keywordColor, fontWeight = FontWeight.Bold))

                // Extra annotations/decorators
                if (lang == "kotlin" || lang == "java" || lang == "python") {
                    val annotationPatternStr = "@[a-zA-Z_][a-zA-Z0-9_.]*"
                    applyRegex(text, annotationPatternStr, SpanStyle(color = theme.operatorColor, fontWeight = FontWeight.Medium))
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
