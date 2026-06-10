package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.RoomEntity
import com.example.data.TimeBlockEntity
import com.example.viewmodel.SpaceTimeViewModel
import kotlinx.coroutines.delay

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalFoundationApi::class)
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

    val activeBlockId by viewModel.activeBlockId.collectAsStateWithLifecycle()
    val activeBlock = remember(activeBlockId, timeBlocks) { 
        if (activeBlockId != -1) timeBlocks.find { it.blockId == activeBlockId } else null 
    }
    val timeRemainingSecs by viewModel.timeRemainingSecs.collectAsStateWithLifecycle()
    val overtimeSecs by viewModel.overtimeSecs.collectAsStateWithLifecycle()
    val isOvertime by viewModel.isOvertime.collectAsStateWithLifecycle()
    val maxTimeSecs by viewModel.maxTimeSecs.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var newBlockTitle by remember { mutableStateOf("") }
    var newBlockDuration by remember { mutableStateOf("") }
    var newBlockColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var showCustomBlockColorPicker by remember { mutableStateOf(false) }
    var showEditTimerDialog by remember { mutableStateOf(false) }
    var customEditTimerMins by remember { mutableStateOf("") }
    
    // For editing time block
    var blockToEdit by remember { mutableStateOf<TimeBlockEntity?>(null) }
    var editBlockTitle by remember { mutableStateOf("") }
    var editBlockDuration by remember { mutableStateOf("") }
    var editBlockColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var showEditCustomBlockColorPicker by remember { mutableStateOf(false) }
    var isReorderMode by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
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

    LaunchedEffect(Unit) {
        viewModel.onTimerCompleteNotification = { titleMsg, msg ->
            notificationHelper.showTimerCompleteNotification(titleMsg, msg)
        }
    }

    val r = room!!
    val isBreakActive = (activeBlock == null && (maxTimeSecs == 5 * 60 || maxTimeSecs == 15 * 60)) || 
            (activeBlock != null && activeBlock!!.title.lowercase().contains("break"))
    
    val roomColor = if (activeBlock != null && activeBlock.colorArgb != 0xFFFFFFFFL && activeBlock.colorArgb != -1L) {
        Color(activeBlock.colorArgb.toInt())
    } else if (isBreakActive) {
        Color(0xFF2E7D32) // Relaxing natural green
    } else {
        Color(r.colorArgb.toInt())
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val backgroundColor = if (isBreakActive) {
        if (isDark) Color(0xFF0B1B0F) else Color(0xFFF1F8F3) // immersive soft green backgrounds
    } else {
        MaterialTheme.colorScheme.background
    }

    val surfaceColor = if (isBreakActive) {
        if (isDark) Color(0xFF142C19) else Color(0xFFE8F5E9) // relaxing green container
    } else {
        MaterialTheme.colorScheme.surface
    }

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
        val bgGradient = if (isDark) {
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    backgroundColor,
                    roomColor.copy(alpha = 0.08f),
                    backgroundColor
                )
            )
        } else {
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    backgroundColor,
                    roomColor.copy(alpha = 0.05f),
                    backgroundColor
                )
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
            // Ambient glowing gradient orbs in the background for real glassy reflection
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = roomColor.copy(alpha = if (isDark) 0.12f else 0.08f),
                    radius = size.width * 0.8f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.2f)
                )
                drawCircle(
                    color = roomColor.copy(alpha = if (isDark) 0.08f else 0.06f),
                    radius = size.width * 0.6f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.7f)
                )
            }

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
                    Column {
                        Text(r.name, style = MaterialTheme.typography.titleLarge, color = roomColor)
                        val tbMins = r.timeBank / 60
                        val tbSign = if (r.timeBank > 0) "+" else if (r.timeBank < 0) "-" else ""
                        val tbColor = if (r.timeBank > 0) Color(0xFF2E7D32) else if (r.timeBank < 0) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "TimeBank: $tbSign${kotlin.math.abs(tbMins)} min",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = tbColor
                            )
                            val totalAssignedMins = timeBlocks.sumOf { it.durationMin }
                            Text(
                                text = "Total: $totalAssignedMins min",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                IconButton(onClick = { showShareDialog = true }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share Room", tint = roomColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Section (Solid minimalist and thin stroke of Apple design)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .background(
                        surfaceColor,
                        RoundedCornerShape(32.dp)
                    )
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                        RoundedCornerShape(32.dp)
                    ),
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
                            val displayTime = if (isOvertime) overtimeSecs else timeRemainingSecs
                            val displaySecs = displayTime % 60
                            val displayMins = displayTime / 60
                            val timeStr = String.format("%02d:%02d", displayMins, displaySecs)
                            val textColor = if (isOvertime) Color.Red else MaterialTheme.colorScheme.onSurface
                            val titleColor = if (isOvertime) Color.Red else roomColor

                            Text(
                                text = if (isOvertime) "+$timeStr" else timeStr,
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 55.sp),
                                color = textColor,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.clickable { 
                                    if (!isOvertime) {
                                        customEditTimerMins = displayMins.toString()
                                        showEditTimerDialog = true 
                                    }
                                }
                            )
                            if (isOvertime) {
                                Text(
                                    text = "OVERTIME",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                                    color = Color.Red
                                )
                            }
                            Text(
                                text = activeBlock?.title ?: if (maxTimeSecs == 25 * 60) "Focus Session" else if (maxTimeSecs == 5 * 60) "Short Break" else if (maxTimeSecs == 15 * 60) "Long Break" else "Select a Block",
                                style = MaterialTheme.typography.titleMedium,
                                color = titleColor
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconButton(
                                    onClick = {
                                        if (activeBlock != null || timeRemainingSecs > 0 || isOvertime) {
                                            viewModel.toggleTimer()
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
                                
                                IconButton(
                                    onClick = {
                                        if (activeBlock != null) {
                                            val assignedSecs = activeBlock.durationMin * 60
                                            val tLeft = if (!isOvertime) timeRemainingSecs else 0
                                            val tTaken = assignedSecs - tLeft
                                            val oTime = if (isOvertime) overtimeSecs else 0
                                            viewModel.recordTaskCompletion(r, activeBlock, tTaken, oTime)
                                        } else if (maxTimeSecs > 0) {
                                            val tLeft = if (!isOvertime) timeRemainingSecs else 0
                                            val tTaken = maxTimeSecs - tLeft
                                            val oTime = if (isOvertime) overtimeSecs else 0
                                            viewModel.recordManualSessionCompletion(r, maxTimeSecs, tTaken, oTime)
                                        }
                                        viewModel.resetTimerState()
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color.Red.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Filled.Stop,
                                        contentDescription = "Stop Formally",
                                        tint = Color.Red,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
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
                        viewModel.activeBlockId.value = -1
                        val duration = 25 * 60
                        viewModel.timeRemainingSecs.value = duration
                        viewModel.maxTimeSecs.value = duration
                        viewModel.isOvertime.value = false
                        viewModel.overtimeSecs.value = 0
                        viewModel.stopTimer()
                    }) { Text("Focus", color = roomColor) }
                    TextButton(onClick = {
                        viewModel.activeBlockId.value = -1
                        val duration = 5 * 60
                        viewModel.timeRemainingSecs.value = duration
                        viewModel.maxTimeSecs.value = duration
                        viewModel.isOvertime.value = false
                        viewModel.overtimeSecs.value = 0
                        viewModel.stopTimer()
                    }) { Text("Short Break", color = roomColor) }
                    TextButton(onClick = {
                        viewModel.activeBlockId.value = -1
                        val duration = 15 * 60
                        viewModel.timeRemainingSecs.value = duration
                        viewModel.maxTimeSecs.value = duration
                        viewModel.isOvertime.value = false
                        viewModel.overtimeSecs.value = 0
                        viewModel.stopTimer()
                    }) { Text("Long Break", color = roomColor) }
                }
            }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedTabIndex == 0) "TIME BLOCKS" else "ANALYTICS", 
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            
            // iOS 26 Segmented Control
            Row(
                modifier = Modifier
                    .background(
                        if (isDark) Color(0xFF222222) else Color(0xFFEEEEEE), 
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        0.5.dp,
                        if (isDark) Color(0xFF333333) else Color(0xFFDDDDDD),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selectedTabIndex == 0) {
                                if (isDark) Color(0xFF3C3C3C) else Color.White
                            } else Color.Transparent
                        )
                        .clickable { selectedTabIndex = 0 }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Tasks", 
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), 
                        color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selectedTabIndex == 1) {
                                if (isDark) Color(0xFF3C3C3C) else Color.White
                            } else Color.Transparent
                        )
                        .clickable { selectedTabIndex = 1 }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Trends", 
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), 
                        color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTabIndex == 0) {
            if (isReorderMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = roomColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Reorder Tasks Active", 
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Tap arrows to shift tasks. Double tap any row to save & exit.", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { isReorderMode = false },
                            colors = ButtonDefaults.buttonColors(containerColor = roomColor)
                        ) {
                            Text("Done", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            }

            // Minimalist iOS 26 Glassy Timeline
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            LazyColumn(
                modifier = Modifier.weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(timeBlocks) { block ->
                    val isCompleted = block.isCompleted
                    val isCurrent = activeBlock?.blockId == block.blockId
                    
                    val checkScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isCompleted) 1.15f else 1.0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "check_scale"
                    )

                    val checkmarkScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isCompleted) 1.0f else 0.0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioHighBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        ),
                        label = "checkmark_scale"
                    )
                    
                    val cardBg = if (isCurrent) {
                        roomColor.copy(alpha = if (isDark) 0.22f else 0.15f)
                    } else {
                        if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                    }
                    val cardBorder = if (isCurrent) {
                        roomColor.copy(alpha = 0.5f)
                    } else {
                        if (isDark) Color(0xFF2E2E2E) else Color(0xFFE5E5E5)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBg, RoundedCornerShape(20.dp))
                            .border(0.5.dp, cardBorder, RoundedCornerShape(20.dp))
                            .combinedClickable(
                                onClick = {
                                    if (!isReorderMode) {
                                        viewModel.activeBlockId.value = block.blockId
                                        val duration = block.durationMin * 60
                                        viewModel.timeRemainingSecs.value = duration
                                        viewModel.maxTimeSecs.value = duration
                                        viewModel.isOvertime.value = false
                                        viewModel.overtimeSecs.value = 0
                                        viewModel.stopTimer()
                                    }
                                },
                                onLongClick = {
                                    if (!isReorderMode) {
                                        blockToEdit = block
                                        editBlockTitle = block.title
                                        editBlockDuration = block.durationMin.toString()
                                        editBlockColor = Color(block.colorArgb.toInt())
                                    }
                                },
                                onDoubleClick = {
                                    isReorderMode = !isReorderMode
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer(
                                    scaleX = checkScale,
                                    scaleY = checkScale
                                )
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !isReorderMode) { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val isActive = activeBlockId == block.blockId
                                    val assignedSecs = block.durationMin * 60
                                    val tLeft = if (isActive && !isOvertime) timeRemainingSecs else if (isActive && isOvertime) 0 else assignedSecs
                                    val tTaken = assignedSecs - tLeft
                                    val oTime = if (isActive && isOvertime) overtimeSecs else 0
                                    
                                    viewModel.recordTaskCompletion(r, block, tTaken, oTime)
                                    
                                    if (isActive) {
                                        viewModel.resetTimerState()
                                    }
                                }
                                .background(
                                    if (isCompleted) roomColor else Color.Transparent, 
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.5.dp,
                                    if (isCompleted) Color.Transparent else roomColor.copy(alpha = 0.5f),
                                    RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checkmarkScale > 0.01f) {
                                Icon(
                                    imageVector = Icons.Filled.Check, 
                                    contentDescription = "Done", 
                                    tint = Color.White, 
                                    modifier = Modifier
                                        .size(14.dp)
                                        .graphicsLayer(
                                            scaleX = checkmarkScale,
                                            scaleY = checkmarkScale
                                        )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = block.title, 
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                ), 
                                color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${block.durationMin} mins", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        
                        if (isReorderMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val idx = timeBlocks.indexOf(block)
                                        if (idx > 0) {
                                            val mutable = timeBlocks.toMutableList()
                                            val temp = mutable[idx]
                                            mutable[idx] = mutable[idx - 1]
                                            mutable[idx - 1] = temp
                                            viewModel.updateTimeBlockOrder(mutable)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = roomColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Reorder Drag Handle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )

                                IconButton(
                                    onClick = {
                                        val idx = timeBlocks.indexOf(block)
                                        if (idx != -1 && idx < timeBlocks.size - 1) {
                                            val mutable = timeBlocks.toMutableList()
                                            val temp = mutable[idx]
                                            mutable[idx] = mutable[idx + 1]
                                            mutable[idx + 1] = temp
                                            viewModel.updateTimeBlockOrder(mutable)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = roomColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            IconButton(
                                onClick = { viewModel.deleteBlock(block) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete, 
                                    contentDescription = "Delete", 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Analytics View (with solid backdrop instead of glass!)
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .background(
                        if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5), 
                        RoundedCornerShape(24.dp)
                    )
                    .border(
                        0.5.dp,
                        if (isDark) Color(0xFF2E2E2E) else Color(0xFFE5E5E5),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(16.dp)
            ) {
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

    val blockColors = listOf(Color(0xFFFFFFFF), Color(0xFF00FFCC), Color(0xFFA200FF), Color(0xFFFF0080), Color(0xFF00E5FF))

    if (showAddBlockDialog) {
        com.example.ui.components.IOSAlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = "Add Task",
            content = {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Task Color", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        blockColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, androidx.compose.foundation.shape.CircleShape)
                                    .border(1.dp, if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.2f), androidx.compose.foundation.shape.CircleShape)
                                    .clickable { newBlockColor = color }
                                    .drawBehind {
                                        if (newBlockColor == color) {
                                            drawCircle(
                                                color = Color.Gray,
                                                radius = size.width / 2 + 4.dp.toPx(),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                            Color.Blue, Color.Magenta, Color.Red
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable { showCustomBlockColorPicker = true }
                                .drawBehind {
                                    if (!blockColors.contains(newBlockColor)) {
                                        drawCircle(
                                            color = Color.Gray,
                                            radius = size.width / 2 + 4.dp.toPx(),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                        )
                    }
                }
            },
            confirmButtonText = "Add",
            onConfirm = {
                val duration = newBlockDuration.toIntOrNull() ?: 25
                if (newBlockTitle.isNotBlank()) {
                    viewModel.addBlock(roomId, newBlockTitle.trim(), duration, newBlockColor.toArgb().toLong())
                }
                showAddBlockDialog = false
                newBlockTitle = ""
                newBlockDuration = ""
                newBlockColor = Color(0xFFFFFFFF)
            },
            dismissButtonText = "Cancel",
            onDismiss = { showAddBlockDialog = false }
        )
    }

    if (showEditTimerDialog) {
        com.example.ui.components.IOSAlertDialog(
            onDismissRequest = { showEditTimerDialog = false },
            title = "Edit Timer",
            content = {
                OutlinedTextField(
                    value = customEditTimerMins,
                    onValueChange = { customEditTimerMins = it },
                    label = { Text("Duration (mins)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButtonText = "Set",
            onConfirm = {
                val newDuration = customEditTimerMins.toIntOrNull()
                if (newDuration != null && newDuration > 0) {
                    viewModel.timeRemainingSecs.value = newDuration * 60
                    viewModel.maxTimeSecs.value = viewModel.timeRemainingSecs.value
                    viewModel.isOvertime.value = false
                    viewModel.overtimeSecs.value = 0
                    viewModel.stopTimer()
                    viewModel.activeBlockId.value = -1
                }
                showEditTimerDialog = false
            },
            dismissButtonText = "Cancel",
            onDismiss = { showEditTimerDialog = false }
        )
    }

    if (blockToEdit != null) {
        com.example.ui.components.IOSAlertDialog(
            onDismissRequest = { blockToEdit = null },
            title = "Edit Task",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editBlockTitle,
                        onValueChange = { editBlockTitle = it },
                        label = { Text("Task Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBlockDuration,
                        onValueChange = { editBlockDuration = it },
                        label = { Text("Duration (mins)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Task Color", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        blockColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, androidx.compose.foundation.shape.CircleShape)
                                    .border(1.dp, if(isDark) Color.White.copy(0.2f) else Color.Black.copy(0.2f), androidx.compose.foundation.shape.CircleShape)
                                    .clickable { editBlockColor = color }
                                    .drawBehind {
                                        if (editBlockColor == color) {
                                            drawCircle(
                                                color = Color.Gray,
                                                radius = size.width / 2 + 4.dp.toPx(),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                            Color.Blue, Color.Magenta, Color.Red
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable { showEditCustomBlockColorPicker = true }
                                .drawBehind {
                                    if (!blockColors.contains(editBlockColor)) {
                                        drawCircle(
                                            color = Color.Gray,
                                            radius = size.width / 2 + 4.dp.toPx(),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                        )
                    }
                }
            },
            confirmButtonText = "Save",
            onConfirm = {
                val duration = editBlockDuration.toIntOrNull() ?: 25
                if (editBlockTitle.isNotBlank()) {
                    viewModel.updateTimeBlockDetails(blockToEdit!!, editBlockTitle.trim(), duration, editBlockColor.toArgb().toLong())
                }
                blockToEdit = null
            },
            dismissButtonText = "Cancel",
            onDismiss = { blockToEdit = null }
        )
    }

    if (showShareDialog && r != null) {
        com.example.ui.components.IOSAlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = "Share Options",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("How would you like to share this room?", style = MaterialTheme.typography.bodyMedium)
                    
                    Button(
                        onClick = {
                            val blocksJson = timeBlocks.joinToString(prefix = "[", postfix = "]") {
                                "{\"title\": \"${it.title}\", \"durationMin\": ${it.durationMin}, \"colorArgb\": ${it.colorArgb}}"
                            }
                            val shareStr = """
                                {
                                    "name": "${r.name}",
                                    "colorArgb": ${r.colorArgb},
                                    "blocks": $blocksJson
                                }
                            """.trimIndent()
                            val encoded = android.util.Base64.encodeToString(shareStr.toByteArray(), android.util.Base64.NO_WRAP)
                            clipboardManager.setText(AnnotatedString("spacetime://$encoded"))
                            android.widget.Toast.makeText(context, "Clean room link copied!", android.widget.Toast.LENGTH_SHORT).show()
                            showShareDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clean Room Structure")
                    }

                    Button(
                        onClick = {
                            val blocksJson = timeBlocks.joinToString(prefix = "[", postfix = "]") {
                                "{\"title\": \"${it.title}\", \"durationMin\": ${it.durationMin}, \"colorArgb\": ${it.colorArgb}, \"isCompleted\": ${it.isCompleted}, \"assignedTime\": ${it.assignedTime}, \"timeTaken\": ${it.timeTaken}, \"timeLeft\": ${it.timeLeft}, \"overtime\": ${it.overtime}, \"completedAt\": ${it.completedAt}}"
                            }
                            val shareStr = """
                                {
                                    "name": "${r.name}",
                                    "colorArgb": ${r.colorArgb},
                                    "isCompleted": ${r.isCompleted},
                                    "totalSessionsCompleted": ${r.totalSessionsCompleted},
                                    "totalTimeLeft": ${r.totalTimeLeft},
                                    "totalOvertime": ${r.totalOvertime},
                                    "timeBank": ${r.timeBank},
                                    "blocks": $blocksJson
                                }
                            """.trimIndent()
                            val encoded = android.util.Base64.encodeToString(shareStr.toByteArray(), android.util.Base64.NO_WRAP)
                            clipboardManager.setText(AnnotatedString("spacetime://$encoded"))
                            android.widget.Toast.makeText(context, "Progressed room link copied!", android.widget.Toast.LENGTH_SHORT).show()
                            showShareDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Include All Progress")
                    }
                }
            },
            confirmButtonText = "Cancel",
            onConfirm = { showShareDialog = false }
        )
    }

    if (showCustomBlockColorPicker) {
        com.example.ui.components.PicsartColorPickerDialog(
            initialColor = newBlockColor,
            onColorSelected = { newBlockColor = it },
            onDismiss = { showCustomBlockColorPicker = false }
        )
    }

    if (showEditCustomBlockColorPicker) {
        com.example.ui.components.PicsartColorPickerDialog(
            initialColor = editBlockColor,
            onColorSelected = { editBlockColor = it },
            onDismiss = { showEditCustomBlockColorPicker = false }
        )
    }
}