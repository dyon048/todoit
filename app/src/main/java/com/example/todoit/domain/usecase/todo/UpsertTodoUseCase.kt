package com.example.todoit.domain.usecase.todo

import com.example.todoit.domain.model.TodoItem
import com.example.todoit.domain.repository.TodoRepository
import java.util.UUID
import javax.inject.Inject

class UpsertTodoUseCase @Inject constructor(
    private val repository: TodoRepository,
) {
    suspend operator fun invoke(todo: TodoItem) {
        val now = System.currentTimeMillis()
        repository.upsertTodo(
            todo.copy(
                id = todo.id.ifBlank { UUID.randomUUID().toString() },
                createdAt = if (todo.createdAt == 0L) now else todo.createdAt,
                updatedAt = now,
            )
        )
    }
}

