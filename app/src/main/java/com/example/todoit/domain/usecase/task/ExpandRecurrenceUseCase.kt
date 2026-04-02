package com.example.todoit.domain.usecase.task

import com.example.todoit.domain.engine.RecurrenceExpander
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.repository.RecurrenceRepository
import com.example.todoit.domain.repository.TaskRepository
import javax.inject.Inject

class ExpandRecurrenceUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val recurrenceRepository: RecurrenceRepository,
    private val expander: RecurrenceExpander,
) {
    /**
     * If [task] has a recurrenceId, expands future instances and saves them.
     * Existing instances are deduplicated by parentTaskId + recurrenceInstanceDate.
     */
    suspend operator fun invoke(task: Task) {
        val recurrenceId = task.recurrenceId ?: return
        val recurrence   = recurrenceRepository.getRecurrenceById(recurrenceId) ?: return

        val existingInstances = taskRepository.getTasksByTodo(task.todoId)
            .filter { it.parentTaskId == task.id }
            .mapNotNull { it.recurrenceInstanceDate }
            .toSet()

        val newInstances = expander.expand(task, recurrence)
            .filter { it.recurrenceInstanceDate !in existingInstances }

        newInstances.forEach { taskRepository.upsertTask(it) }
    }
}


