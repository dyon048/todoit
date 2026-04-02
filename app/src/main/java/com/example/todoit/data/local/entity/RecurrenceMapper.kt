package com.example.todoit.data.local.entity

import com.example.todoit.domain.model.Recurrence
import com.example.todoit.domain.model.RecurrenceType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

fun RecurrenceEntity.toDomain() = Recurrence(
    id = id,
    type = RecurrenceType.valueOf(type),
    interval = interval,
    daysOfWeek = daysOfWeek?.let { Json.decodeFromString(ListSerializer(Int.serializer()), it) },
    endDate = endDate,
    maxOccurrences = maxOccurrences,
    updatedAt = updatedAt,
)

fun Recurrence.toEntity(syncedAt: Long? = null) = RecurrenceEntity(
    id = id,
    type = type.name,
    interval = interval,
    daysOfWeek = daysOfWeek?.let { Json.encodeToString(ListSerializer(Int.serializer()), it) },
    endDate = endDate,
    maxOccurrences = maxOccurrences,
    updatedAt = updatedAt,
    syncedAt = syncedAt,
)
