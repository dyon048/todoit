package com.example.todoit.domain.usecase.task

import com.example.todoit.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String) {
        // Also cascade-delete all recurrence instances of this parent task
        repository.softDeleteInstancesByParent(taskId)
        repository.softDeleteTask(taskId)
    }
}

