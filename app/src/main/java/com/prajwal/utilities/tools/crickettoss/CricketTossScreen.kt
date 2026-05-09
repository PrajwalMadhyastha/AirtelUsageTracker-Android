package com.prajwal.utilities.tools.crickettoss

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CricketTossScreen(onNavigateBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    
    var isFlipping by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<String>() }
    
    val rotation = remember { Animatable(0f) }
    val verticalOffset = remember { Animatable(0f) }

    fun flipCoin() {
        if (isFlipping) return
        isFlipping = true
        resultText = null
        
        val isHeads = Random.nextBoolean()
        val currentAngle = rotation.value % 360
        val extraSpins = 5 * 360f
        val targetAngle = if (isHeads) 0f else 180f
        val diff = targetAngle - currentAngle
        val finalRotation = rotation.value + extraSpins + diff
        
        scope.launch {
            launch {
                rotation.animateTo(
                    targetValue = finalRotation,
                    animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
                )
            }
            launch {
                verticalOffset.animateTo(
                    targetValue = -400f,
                    animationSpec = tween(durationMillis = 750, easing = FastOutLinearInEasing)
                )
                verticalOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 750, easing = LinearOutSlowInEasing)
                )
            }.invokeOnCompletion {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isFlipping = false
                val outcome = if (isHeads) "H" else "T"
                resultText = if (isHeads) "HEADS" else "TAILS"
                if (history.size >= 10) history.removeLast()
                history.add(0, outcome)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cricket Toss") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Coin
                Box(
                    modifier = Modifier
                        .offset(y = verticalOffset.value.dp)
                        .graphicsLayer {
                            rotationY = rotation.value
                            cameraDistance = 8 * density
                        }
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val normalizedRotation = (rotation.value % 360).let { if (it < 0) it + 360 else it }
                    val isBackVisible = normalizedRotation in 90f..270f
                    
                    if (isBackVisible) {
                        // Tails side
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "T",
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    } else {
                        // Heads side
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "H",
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(64.dp))
                
                // Result text placeholder to avoid layout shift
                Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                    if (resultText != null) {
                        Text(
                            text = "It's $resultText!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { flipCoin() },
                    enabled = !isFlipping,
                    modifier = Modifier
                        .size(width = 200.dp, height = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isFlipping) "FLIPPING..." else "TOSS COIN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (history.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Recent Tosses",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        history.forEach { outcome ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (outcome == "H") MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = outcome,
                                    fontWeight = FontWeight.Bold,
                                    color = if (outcome == "H") MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
