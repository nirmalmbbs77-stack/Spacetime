package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.*
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlinx.coroutines.withContext
import android.graphics.Color

class SpaceTimeViewModel(private val repository: SpaceTimeRepository) : ViewModel() {
    
    val isDarkMode = MutableStateFlow(true)

    fun toggleTheme() {
        isDarkMode.value = !isDarkMode.value
    }

    val rooms = repository.allRooms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isGenerating = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    // Global Timer State
    var activeBlockId = MutableStateFlow(-1)
    var timeRemainingSecs = MutableStateFlow(0)
    var overtimeSecs = MutableStateFlow(0)
    var isOvertime = MutableStateFlow(false)
    var maxTimeSecs = MutableStateFlow(0)
    var isTimerRunning = MutableStateFlow(false)
    var lastTickTime = MutableStateFlow(0L)

    private var timerJob: kotlinx.coroutines.Job? = null

    var onTimerCompleteNotification: ((String, String) -> Unit)? = null

    fun resetTimerState() {
        isTimerRunning.value = false
        timerJob?.cancel()
        timerJob = null
        timeRemainingSecs.value = 0
        overtimeSecs.value = 0
        isOvertime.value = false
        maxTimeSecs.value = 0
        activeBlockId.value = -1
        lastTickTime.value = 0L
    }

    fun startTimer() {
        if (!isTimerRunning.value) {
            isTimerRunning.value = true
            lastTickTime.value = System.currentTimeMillis()
            timerJob = viewModelScope.launch(Dispatchers.Default) {
                while (isTimerRunning.value) {
                    kotlinx.coroutines.delay(500L)
                    val currentNow = System.currentTimeMillis()
                    val elapsed = (currentNow - lastTickTime.value) / 1000L
                    if (elapsed > 0) {
                        withContext(Dispatchers.Main) {
                            if (!isOvertime.value) {
                                timeRemainingSecs.value -= elapsed.toInt()
                                if (timeRemainingSecs.value <= 0) {
                                    val overThreshold = -timeRemainingSecs.value
                                    timeRemainingSecs.value = 0
                                    isOvertime.value = true
                                    overtimeSecs.value = overThreshold
                                    onTimerCompleteNotification?.invoke(
                                        "Time's Up! Overtime Started",
                                        "Assigned time has finished."
                                    )
                                }
                            } else {
                                overtimeSecs.value += elapsed.toInt()
                            }
                            lastTickTime.value += elapsed * 1000L
                        }
                    }
                }
            }
        }
    }

    fun stopTimer() {
        isTimerRunning.value = false
        timerJob?.cancel()
        timerJob = null
    }

    fun toggleTimer() {
        if (isTimerRunning.value) stopTimer() else startTimer()
    }

    fun createManualRoom(name: String, colorArgb: Long, iconName: String = "Star") {
        viewModelScope.launch(Dispatchers.IO) {
            val maxOrder = rooms.value.maxOfOrNull { it.orderIndex } ?: 0
            val newRoom = RoomEntity(
                name = name,
                colorArgb = colorArgb,
                iconName = iconName,
                totalSessionsCompleted = 0,
                orderIndex = maxOrder + 1
            )
            repository.insertRoom(newRoom)
        }
    }

