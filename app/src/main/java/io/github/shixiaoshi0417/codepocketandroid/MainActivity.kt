package io.github.shixiaoshi0417.codepocketandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.shixiaoshi0417.codepocketandroid.model.ConnectionState
import io.github.shixiaoshi0417.codepocketandroid.ui.agent.AgentScreen
import io.github.shixiaoshi0417.codepocketandroid.ui.component.ConversationDrawerContent
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
            val connectionState by viewModel.connectionState.collectAsState()
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                AgentScreen(
                    viewModel = viewModel.agentViewModel,
                    connectionState = connectionState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
