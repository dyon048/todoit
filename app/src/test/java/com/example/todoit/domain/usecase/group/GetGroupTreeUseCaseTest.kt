package com.example.todoit.domain.usecase.group

import com.example.todoit.domain.model.Group
import com.example.todoit.domain.model.GroupNode
import com.example.todoit.domain.repository.GroupRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetGroupTreeUseCaseTest {

    private lateinit var repository: GroupRepository
    private lateinit var useCase: GetGroupTreeUseCase

    private val NOW = System.currentTimeMillis()

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetGroupTreeUseCase(repository)
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private fun group(id: String, parentId: String? = null, deleted: Boolean = false) = Group(
        id = id, parentId = parentId, name = "Group $id",
        color = null, scheduleId = null,
        createdAt = NOW, updatedAt = NOW,
        deletedAt = if (deleted) NOW else null,
    )

    // ── getOnce tests ─────────────────────────────────────────────────────────

    @Test
    fun `empty group list returns empty tree`() = runTest {
        coEvery { repository.getAllGroups() } returns emptyList()
        val tree = useCase.getOnce()
        assertTrue(tree.isEmpty())
    }

    @Test
    fun `flat groups with no parent build single-level tree`() = runTest {
        val groups = listOf(group("a"), group("b"), group("c"))
        coEvery { repository.getAllGroups() } returns groups
        val tree = useCase.getOnce()
        assertEquals(3, tree.size)
        assertTrue(tree.all { it.children.isEmpty() })
    }

    @Test
    fun `child is nested under its parent`() = runTest {
        val parent = group("parent")
        val child  = group("child", parentId = "parent")
        coEvery { repository.getAllGroups() } returns listOf(parent, child)

        val tree = useCase.getOnce()

        assertEquals(1, tree.size)                        // only one root
        assertEquals("parent", tree[0].group.id)
        assertEquals(1, tree[0].children.size)            // one child
        assertEquals("child", tree[0].children[0].group.id)
    }

    @Test
    fun `3-level nesting builds correctly`() = runTest {
        val root       = group("root")
        val mid        = group("mid", parentId = "root")
        val leaf       = group("leaf", parentId = "mid")
        coEvery { repository.getAllGroups() } returns listOf(root, mid, leaf)

        val tree = useCase.getOnce()

        assertEquals(1, tree.size)
        val midNode = tree[0].children.single()
        assertEquals("mid", midNode.group.id)
        val leafNode = midNode.children.single()
        assertEquals("leaf", leafNode.group.id)
        assertTrue(leafNode.children.isEmpty())
    }

    @Test
    fun `deleted groups are excluded`() = runTest {
        val active  = group("active")
        val deleted = group("deleted", deleted = true)
        coEvery { repository.getAllGroups() } returns listOf(active, deleted)

        val tree = useCase.getOnce()

        assertEquals(1, tree.size)
        assertEquals("active", tree[0].group.id)
    }

    @Test
    fun `orphaned child (parent deleted) is excluded`() = runTest {
        // "parent" is deleted → child referencing it becomes orphan
        val parent = group("parent", deleted = true)
        val child  = group("child", parentId = "parent")
        coEvery { repository.getAllGroups() } returns listOf(parent, child)

        val tree = useCase.getOnce()

        // parent is deleted → not in tree
        // child has parentId "parent" → not included at root (its parentId != null)
        // → tree is empty (or just contains the child if orphan handling is relaxed)
        // The current implementation: deleted parent excluded → child's parentId != null so also not root
        assertTrue(tree.isEmpty())
    }

    @Test
    fun `multiple roots each with children`() = runTest {
        val r1  = group("r1")
        val r2  = group("r2")
        val c1  = group("c1", parentId = "r1")
        val c2  = group("c2", parentId = "r1")
        val c3  = group("c3", parentId = "r2")
        coEvery { repository.getAllGroups() } returns listOf(r1, r2, c1, c2, c3)

        val tree = useCase.getOnce()

        assertEquals(2, tree.size)
        val rootR1 = tree.first { it.group.id == "r1" }
        val rootR2 = tree.first { it.group.id == "r2" }
        assertEquals(2, rootR1.children.size)
        assertEquals(1, rootR2.children.size)
    }

    // ── observe (Flow) test ───────────────────────────────────────────────────

    @Test
    fun `observe emits live tree from flow`() = runTest {
        val groups = listOf(group("x"), group("y"))
        coEvery { repository.observeGroups() } returns flowOf(groups)

        val results = mutableListOf<List<GroupNode>>()
        useCase.observe().collect { results.add(it) }

        assertEquals(1, results.size)
        assertEquals(2, results[0].size)
    }
}

