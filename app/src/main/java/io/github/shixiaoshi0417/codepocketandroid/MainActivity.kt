package io.github.shixiaoshi0417.codepocketandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.shixiaoshi0417.codepocketandroid.model.ConnectionState
import io.github.shixiaoshi0417.codepocketandroid.model.OpenCodeSession
import io.github.shixiaoshi0417.codepocketandroid.ui.agent.AgentScreen
import io.github.shixiaoshi0417.codepocketandroid.ui.theme.CodePocketAndroidTheme
import io.github.shixiaoshi0417.codepocketandroid.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CodePocketAndroidTheme { MainApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val viewModel: MainViewModel = viewModel()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.connect() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val sessions by viewModel.sessions.collectAsState()
            val selectedId by viewModel.selectedSessionId.collectAsState()
            ModalDrawerSheet {
                LazyColumn {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Sessions", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.newSession(); scope.launch { drawerState.close() } }) {
                                Icon(Icons.Default.Add, contentDescription = "New")
                            }
                        }
                        HorizontalDivider()
                    }
                    items(sessions, key = { it.id }) { session ->
                        val isSelected = session.id == selectedId
                        val df = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { viewModel.openSession(session.id); scope.launch { drawerState.close() } }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isSelected) "\u25B6" else "\u2003", modifier = Modifier.padding(end = 8.dp),
                                color = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.title, style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(session.directory, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text(df.format(Date(session.timeUpdated)), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteSession(session.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val sessions by viewModel.sessions.collectAsState()
                        val selectedId by viewModel.selectedSessionId.collectAsState()
                        Text(sessions.find { it.id == selectedId }?.title ?: "CodePocketAndroid")
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        val currentModel by viewModel.currentModel.collectAsState()
                        Text(currentModel, style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp))
                    }
                )
            }
        ) { innerPadding ->
            val connectionState by viewModel.connectionState.collectAsState()
            val currentModel by viewModel.currentModel.collectAsState()
            val selectedId by viewModel.selectedSessionId.collectAsState()
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                AgentScreen(
                    viewModel = viewModel.agentViewModel,
                    connectionState = connectionState,
                    model = currentModel,
                    sessionId = selectedId,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
