package com.airtel.usagetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: UsageViewModel,
    onNavigateBack: () -> Unit
) {
    val syncInterval by viewModel.syncIntervalHours.collectAsState()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsState()
    val config by viewModel.routerConfig.collectAsState()
    
    var showCredentialsDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Sync Settings Section
            Text(
                text = "Sync & Background",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Auto Sync Toggle
            ListItem(
                headlineContent = { Text("Auto Sync") },
                supportingContent = { Text("Automatically check usage in background") },
                trailingContent = {
                    Switch(
                        checked = isAutoSyncEnabled,
                        onCheckedChange = { viewModel.setAutoSyncEnabled(it) }
                    )
                }
            )
            
            HorizontalDivider()
            
            // Sync Interval
            ListItem(
                headlineContent = { Text("Sync Frequency") },
                supportingContent = { 
                    Text("Every $syncInterval hours") 
                },
                trailingContent = {
                   // A dropdown or dialog would be better, but for simplicity let's cycle or show a dialog
                   // Let's use a simple dropdown menu here or just a click to open dialog
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            // Interval Selection Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val intervals = listOf(1, 4, 6, 12)
                intervals.forEach { hours ->
                    FilterChip(
                        selected = syncInterval == hours,
                        onClick = { viewModel.setSyncInterval(hours) },
                        label = { Text("${hours}h") },
                        enabled = isAutoSyncEnabled
                    )
                }
            }
            
            if (syncInterval < 4 && isAutoSyncEnabled) {
                ListItem(
                    headlineContent = { 
                        Text(
                            "High Battery Usage",
                            color = MaterialTheme.colorScheme.error
                        ) 
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    supportingContent = { Text("Frequent syncs may drain battery faster.") }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Router Settings Section
            Text(
                text = "Router Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text("Credentials & IP") },
                supportingContent = { Text("${config.username} @ ${config.routerIp}") },
                trailingContent = {
                    Button(onClick = { showCredentialsDialog = true }) {
                        Text("Edit")
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text("1.0.0") },
                leadingContent = { Icon(Icons.Default.Info, null) }
            )
        }
    }
    
    if (showCredentialsDialog) {
        CredentialsDialog(
            initialConfig = config,
            onDismiss = { showCredentialsDialog = false },
            onSave = { newConfig ->
                viewModel.updateRouterConfig(newConfig)
                showCredentialsDialog = false
            }
        )
    }
}

@Composable
fun CredentialsDialog(
    initialConfig: com.airtel.usagetracker.data.models.RouterConfig,
    onDismiss: () -> Unit,
    onSave: (com.airtel.usagetracker.data.models.RouterConfig) -> Unit
) {
    var ipAddress by remember { mutableStateOf(initialConfig.routerIp) }
    var username by remember { mutableStateOf(initialConfig.username) }
    var password by remember { mutableStateOf(initialConfig.password) }
    var fupLimit by remember { mutableStateOf(initialConfig.fupLimitGb.toString()) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Credentials") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("Router IP") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = fupLimit,
                    onValueChange = { if (it.all { char -> char.isDigit() }) fupLimit = it },
                    label = { Text("Limit (GB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initialConfig.copy(
                            routerIp = ipAddress,
                            username = username,
                            password = password,
                            fupLimitGb = fupLimit.toIntOrNull() ?: 3333
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
