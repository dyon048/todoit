package com.example.todoit.domain.usecase.task

import com.example.todoit.domain.model.Task
import com.example.todoit.domain.repository.TaskRepository
import java.util.UUID
import javax.inject.Inject

class UpsertTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val expandRecurrence: ExpandRecurrenceUseCase,
) {
    suspend operator fun invoke(task: Task) {
        val now = System.currentTimeMillis()
        val saved = task.copy(
            id        = task.id.ifBlank { UUID.randomUUID().toString() },
            createdAt = if (task.createdAt == 0L) now else task.createdAt,
            updatedAt = now,
        )
        repository.upsertTask(saved)
        if (saved.recurrenceId != null) expandRecurrence(saved)
    }
}
