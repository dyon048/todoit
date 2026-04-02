package com.example.todoit.data.local.entity

import com.example.todoit.domain.model.Schedule
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

fun ScheduleEntity.toDomain() = Schedule(
    id = id,
    name = name,
    activeDays = Json.decodeFromString(ListSerializer(Int.serializer()), activeDays),
    startTimeOfDay = startTimeOfDay,
    endTimeOfDay = endTimeOfDay,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Schedule.toEntity(syncedAt: Long? = null) = ScheduleEntity(
    id = id,
    name = name,
    activeDays = Json.encodeToString(ListSerializer(Int.serializer()), activeDays),
    startTimeOfDay = startTimeOfDay,
    endTimeOfDay = endTimeOfDay,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncedAt = syncedAt,
)