    fun updateRoomOrder(orderedRooms: List<RoomEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = orderedRooms.mapIndexed { index, room ->
                room.copy(orderIndex = index)
            }
            repository.updateRooms(updated)
        }
    }

    fun updateTimeBlockOrder(orderedBlocks: List<TimeBlockEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = orderedBlocks.mapIndexed { index, block ->
                block.copy(orderIndex = index)
            }
            repository.updateTimeBlocks(updated)
        }
    }

    fun createImportedRoomFromJson(jsonObj: org.json.JSONObject) {
        viewModelScope.launch(Dispatchers.IO) {
            val name = jsonObj.getString("name")
            val colorArgb = jsonObj.getLong("colorArgb")
            val blocksArray = jsonObj.getJSONArray("blocks")
            
            val newRoom = RoomEntity(
                name = name,
                colorArgb = colorArgb,
                iconName = "Star",
                isCompleted = jsonObj.optBoolean("isCompleted", false),
                totalSessionsCompleted = jsonObj.optInt("totalSessionsCompleted", 0),
                totalTimeLeft = jsonObj.optInt("totalTimeLeft", 0),
                totalOvertime = jsonObj.optInt("totalOvertime", 0),
                timeBank = jsonObj.optInt("timeBank", 0)
            )
            val newRoomId = repository.insertRoom(newRoom).toInt()
            for (i in 0 until blocksArray.length()) {
                val blockObj = blocksArray.getJSONObject(i)
                val title = blockObj.getString("title")
                val duration = blockObj.getInt("durationMin")
                val color = if (blockObj.has("colorArgb")) blockObj.getLong("colorArgb") else 0xFFFFFFFF
                repository.insertTimeBlock(TimeBlockEntity(
                    roomId = newRoomId,
                    title = title,
                    durationMin = duration,
                    orderIndex = i,
                    colorArgb = color,
                    isCompleted = blockObj.optBoolean("isCompleted", false),
                    assignedTime = blockObj.optInt("assignedTime", 0),
                    timeTaken = blockObj.optInt("timeTaken", 0),
                    timeLeft = blockObj.optInt("timeLeft", 0),
                    overtime = blockObj.optInt("overtime", 0),
                    completedAt = blockObj.optLong("completedAt", 0L)
                ))
            }
        }
    }

    fun getRoom(id: Int) = repository.getRoomById(id)
    fun getTimeBlocks(id: Int) = repository.getTimeBlocksForRoom(id)

    fun markRoomAsUsed(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val room = repository.getRoomByIdOneShot(id) ?: return@launch
            val updated = room.copy(lastUsedAt = System.currentTimeMillis())
            repository.updateRoom(updated)
        }
    }

    fun createRoomFromText(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isGenerating.value = true
            errorMessage.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    errorMessage.value = "To use AI, please set your Gemini API key in the Secrets panel of Google AI Studio."
                    isGenerating.value = false
                    return@launch
                }
                
                val promptText = """
                    Generate a productivity pomodoro schedule based on: $prompt.
                    Return ONLY valid JSON in this exact format:
                    {
                      "roomName": "Creative short name",
                      "colorHex": "A200FF or 00FFCC (no hash)",
                      "blocks": [
                        { "title": "Focus Session", "durationMin": 45 },
                        { "title": "Short Break", "durationMin": 10 }
                      ]
                    }
                    Do not add markdown formatting, backticks, or any other text.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    systemInstruction = Content(parts = listOf(Part(text = "You are an AI for a futuristic space-themed pomodoro app. Output strictly standard JSON."))),
                    generationConfig = GenerationConfig(
                        temperature = 0.5f,
                        responseMimeType = "application/json"
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                var responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
                    responseText = responseText.trim()
                    if (responseText.startsWith("```json")) {
                        responseText = responseText.substringAfter("```json")
                    }
                    if (responseText.startsWith("```")) {
                        responseText = responseText.substringAfter("```")
                    }
                    if (responseText.endsWith("```")) {
                        responseText = responseText.substringBeforeLast("```")
                    }
                    responseText = responseText.trim()

                    val jsonObj = Json.parseToJsonElement(responseText).jsonObject
                    val roomName = jsonObj["roomName"]?.jsonPrimitive?.content ?: "AI Room"
                    val colorHex = jsonObj["colorHex"]?.jsonPrimitive?.content ?: "00FFCC"
                    val colorLong = try {
                        android.graphics.Color.parseColor("#${colorHex.trim('#')}").toLong()
                    } catch (e: Exception) {
                        0xFF00FFCC // fallback cyan
                    }
                    
                    val newRoom = RoomEntity(
                        name = roomName,
                        colorArgb = colorLong,
                        totalSessionsCompleted = 0
                    )
                    
                    val newRoomId = repository.insertRoom(newRoom).toInt()
                    
                    val blocksArray = jsonObj["blocks"]?.jsonArray
                    blocksArray?.forEachIndexed { index, blockEl ->
                        val blockObj = blockEl.jsonObject
                        val title = blockObj["title"]?.jsonPrimitive?.content ?: "Focus"
                        val duration = blockObj["durationMin"]?.jsonPrimitive?.int ?: 25
                        
                        repository.insertTimeBlock(TimeBlockEntity(
                            roomId = newRoomId,
                            title = title,
                            durationMin = duration,
                            orderIndex = index
                        ))
                    }
                } else {
                    errorMessage.value = "Failed to parse AI response."
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                errorMessage.value = "HTTP ${e.code()}: $errorBody"
            } catch (e: Exception) {
                errorMessage.value = "Error generating schedule: ${e.message}"
            } finally {
                isGenerating.value = false
            }
        }
    }

    fun completeBlock(block: TimeBlockEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTimeBlock(block.copy(isCompleted = !block.isCompleted))
            // simplistic logic: just toggle
        }
    }

    fun recordTaskCompletion(room: RoomEntity, block: TimeBlockEntity, timeTakenSecs: Int, overtimeSecs: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val assignedTimeSecs = block.durationMin * 60
            val finalTimeTaken = if (timeTakenSecs > assignedTimeSecs) assignedTimeSecs else timeTakenSecs
            val timeLeftSecs = maxOf(0, assignedTimeSecs - finalTimeTaken)
            
            val updatedBlock = block.copy(
                isCompleted = !block.isCompleted,
                assignedTime = assignedTimeSecs,
                timeTaken = finalTimeTaken,
                timeLeft = timeLeftSecs,
                overtime = overtimeSecs,
                completedAt = System.currentTimeMillis()
            )
            repository.updateTimeBlock(updatedBlock)
            
            if (updatedBlock.isCompleted) {
                val addedTimeLeft = if (timeTakenSecs > 0) timeLeftSecs else 0
                val updatedTotalTimeLeft = room.totalTimeLeft + addedTimeLeft
                val updatedTotalOvertime = room.totalOvertime + overtimeSecs
                val newTimeBank = updatedTotalTimeLeft - updatedTotalOvertime
                
                val updatedRoom = room.copy(
                    totalTimeLeft = updatedTotalTimeLeft,
                    totalOvertime = updatedTotalOvertime,
                    timeBank = newTimeBank,
                    totalSessionsCompleted = room.totalSessionsCompleted + 1,
                    lastUsedAt = System.currentTimeMillis()
                )
                repository.updateRoom(updatedRoom)
            } else {
                // If they un-check it, we might want to revert, but let's keep it simple.
            }
        }
    }

    // For non-block tasks (manual sessions)
    fun recordManualSessionCompletion(room: RoomEntity, assignedTimeSecs: Int, timeTakenSecs: Int, overtimeSecs: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val finalTimeTaken = if (timeTakenSecs > assignedTimeSecs) assignedTimeSecs else timeTakenSecs
            val timeLeftSecs = maxOf(0, assignedTimeSecs - finalTimeTaken)
            
            val updatedTotalTimeLeft = room.totalTimeLeft + timeLeftSecs
            val updatedTotalOvertime = room.totalOvertime + overtimeSecs
            val newTimeBank = updatedTotalTimeLeft - updatedTotalOvertime
            
            val updatedRoom = room.copy(
                totalTimeLeft = updatedTotalTimeLeft,
                totalOvertime = updatedTotalOvertime,
                timeBank = newTimeBank,
                totalSessionsCompleted = room.totalSessionsCompleted + 1,
                lastUsedAt = System.currentTimeMillis()
            )
            repository.updateRoom(updatedRoom)
        }
    }
    
    fun addBlock(roomId: Int, title: String, durationMin: Int, colorArgb: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val newBlock = TimeBlockEntity(
                roomId = roomId,
                title = title,
                durationMin = durationMin,
                // Using current time as a simple way to append to the end of the list
                orderIndex = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                colorArgb = colorArgb
            )
            repository.insertTimeBlock(newBlock)
        }
    }

    fun deleteBlock(block: TimeBlockEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTimeBlockById(block.blockId)
        }
    }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRoomById(room.roomId) // It should be roomId, let's check RoomEntity.
        }
    }

    fun updateRoomNameAndColor(room: RoomEntity, newName: String, newColorArgb: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = room.copy(name = newName, colorArgb = newColorArgb)
            repository.updateRoom(updated)
        }
    }

    fun updateTimeBlockDetails(block: TimeBlockEntity, newTitle: String, newDuration: Int, newColorArgb: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = block.copy(title = newTitle, durationMin = newDuration, colorArgb = newColorArgb)
            repository.updateTimeBlock(updated)
        }
    }
}

class SpaceTimeViewModelFactory(private val repository: SpaceTimeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpaceTimeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpaceTimeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
