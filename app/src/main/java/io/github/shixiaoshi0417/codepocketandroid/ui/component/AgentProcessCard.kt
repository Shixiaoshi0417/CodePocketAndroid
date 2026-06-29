package io.github.shixiaoshi0417.codepocketandroid.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.ui.markdown.MarkdownText

private const val CHUNK_SIZE = 1000

@Composable
fun AgentProcessCard(
    processMessages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    if (processMessages.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(false) }

    val totalChars = processMessages.sumOf { it.content.length }
    val stepCount = processMessages.size
    val summary = buildString {
        append("\uD83E\uDD16 Agent\u8FC7\u7A0B\uFF08${stepCount}\u6B65")
        if (totalChars > 1000) append("\uFF0C${totalChars / 1000}.${(totalChars % 1000) / 100}k\u5B57\u7B26")
        append("\uFF09")
    }

    val rawContent = processMessages.joinToString("\n") { it.content }
    val chunks = rawContent.chunked(CHUNK_SIZE)

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (expanded) "\u25BC" else "\u25B6", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
            Text(if (expanded) "Agent\u8FC7\u7A0B" else summary, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                chunks.forEach { chunk ->
                    MarkdownText(content = chunk)
                    if (chunks.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
