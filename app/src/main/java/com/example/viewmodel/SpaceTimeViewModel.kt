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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlinx.coroutines.withContext
import android.graphics.Color

class SpaceTimeViewModel(private val repository: SpaceTimeRepository) : ViewModel() {
    
    val rooms = repository.allRooms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isGenerating = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    fun getRoom(id: Int) = repository.getRoomById(id)
    fun getTimeBlocks(id: Int) = repository.getTimeBlocksForRoom(id)

    fun createRoomFromText(prompt: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isGenerating.value = true
            errorMessage.value = null
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val schemaStr = """
                {
                  "type": "OBJECT",
                  "properties": {
                    "roomName": { "type": "STRING", "description": "Creative, short room name. Eg. Space Revision" },
                    "colorHex": { "type": "STRING", "description": "Neon 6-char hex color, no hash. Eg. 00FFCC or A200FF" },
                    "blocks": {
                      "type": "ARRAY",
                      "items": {
                        "type": "OBJECT",
                        "properties": {
                          "title": { "type": "STRING", "description": "Task or break name" },
                          "durationMin": { "type": "INTEGER", "description": "Duration in minutes" }
                        }
                      }
                    }
                  }
                }
                """.trimIndent()
                
                val schemaObj = Json.parseToJsonElement(schemaStr).jsonObject

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = "Generate a productivity pomodoro schedule based on: " + prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = "You are an AI for a futuristic space-themed pomodoro app. Parse user prompts into strict JSON schedules. Colors should be neon and vibrant."))),
                    generationConfig = GenerationConfig(
                        temperature = 0.5f,
                        responseFormat = ResponseFormat(
                            text = ResponseFormatText(
                                mimeType = "application/json",
                                schema = schemaObj
                            )
                        )
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
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
            } catch (e: Exception) {
                errorMessage.value = "Error generating schedule: ${e.message}"
            } finally {
                isGenerating.value = false
            }
        }
    }

    fun completeBlock(block: TimeBlockEntity) {
        viewModelScope.launch {
            repository.updateTimeBlock(block.copy(isCompleted = !block.isCompleted))
            // simplistic logic: just toggle
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
