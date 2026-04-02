package com.example.todoit.domain.usecase.group

import com.example.todoit.domain.repository.GroupRepository
import com.example.todoit.domain.repository.TaskRepository
import com.example.todoit.domain.repository.TodoRepository
import javax.inject.Inject

class DeleteGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val todoRepository: TodoRepository,
    private val taskRepository: TaskRepository,
) {
    /** Recursively soft-deletes a group and all its children, their todos, and tasks. */
    suspend operator fun invoke(groupId: String) {
        deleteRecursive(groupId)
    }

    private suspend fun deleteRecursive(groupId: String) {
        // Delete all child groups first
        val allGroups = groupRepository.getAllGroups()
        val children = allGroups.filter { it.parentId == groupId && it.deletedAt == null }
        children.forEach { deleteRecursive(it.id) }

        // Delete todos and their tasks in this group
        val todos = todoRepository.getTodosByGroup(groupId)
        todos.forEach { todo ->
            taskRepository.softDeleteTasksByTodo(todo.id)
        }
        todoRepository.softDeleteTodosByGroup(groupId)

        // Delete the group itself
        groupRepository.softDeleteGroup(groupId)
    }
}

