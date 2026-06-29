package io.github.shixiaoshi0417.codepocketandroid.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.ui.markdown.MarkdownText
import kotlinx.coroutines.delay

private const val INITIAL_CHARS = 1000
private const val CHUNK_SIZE = 5000
private const val CHUNK_DELAY_MS = 500L
private const val MARKDOWN_THRESHOLD = 10000

@Composable
fun AgentProcessCard(
    processMessages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    if (processMessages.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    var visibleChars by rememberSaveable { mutableIntStateOf(0) }

    val totalChars = processMessages.sumOf { it.content.length }
    val stepCount = processMessages.size
    val summary = when {
        totalChars > 1000 -> "\uD83E\uDD16 Agent\u8FC7\u7A0B\uFF08${stepCount}\u6B65\uFF0C${totalChars / 1000}.${(totalChars % 1000) / 100}k\u5B57\u7B26\uFF09"
        else -> "\uD83E\uDD16 Agent\u8FC7\u7A0B\uFF08${stepCount}\u6B65\uFF09"
    }

    val rawContent = processMessages.joinToString("\n") { it.content }
    val isLoading = expanded && visibleChars > 0 && visibleChars < totalChars.coerceAtMost(100_000)
    val isComplete = !expanded || visibleChars >= totalChars.coerceAtMost(100_000)
    val useMarkdown = visibleChars <= MARKDOWN_THRESHOLD || isComplete

    LaunchedEffect(expanded) {
        if (expanded && totalChars > INITIAL_CHARS) {
            visibleChars = INITIAL_CHARS
            var current = INITIAL_CHARS
            val max = totalChars.coerceAtMost(100_000)
            while (current < max) {
                delay(CHUNK_DELAY_MS)
                current = (current + CHUNK_SIZE).coerceAtMost(max)
                visibleChars = current
            }
        } else if (expanded) {
            visibleChars = totalChars
        }
    }

    val displayContent = if (expanded && visibleChars > 0) {
        rawContent.take(visibleChars).trimEnd()
    } else {
        ""
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (expanded) "\u25BC" else "\u25B6", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
            Text(
                if (expanded) "Agent\u8FC7\u7A0B" else summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
                    Text(
                        "\u6B63\u5728\u52A0\u8F7D\u601D\u8003\u94FE\u2026 ${visibleChars}/${totalChars.coerceAtMost(100_000)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (displayContent.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        if (useMarkdown) {
                            MarkdownText(content = displayContent)
                        } else {
                            Text(
                                text = displayContent,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
