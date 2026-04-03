package com.example.todoit.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.todoit.BuildConfig
import com.example.todoit.presentation.common.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFmt = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Always forward to ViewModel regardless of resultCode.
        // The old GoogleSignInClient can return a non-RESULT_OK code even on success
        // when additional OAuth scopes (Sheets, Drive) are requested.
        // onSignInResult handles null data (user pressed Back) and ApiExceptions internally.
        viewModel.onSignInResult(result.data)
    }

    LaunchedEffect(uiState.syncError) {
        uiState.syncError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top,
        ) {
            // ── Google Account section ────────────────────────────────────
            Text(
                "Google Account & Sync",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (uiState.isSignedIn) {
                ListItem(
                    headlineContent  = { Text(uiState.accountEmail ?: "Signed in") },
                    supportingContent = {
                        val lastSync = if (uiState.lastSyncAt > 0L)
                            "Last sync: ${dateFmt.format(Date(uiState.lastSyncAt))}"
                        else "Never synced"
                        Text(lastSync)
                    },
                )
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Button(
                        onClick = { viewModel.syncNow() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSyncing,
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp))
                        } else {
                            Text("Sync Now")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sign Out")
                    }
                }
            } else {
                ListItem(
                    headlineContent   = { Text("Not signed in") },
                    supportingContent = { Text("Sign in to sync with Google Sheets") },
                )
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Button(
                        onClick  = { signInLauncher.launch(viewModel.getSignInIntent()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Sign in with Google") }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ── Schedules ──────────────────────────────────────────────────
            Text(
                "Schedules",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent   = { Text("Manage schedules") },
                supportingContent = { Text("Define active days & hours for groups or tasks") },
                trailingContent = {
                    TextButton(onClick = { navController.navigate(Screen.SCHEDULES) }) {
                        Text("Open")
                    }
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            // ── About ──────────────────────────────────────────────────────
            Text(
                "About",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent   = { Text("Version") },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            )
        }
    }
}
