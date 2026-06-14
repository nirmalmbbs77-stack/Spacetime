package com.example.data

import kotlinx.coroutines.flow.Flow

class SpaceTimeRepository(private val dao: SpaceTimeDao) {
    val allRooms: Flow<List<RoomEntity>> = dao.getAllRooms()

    fun getRoomById(id: Int): Flow<RoomEntity?> = dao.getRoomById(id)

    suspend fun getRoomByIdOneShot(id: Int): RoomEntity? = dao.getRoomByIdOneShot(id)

    suspend fun insertRoom(room: RoomEntity): Long = dao.insertRoom(room)

    suspend fun updateRoom(room: RoomEntity) = dao.updateRoom(room)

    suspend fun updateRooms(rooms: List<RoomEntity>) = dao.updateRooms(rooms)

    suspend fun deleteRoomById(id: Int) = dao.deleteRoomById(id)

    fun getTimeBlocksForRoom(roomId: Int): Flow<List<TimeBlockEntity>> = dao.getTimeBlocksForRoom(roomId)

    suspend fun insertTimeBlock(block: TimeBlockEntity) = dao.insertTimeBlock(block)

    suspend fun insertTimeBlocks(blocks: List<TimeBlockEntity>) = dao.insertTimeBlocks(blocks)

    suspend fun updateTimeBlock(block: TimeBlockEntity) = dao.updateTimeBlock(block)

    suspend fun updateTimeBlocks(blocks: List<TimeBlockEntity>) = dao.updateTimeBlocks(blocks)

    suspend fun deleteTimeBlockById(id: Int) = dao.deleteTimeBlockById(id)
}
