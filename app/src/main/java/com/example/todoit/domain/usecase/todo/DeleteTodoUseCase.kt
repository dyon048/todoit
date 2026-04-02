package com.example.todoit.domain.usecase.todo

import com.example.todoit.domain.repository.TaskRepository
import com.example.todoit.domain.repository.TodoRepository
import javax.inject.Inject

class DeleteTodoUseCase @Inject constructor(
    private val todoRepository: TodoRepository,
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(todoId: String) {
        taskRepository.softDeleteTasksByTodo(todoId)
        todoRepository.softDeleteTodo(todoId)
    }
}

