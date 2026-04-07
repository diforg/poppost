package com.poppost.viewmodel

import com.poppost.data.local.PostDao
import com.poppost.data.local.PostEntity
import com.poppost.data.repository.PostRepository
import com.poppost.domain.model.Post
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun createPost_addsNewActivePostWhenValid() = runTest {
        val viewModel = buildViewModel()

        viewModel.createPost("Meu primeiro pop")
        advanceUntilIdle()

        assertEquals(1, viewModel.activePosts.value.size)
        assertEquals("Meu primeiro pop", viewModel.activePosts.value.first().content)
        assertTrue(viewModel.archivedPosts.value.isEmpty())
    }

    @Test
    fun createPost_ignoresBlankOrTooLongContent() = runTest {
        val viewModel = buildViewModel()

        viewModel.createPost("   ")
        viewModel.createPost("x".repeat(PostViewModel.MAX_POST_LENGTH + 1))
        advanceUntilIdle()

        assertTrue(viewModel.activePosts.value.isEmpty())
    }

    @Test
    fun archiveAndUnarchive_movesPostBetweenLists() = runTest {
        val dao = FakePostDao()
        val repository = PostRepository(dao)
        val post = Post(
            id = "post-1",
            content = "Conteudo",
            createdAt = System.currentTimeMillis(),
            isArchived = false
        )
        repository.insert(post)

        val viewModel = PostViewModel(repository)
        advanceUntilIdle()

        viewModel.archivePost("post-1")
        advanceUntilIdle()
        assertTrue(viewModel.activePosts.value.isEmpty())
        assertEquals(1, viewModel.archivedPosts.value.size)

        viewModel.unarchivePost("post-1")
        advanceUntilIdle()
        assertEquals(1, viewModel.activePosts.value.size)
        assertTrue(viewModel.archivedPosts.value.isEmpty())
    }

    @Test
    fun filterByDate_filtersOnlyMatchingCalendarDay() = runTest {
        val dao = FakePostDao()
        val repository = PostRepository(dao)
        val dayA = LocalDate.of(2026, 4, 7)
        val dayB = dayA.minusDays(1)

        repository.insert(
            Post(
                id = "a",
                content = "Hoje",
                createdAt = dayA.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                isArchived = false
            )
        )
        repository.insert(
            Post(
                id = "b",
                content = "Ontem",
                createdAt = dayB.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                isArchived = false
            )
        )

        val viewModel = PostViewModel(repository)
        advanceUntilIdle()
        assertEquals(2, viewModel.activePosts.value.size)

        viewModel.filterByDate(dayA)
        advanceUntilIdle()

        assertEquals(dayA, viewModel.activeDateFilter.value)
        assertEquals(1, viewModel.activePosts.value.size)
        assertEquals("a", viewModel.activePosts.value.first().id)
    }

    private fun buildViewModel(): PostViewModel {
        val repository = PostRepository(FakePostDao())
        return PostViewModel(repository)
    }
}

private class FakePostDao : PostDao {
    private val posts = mutableListOf<PostEntity>()
    private val activePostsFlow = MutableStateFlow<List<PostEntity>>(emptyList())
    private val archivedPostsFlow = MutableStateFlow<List<PostEntity>>(emptyList())

    override suspend fun insert(post: PostEntity) {
        posts.removeAll { it.id == post.id }
        posts.add(post)
        publish()
    }

    override fun getAllActive(): Flow<List<PostEntity>> = activePostsFlow

    override fun getAllArchived(): Flow<List<PostEntity>> = archivedPostsFlow

    override suspend fun updateArchiveStatus(id: String, isArchived: Boolean): Int {
        val index = posts.indexOfFirst { it.id == id }
        if (index == -1) return 0

        posts[index] = posts[index].copy(isArchived = isArchived)
        publish()
        return 1
    }

    private fun publish() {
        activePostsFlow.value = posts
            .filter { !it.isArchived }
            .sortedByDescending { it.createdAt }

        archivedPostsFlow.value = posts
            .filter { it.isArchived }
            .sortedByDescending { it.createdAt }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}


