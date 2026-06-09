package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RoomEntity
import com.example.data.TimeBlockEntity
import com.example.viewmodel.SpaceTimeViewModel
import kotlinx.coroutines.delay

@Composable
fun RoomScreen(
    roomId: Int,
    viewModel: SpaceTimeViewModel,
    onBack: () -> Unit
) {
    val room by viewModel.getRoom(roomId).collectAsStateWithLifecycle(initialValue = null)
    val timeBlocks by viewModel.getTimeBlocks(roomId).collectAsStateWithLifecycle(initialValue = emptyList())

    if (room == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var activeBlock by remember { mutableStateOf<TimeBlockEntity?>(null) }
    var timeRemainingSecs by remember { mutableStateOf(0) }
    var maxTimeSecs by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var newBlockTitle by remember { mutableStateOf("") }
    var newBlockDuration by remember { mutableStateOf("") }
    var showEditTimerDialog by remember { mutableStateOf(false) }
    var customEditTimerMins by remember { mutableStateOf("") }

    val context = LocalContext.current
    val notificationHelper = remember { com.example.util.NotificationHelper(context) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle response if needed
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(isTimerRunning, timeRemainingSecs) {
        if (isTimerRunning && timeRemainingSecs > 0) {
            delay(1000L)
            timeRemainingSecs -= 1
        } else if (isTimerRunning && timeRemainingSecs == 0) {
            isTimerRunning = false
            // Mark block complete if needed
            if (activeBlock != null) {
                viewModel.completeBlock(activeBlock!!) 
                notificationHelper.showTimerCompleteNotification(
                    title = "Time's Up!", 
                    message = "${activeBlock!!.title} is complete."
                )
            } else if (maxTimeSecs > 0) {
                val manualTitle = if (maxTimeSecs == 25 * 60) "Focus Session" else if (maxTimeSecs == 5 * 60) "Short Break" else if (maxTimeSecs == 15 * 60) "Long Break" else "Timer"
                notificationHelper.showTimerCompleteNotification(
                    title = "Time's Up!", 
                    message = "$manualTitle is complete."
                )
            }
        }
    }

    val r = room!!
    val roomColor = Color(r.colorArgb.toInt())

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showAddBlockDialog = true },
                    containerColor = roomColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(r.name, style = MaterialTheme.typography.titleLarge, color = roomColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = if (maxTimeSecs == 0) 0f
                           else timeRemainingSecs.toFloat() / maxTimeSecs.toFloat()

                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(0.75f),
                            color = roomColor,
                            trackColor = roomColor.copy(alpha = 0.1f),
                            strokeWidth = 10.dp,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val displaySecs = timeRemainingSecs % 60
                            val displayMins = timeRemainingSecs / 60
                            val timeStr = String.format("%02d:%02d", displayMins, displaySecs)

                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 55.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.clickable { 
                                    customEditTimerMins = displayMins.toString()
                                    showEditTimerDialog = true 
                                }
                            )
                            Text(
                                text = activeBlock?.title ?: if (maxTimeSecs == 25 * 60) "Focus Session" else if (maxTimeSecs == 5 * 60) "Short Break" else if (maxTimeSecs == 15 * 60) "Long Break" else "Select a Block",
                                style = MaterialTheme.typography.titleMedium,
                                color = roomColor
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            IconButton(
                                onClick = {
                                    if (activeBlock != null || timeRemainingSecs > 0) {
                                        isTimerRunning = !isTimerRunning
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(roomColor.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    if (isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = roomColor,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
                    
                Row(
                    horizontalArrangement = Arrangement.Center, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    TextButton(onClick = {
                        activeBlock = null
                        val duration = 25 * 60
                        timeRemainingSecs = duration
                        maxTimeSecs = duration
                        isTimerRunning = false
                    }) { Text("Focus", color = roomColor) }
                    TextButton(onClick = {
                        activeBlock = null
                        val duration = 5 * 60
                        timeRemainingSecs = duration
                        maxTimeSecs = duration
                        isTimerRunning = false
                    }) { Text("Short Break", color = roomColor) }
                    TextButton(onClick = {
                        activeBlock = null
                        val duration = 15 * 60
                        timeRemainingSecs = duration
                        maxTimeSecs = duration
                        isTimerRunning = false
                    }) { Text("Long Break", color = roomColor) }
                }
            }

        var selectedTabIndex by remember { mutableStateOf(0) }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedTabIndex == 0) "TIME BLOCKS" else "ANALYTICS", 
                style = MaterialTheme.typography.labelLarge, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTabIndex == 0) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .clickable { selectedTabIndex = 0 }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Tasks", style = MaterialTheme.typography.labelMedium, color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedTabIndex == 1) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .clickable { selectedTabIndex = 1 }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Trends", style = MaterialTheme.typography.labelMedium, color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTabIndex == 0) {
            // Time Blocks Timeline
            LazyColumn(
                modifier = Modifier.weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(timeBlocks) { block ->
                    val isCompleted = block.isCompleted
                    val isCurrent = activeBlock?.blockId == block.blockId
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            .clickable {
                                activeBlock = block
                                val duration = block.durationMin * 60
                                timeRemainingSecs = duration
                                maxTimeSecs = duration
                                isTimerRunning = false
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.completeBlock(block) }
                                .background(
                                    if (isCompleted) roomColor else Color.Transparent, 
                                    CircleShape
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Filled.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(block.title, style = MaterialTheme.typography.bodyLarge, color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                            Text("${block.durationMin} mins", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.deleteBlock(block) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            // Analytics View
            Box(modifier = Modifier.weight(0.45f)) {
                com.example.ui.components.ProductivityChart(
                    roomColor = roomColor,
                    totalSessions = r.totalSessionsCompleted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    }
    }

    if (showAddBlockDialog) {
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Add Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newBlockTitle,
                        onValueChange = { newBlockTitle = it },
                        label = { Text("Task Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newBlockDuration,
                        onValueChange = { newBlockDuration = it },
                        label = { Text("Duration (mins)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val duration = newBlockDuration.toIntOrNull() ?: 25
                        if (newBlockTitle.isNotBlank()) {
                            viewModel.addBlock(roomId, newBlockTitle.trim(), duration)
                        }
                        showAddBlockDialog = false
                        newBlockTitle = ""
                        newBlockDuration = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditTimerDialog) {
        AlertDialog(
            onDismissRequest = { showEditTimerDialog = false },
            title = { Text("Edit Timer") },
            text = {
                OutlinedTextField(
                    value = customEditTimerMins,
                    onValueChange = { customEditTimerMins = it },
                    label = { Text("Duration (mins)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newDuration = customEditTimerMins.toIntOrNull()
                    if (newDuration != null && newDuration > 0) {
                        timeRemainingSecs = newDuration * 60
                        maxTimeSecs = timeRemainingSecs
                        isTimerRunning = false
                        activeBlock = null
                    }
                    showEditTimerDialog = false
                }) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTimerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}