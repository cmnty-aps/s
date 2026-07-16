package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

object TerminalHighlighter {
    private val ansiRegex = Regex("\u001B\\[([0-9;]*)m")

    fun parseAnsiToAnnotatedString(text: String): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            var currentStyle = SpanStyle(color = Color(0xFFECEFF1))
            
            val matches = ansiRegex.findAll(text)
            
            for (match in matches) {
                // Add text before the match
                if (match.range.first > currentIndex) {
                    withStyle(currentStyle) {
                        append(text.substring(currentIndex, match.range.first))
                    }
                }
                
                // Update style based on the code
                val codeString = match.groupValues[1]
                val codes = if (codeString.isEmpty()) listOf(0) else codeString.split(";").mapNotNull { it.toIntOrNull() }
                
                for (code in codes) {
                    when (code) {
                        0 -> currentStyle = SpanStyle(color = Color(0xFFECEFF1)) // Reset
                        1 -> currentStyle = currentStyle.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        3 -> currentStyle = currentStyle.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        4 -> currentStyle = currentStyle.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                        30 -> currentStyle = currentStyle.copy(color = Color.Black)
                        31 -> currentStyle = currentStyle.copy(color = Color.Red)
                        32 -> currentStyle = currentStyle.copy(color = Color.Green)
                        33 -> currentStyle = currentStyle.copy(color = Color(0xFFFFC107)) // Yellow
                        34 -> currentStyle = currentStyle.copy(color = Color(0xFF2196F3)) // Blue
                        35 -> currentStyle = currentStyle.copy(color = Color(0xFF9C27B0)) // Magenta
                        36 -> currentStyle = currentStyle.copy(color = Color(0xFF00BCD4)) // Cyan
                        37 -> currentStyle = currentStyle.copy(color = Color.White)
                        90 -> currentStyle = currentStyle.copy(color = Color.Gray)
                        91 -> currentStyle = currentStyle.copy(color = Color(0xFFFF5252))
                        92 -> currentStyle = currentStyle.copy(color = Color(0xFF69F0AE))
                        93 -> currentStyle = currentStyle.copy(color = Color(0xFFFFE528))
                        94 -> currentStyle = currentStyle.copy(color = Color(0xFF448AFF))
                        95 -> currentStyle = currentStyle.copy(color = Color(0xFFE040FB))
                        96 -> currentStyle = currentStyle.copy(color = Color(0xFF18FFFF))
                        97 -> currentStyle = currentStyle.copy(color = Color.White)
                    }
                }
                
                currentIndex = match.range.last + 1
            }
            
            // Add remaining text
            if (currentIndex < text.length) {
                withStyle(currentStyle) {
                    append(text.substring(currentIndex))
                }
            }
        }
    }
}
