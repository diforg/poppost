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
import kotlinx.coroutines.launch
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

    // -------------------------------------------------------------------------
    // Testes da nova API de criação (commit 6)
    // -------------------------------------------------------------------------

    @Test
    fun onCreateContentChanged_withValidText_updatesCounterAndSetsValid() = runTest {
        val viewModel = buildViewModel()
        val text = "Olá mundo"

        viewModel.onCreateContentChanged(text)

        val state = viewModel.createPostUiState.value
        assertEquals(text, state.content)
        assertEquals(PostViewModel.MAX_POST_LENGTH - text.length, state.remainingCharacters)
        assertTrue("isValid deveria ser true para texto válido", state.isValid)
        assertEquals(null, state.validationError)
    }

    @Test
    fun onCreateContentChanged_withTooLongText_setsErrorAndInvalidState() = runTest {
        val viewModel = buildViewModel()
        val tooLong = "x".repeat(PostViewModel.MAX_POST_LENGTH + 1)

        viewModel.onCreateContentChanged(tooLong)

        val state = viewModel.createPostUiState.value
        assertEquals(false, state.isValid)
        assertEquals(PostViewModel.CreatePostValidationError.TOO_LONG, state.validationError)
        // remainingCharacters deve ser negativo
        assertTrue("remainingCharacters deve ser negativo", state.remainingCharacters < 0)
    }

    @Test
    fun onCreatePostSubmit_withValidContent_insertsPostEmitsSuccessAndResetsState() = runTest {
        val viewModel = buildViewModel()
        val events = mutableListOf<PostViewModel.CreatePostUiEvent>()
        // Inicia coleta de eventos antes do submit
        val collectJob = launch { viewModel.createPostEvents.collect { events.add(it) } }

        viewModel.onCreateContentChanged("Meu post válido")
        viewModel.onCreatePostSubmit()
        advanceUntilIdle()
        collectJob.cancel()

        // Post inserido na lista ativa
        assertEquals(1, viewModel.activePosts.value.size)
        assertEquals("Meu post válido", viewModel.activePosts.value.first().content)
        // Evento de sucesso emitido
        assertEquals(1, events.size)
        assertEquals(PostViewModel.CreatePostUiEvent.Success, events.first())
        // Estado resetado após sucesso
        val state = viewModel.createPostUiState.value
        assertEquals("", state.content)
        assertEquals(false, state.isSubmitting)
        assertEquals(PostViewModel.MAX_POST_LENGTH, state.remainingCharacters)
    }

    @Test
    fun onCreatePostSubmit_withBlankContent_emitsErrorEmptyAndDoesNotInsert() = runTest {
        val viewModel = buildViewModel()
        val events = mutableListOf<PostViewModel.CreatePostUiEvent>()
        val collectJob = launch { viewModel.createPostEvents.collect { events.add(it) } }
        advanceUntilIdle() // Garante coletor ativo antes da emissão imediata de erro

        // Apenas espaços em branco — inválido após trim
        viewModel.onCreateContentChanged("   ")
        viewModel.onCreatePostSubmit()
        advanceUntilIdle()
        collectJob.cancel()

        assertTrue("Nenhum post deve ser inserido", viewModel.activePosts.value.isEmpty())
        assertEquals(1, events.size)
        val errorEvent = events.first() as PostViewModel.CreatePostUiEvent.Error
        assertEquals(PostViewModel.CreatePostValidationError.EMPTY, errorEvent.reason)
    }

    @Test
    fun onCreatePostSubmit_withTooLongContent_emitsErrorTooLongAndDoesNotInsert() = runTest {
        val viewModel = buildViewModel()
        val events = mutableListOf<PostViewModel.CreatePostUiEvent>()
        val collectJob = launch { viewModel.createPostEvents.collect { events.add(it) } }
        advanceUntilIdle() // Garante coletor ativo antes da emissão imediata de erro

        viewModel.onCreateContentChanged("y".repeat(PostViewModel.MAX_POST_LENGTH + 1))
        viewModel.onCreatePostSubmit()
        advanceUntilIdle()
        collectJob.cancel()

        assertTrue("Nenhum post deve ser inserido", viewModel.activePosts.value.isEmpty())
        assertEquals(1, events.size)
        val errorEvent = events.first() as PostViewModel.CreatePostUiEvent.Error
        assertEquals(PostViewModel.CreatePostValidationError.TOO_LONG, errorEvent.reason)
    }

    @Test
    fun onCreatePostSubmit_calledTwiceRapidly_insertsOnlyOnce() = runTest {
        val viewModel = buildViewModel()

        viewModel.onCreateContentChanged("Post único")
        // Primeiro submit: lança coroutine mas não avança ainda (StandardTestDispatcher)
        viewModel.onCreatePostSubmit()
        // Segundo submit imediato: isSubmitting já deve ser true → deve ser ignorado
        viewModel.onCreatePostSubmit()
        advanceUntilIdle()

        assertEquals(
            "Somente um post deve ser inserido mesmo com dois submits rápidos",
            1,
            viewModel.activePosts.value.size
        )
    }

    @Test
    fun resetCreatePostState_whenNotSubmitting_clearsAllFields() = runTest {
        val viewModel = buildViewModel()

        viewModel.onCreateContentChanged("Texto que será descartado")
        // Garante estado não-vazio antes do reset
        assertEquals("Texto que será descartado", viewModel.createPostUiState.value.content)

        viewModel.resetCreatePostState()

        val state = viewModel.createPostUiState.value
        assertEquals("", state.content)
        assertEquals(PostViewModel.MAX_POST_LENGTH, state.remainingCharacters)
        assertEquals(false, state.isValid)
        assertEquals(false, state.isSubmitting)
        assertEquals(null, state.validationError)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    override suspend fun insertAll(posts: List<PostEntity>) {
        posts.forEach { post ->
            this.posts.removeAll { it.id == post.id }
            this.posts.add(post)
        }
        publish()
    }

    override fun getAllActive(): Flow<List<PostEntity>> = activePostsFlow

    override fun getAllArchived(): Flow<List<PostEntity>> = archivedPostsFlow

    override suspend fun getAllPostsSnapshot(): List<PostEntity> {
        return posts.sortedByDescending { it.createdAt }
    }

    override suspend fun updateArchiveStatus(id: String, isArchived: Boolean): Int {
        val index = posts.indexOfFirst { it.id == id }
        if (index == -1) return 0

        posts[index] = posts[index].copy(isArchived = isArchived)
        publish()
        return 1
    }

    override suspend fun deleteById(id: String): Int {
        val removed = posts.removeAll { it.id == id }
        if (removed) {
            publish()
            return 1
        }
        return 0
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


