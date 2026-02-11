package com.airtel.usagetracker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airtel.usagetracker.data.models.RouterConfig
import com.airtel.usagetracker.data.models.ScrapingStatus
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: UsageViewModel
) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 3
    
    Scaffold(
        bottomBar = {
            if (currentStep < totalSteps) { // Don't show bottom bar on final success screen if we had one, but we just navigate
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        TextButton(onClick = { currentStep-- }) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    // Dots indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(totalSteps) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (index == currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                        }
                    }

                    // Next button is handled by specific screens or here if generic
                    // We'll let specific screens handle the "Next" action to validate input
                    Spacer(modifier = Modifier.width(64.dp)) // Placeholder for balance
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentStep) {
                0 -> WelcomeStep(
                    onNext = { currentStep++ }
                )
                1 -> CredentialsStep(
                    viewModel = viewModel,
                    onNext = { currentStep++ }
                )
                2 -> SyncSettingsStep(
                    viewModel = viewModel,
                    onGetStarted = onOnboardingComplete
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SignalWifi4Bar,
            contentDescription = "WiFi",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to\nAirtel Usage Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Track your router usage, detect reboots, and stay on top of your data plan directly from your phone.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Let's Get Started")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun CredentialsStep(
    viewModel: UsageViewModel,
    onNext: () -> Unit
) {
    val config by viewModel.routerConfig.collectAsState()
    var ipAddress by remember { mutableStateOf(config.routerIp) }
    var username by remember { mutableStateOf(config.username) }
    var password by remember { mutableStateOf(config.password) }
    var fupLimit by remember { mutableStateOf(config.fupLimitGb.toString()) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val scrapingStatus by viewModel.scrapingStatus.collectAsState()
    val isTesting = scrapingStatus != ScrapingStatus.IDLE && scrapingStatus != ScrapingStatus.SUCCESS && scrapingStatus != ScrapingStatus.ERROR
    var testResult by remember { mutableStateOf<Boolean?>(null) }
    
    // Reset test result when inputs change
    LaunchedEffect(ipAddress, username, password) {
        testResult = null
    }

    // Capture success state to allow next
    LaunchedEffect(scrapingStatus) {
        if (scrapingStatus == ScrapingStatus.SUCCESS) {
            testResult = true
        } else if (scrapingStatus == ScrapingStatus.ERROR) {
            testResult = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Router Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter your router's login details. We need this to fetch the usage data.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("Router IP Address") },
            placeholder = { Text("e.g. 192.168.1.1") },
            leadingIcon = { Icon(Icons.Default.Router, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = fupLimit,
            onValueChange = { if (it.all { char -> char.isDigit() }) fupLimit = it },
            label = { Text("Monthly Limit (GB)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test Connection Button
        Button(
            onClick = { 
                viewModel.updateRouterConfig(
                    config.copy(
                        routerIp = ipAddress,
                        username = username,
                        password = password,
                        fupLimitGb = fupLimit.toIntOrNull() ?: 3333
                    )
                )
                viewModel.refreshUsageData()
            },
            enabled = !isTesting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            if (isTesting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing Connection...")
            } else {
                Text(if (testResult == null) "Test Connection" else "Test Again")
            }
        }
        
        // Test Result feedback
        AnimatedContent(targetState = testResult, label = "TestResult") { result ->
            if (result != null) {
                val color = if (result) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                val containerColor = if (result) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                val icon = if (result) Icons.Default.CheckCircle else Icons.Default.Warning
                val message = if (result) "Connection Successful!" else "Connection Failed. Check credentials or WiFi."
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = color)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message, color = color, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                 // Save config before moving on
                 viewModel.updateRouterConfig(
                    config.copy(
                        routerIp = ipAddress,
                        username = username,
                        password = password,
                        fupLimitGb = fupLimit.toIntOrNull() ?: 3333
                    )
                )
                onNext()
            },
            enabled = !isTesting, // Allow proceeding even if test failed (user might be offline), but warn? For now just allow.
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
fun SyncSettingsStep(
    viewModel: UsageViewModel,
    onGetStarted: () -> Unit
) {
    val intervalOptions = listOf(4, 6, 12, 1) // Hours. 15min handled separately if needed, but keeping simple for now.
    var selectedInterval by remember { mutableStateOf(4) }
    
    // Save the selected interval
    LaunchedEffect(selectedInterval) {
        viewModel.setSyncInterval(selectedInterval)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sync Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "How often should we check for usage updates?",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Interval Options
        intervalOptions.forEach { hours ->
            val isSelected = selectedInterval == hours
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                onClick = { selectedInterval = hours }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { selectedInterval = hours }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Every $hours hours",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (hours == 4 || hours == 6) {
                            Text(
                                text = "Recommended for battery life",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (hours == 1) {
                            Text(
                                text = "May impact battery slightly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "We'll only sync when you're connected to WiFi to save data and battery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finish & Go to Dashboard")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Check, contentDescription = null)
        }
    }
}
