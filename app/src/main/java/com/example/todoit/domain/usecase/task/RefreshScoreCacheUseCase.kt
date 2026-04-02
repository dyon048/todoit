package com.example.todoit.domain.usecase.task

import com.example.todoit.domain.engine.ScheduleEvaluator
import com.example.todoit.domain.engine.TaskScorer
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.repository.GroupRepository
import com.example.todoit.domain.repository.LocationRepository
import com.example.todoit.domain.repository.ScheduleRepository
import com.example.todoit.domain.repository.TaskRepository
import com.example.todoit.domain.repository.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RefreshScoreCacheUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val todoRepository: TodoRepository,
    private val groupRepository: GroupRepository,
    private val scheduleRepository: ScheduleRepository,
    private val locationRepository: LocationRepository,
    private val taskScorer: TaskScorer,
    private val scheduleEvaluator: ScheduleEvaluator,
) {
    /**
     * Recalculates score_cache for every non-DONE task.
     * [userLat]/[userLng] from last known passive location — null if unavailable.
     */
    suspend operator fun invoke(userLat: Double? = null, userLng: Double? = null) =
        withContext(Dispatchers.Default) {
            val now    = System.currentTimeMillis()
            val tasks  = taskRepository.getAllActiveTasks()
                .filter { it.status != TaskStatus.DONE }

            tasks.forEach { task ->
                val location  = task.locationId?.let { locationRepository.getLocationById(it) }
                val schedule  = resolveSchedule(task)
                val newScore  = taskScorer.score(task, location, userLat, userLng, schedule, now)
                if (newScore != task.scoreCache) {
                    taskRepository.updateScoreCache(task.id, newScore)
                }
            }
        }

    private suspend fun resolveSchedule(task: Task) = runCatching {
        val schedId = task.scheduleId
            ?: if (task.inheritSchedule) {
                val todo  = todoRepository.getTodoById(task.todoId) ?: return@runCatching null
                val group = groupRepository.getGroupById(todo.groupId) ?: return@runCatching null
                group.scheduleId
            } else null
        schedId?.let { scheduleRepository.getScheduleById(it) }
    }.getOrNull()
}



