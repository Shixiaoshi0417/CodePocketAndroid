package io.github.shixiaoshi0417.codepocketandroid.ui.agent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.shixiaoshi0417.codepocketandroid.model.ConnectionState
import io.github.shixiaoshi0417.codepocketandroid.ui.component.MessageBubble
import io.github.shixiaoshi0417.codepocketandroid.viewmodel.AgentViewModel

@Composable
fun AgentScreen(
    viewModel: AgentViewModel,
    connectionState: ConnectionState,
    model: String = "",
    sessionId: String = "",
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }

    val isConnected = connectionState == ConnectionState.CONNECTED
    val isProcessing = viewModel.isProcessing

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        val statusText = when (connectionState) {
            ConnectionState.DISCONNECTED -> "\uD83D\uDD34 Disconnected"
            ConnectionState.CONNECTING -> "\uD83D\uDFE1 Connecting"
            ConnectionState.CONNECTED -> "\uD83D\uDFE2 Connected"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (messages.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ask OpenCode to work on your project",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                enabled = isConnected && !isProcessing,
                modifier = Modifier.weight(1f),
                maxLines = 3,
                placeholder = {
                    Text(text = when {
                        !isConnected -> "Disconnected"
                        isProcessing -> "Agent is working..."
                        else -> "Ask OpenCode..."
                    })
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val text = inputText.trim()
                    if (text.isNotEmpty()) {
                        viewModel.sendPrompt(text, model, sessionId)
                        inputText = ""
                    }
                },
                enabled = isConnected && !isProcessing && inputText.isNotBlank()
            ) { Text(text = "Send") }
        }
    }
}
