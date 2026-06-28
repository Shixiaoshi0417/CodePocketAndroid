package io.github.shixiaoshi0417.codepocketandroid.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.shixiaoshi0417.codepocketandroid.ui.component.CodeBlock

@Composable
fun MarkdownText(content: String, modifier: Modifier = Modifier) {
    val inlineCodeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val linkColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        val blocks = parseMarkdownBlocks(content)
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Header -> {
                    Text(
                        text = block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlock(code = block.code, language = block.language)
                }
                is MarkdownBlock.BlockQuote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        MarkdownText(
                            content = block.text,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    val annotated = parseInlineMarkdown(block.text, inlineCodeBackground, linkColor)
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }
                is MarkdownBlock.ListItem -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = block.bullet,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp)
                        )
                        val annotated = parseInlineMarkdown(block.text, inlineCodeBackground, linkColor)
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Header(val text: String, val level: Int) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class ListItem(val bullet: String, val text: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        when {
            line.startsWith("```") -> {
                val language = line.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), language))
                i++
            }
            line.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Header(line.removePrefix("# ").trim(), 1))
                i++
            }
            line.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Header(line.removePrefix("## ").trim(), 2))
                i++
            }
            line.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Header(line.removePrefix("### ").trim(), 3))
                i++
            }
            line.startsWith("> ") -> {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].startsWith("> ")) {
                    quoteLines.add(lines[i].removePrefix("> "))
                    i++
                }
                blocks.add(MarkdownBlock.BlockQuote(quoteLines.joinToString("\n")))
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                blocks.add(MarkdownBlock.ListItem(line.substring(0, 1), line.substring(2).trim()))
                i++
            }
            line.matches(Regex("^\\d+\\. .*")) -> {
                val num = line.substringBefore(".")
                blocks.add(MarkdownBlock.ListItem("$num.", line.substringAfter(". ").trim()))
                i++
            }
            line.isBlank() -> {
                i++
            }
            else -> {
                val paragraphLines = mutableListOf<String>()
                while (i < lines.size && lines[i].isNotBlank() &&
                    !lines[i].startsWith("#") && !lines[i].startsWith("```") &&
                    !lines[i].startsWith(">") && !lines[i].startsWith("- ") &&
                    !lines[i].startsWith("* ") && !lines[i].matches(Regex("^\\d+\\. .*"))
                ) {
                    paragraphLines.add(lines[i])
                    i++
                }
                if (paragraphLines.isNotEmpty()) {
                    blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString("\n")))
                }
            }
        }
    }
    return blocks
}

fun parseInlineMarkdown(text: String, codeBackground: Color, linkColor: Color): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex(
            """(\*\*\*(.+?)\*\*\*)|(\*\*(.+?)\*\*)|(\*(.+?)\*)|(`(.+?)`)|(~~(.+?)~~)|(\[(.+?)]\((.+?)\))"""
        )
        var lastIndex = 0
        val matches = regex.findAll(text)
        for (match in matches) {
            val start = match.range.first
            append(text.substring(lastIndex, start))
            when {
                match.groupValues[1].isNotEmpty() -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(match.groupValues[2])
                    }
                }
                match.groupValues[3].isNotEmpty() -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[4])
                    }
                }
                match.groupValues[5].isNotEmpty() -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groupValues[6])
                    }
                }
                match.groupValues[7].isNotEmpty() -> {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground
                    )) {
                        append(match.groupValues[8])
                    }
                }
                match.groupValues[9].isNotEmpty() -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(match.groupValues[10])
                    }
                }
                match.groupValues[11].isNotEmpty() -> {
                    withStyle(SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(match.groupValues[12])
                    }
                }
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
