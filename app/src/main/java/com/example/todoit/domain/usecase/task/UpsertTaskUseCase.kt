package com.example.todoit.domain.usecase.task

import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.repository.TaskRepository
import com.example.todoit.domain.usecase.todo.SyncTodoStatusUseCase
import com.example.todoit.notification.ReminderScheduler
import java.util.UUID
import javax.inject.Inject

class UpsertTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val expandRecurrence: ExpandRecurrenceUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val syncTodoStatus: SyncTodoStatusUseCase,
) {
    suspend operator fun invoke(task: Task) {
        val now = System.currentTimeMillis()
        val saved = task.copy(
            id        = task.id.ifBlank { UUID.randomUUID().toString() },
            createdAt = if (task.createdAt == 0L) now else task.createdAt,
            updatedAt = now,
        )
        repository.upsertTask(saved)

        // Schedule or cancel exact-time reminder alarm
        val reminderAt = saved.reminderAt
        if (reminderAt != null && reminderAt > now && saved.status != TaskStatus.DONE) {
            reminderScheduler.scheduleReminder(saved.id, saved.title, reminderAt)
        } else {
            reminderScheduler.cancelReminder(saved.id)
        }

        if (saved.recurrenceId != null) expandRecurrence(saved)

        // Keep parent todo's status in sync
        syncTodoStatus(saved.todoId)
    }
}
