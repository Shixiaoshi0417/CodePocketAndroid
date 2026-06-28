package io.github.shixiaoshi0417.codepocketandroid.ui.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage
import io.github.shixiaoshi0417.codepocketandroid.model.MessageRole
import io.github.shixiaoshi0417.codepocketandroid.ui.markdown.MarkdownText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val arrangement = if (isUser) {
        Arrangement.End
    } else {
        Arrangement.Start
    }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeText = timeFormat.format(Date(message.timestamp))

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = arrangement
    ) {
        Column(
            horizontalAlignment = if (isUser) {
                androidx.compose.ui.Alignment.End
            } else {
                androidx.compose.ui.Alignment.Start
            },
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = if (isUser) "You" else "CodePocket AI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (message.isStreaming && message.content.isEmpty()) {
                    Text(
                        text = "Typing...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cursorAlpha),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else if (isUser) {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    MarkdownText(
                        content = if (message.isStreaming) {
                            message.content + " ▌"
                        } else {
                            message.content
                        }
                    )
                }
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}
