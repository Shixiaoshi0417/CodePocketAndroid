package io.github.shixiaoshi0417.codepocketandroid.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.shixiaoshi0417.codepocketandroid.model.ChatMessage

@Composable
fun MessageBubble(message: ChatMessage) {
    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val arrangement = if (message.isUser) {
        Arrangement.End
    } else {
        Arrangement.Start
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = arrangement
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
