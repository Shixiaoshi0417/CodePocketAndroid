package io.github.shixiaoshi0417.codepocketandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.shixiaoshi0417.codepocketandroid.model.ConnectionState
import io.github.shixiaoshi0417.codepocketandroid.ui.component.ConversationDrawerContent
import io.github.shixiaoshi0417.codepocketandroid.ui.component.MessageBubble
import io.github.shixiaoshi0417.codepocketandroid.ui.theme.CodePocketAndroidTheme
import io.github.shixiaoshi0417.codepocketandroid.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodePocketAndroidTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val viewModel: MainViewModel = viewModel()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val conversations by viewModel.conversations.collectAsState()
            val currentConvId by viewModel.currentConversationId.collectAsState()
            ConversationDrawerContent(
                conversations = conversations,
                currentConversationId = currentConvId,
                onConversationClick = { viewModel.switchConversation(it) },
                onNewConversation = { viewModel.newConversation() },
                onDeleteConversation = { viewModel.deleteConversation(it) },
                drawerState = drawerState,
                scope = scope
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val currentConvId by viewModel.currentConversationId.collectAsState()
                        val conversations by viewModel.conversations.collectAsState()
                        val title = conversations.find { it.id == currentConvId }?.title ?: "CodePocketAndroid"
                        Text(text = title)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            ChatScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    var inputText by rememberSaveable { mutableStateOf("") }

    val isConnected = connectionState == ConnectionState.CONNECTED

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                enabled = isConnected,
                modifier = Modifier.weight(1f),
                maxLines = 3,
                placeholder = {
                    Text(text = if (isConnected) "Type a message..." else "Disconnected")
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val text = inputText.trim()
                    if (text.isNotEmpty()) {
                        viewModel.sendChat(text)
                        inputText = ""
                    }
                },
                enabled = isConnected && inputText.isNotBlank()
            ) {
                Text(text = "Send")
            }
        }
    }
}
