package com.poppost.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poppost.data.repository.PostCsvRestoreService
import com.poppost.data.repository.PostRepository
import com.poppost.data.repository.CsvImportResult
import com.poppost.domain.model.Post
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel(
    private val repository: PostRepository
) : ViewModel() {

    private val csvRestoreService = PostCsvRestoreService()

    data class ArchivedUiState(
        val posts: List<Post> = emptyList(),
    ) {
        val isEmpty: Boolean
            get() = posts.isEmpty()
    }

    data class CreatePostUiState(
        val content: String = "",
        val remainingCharacters: Int = MAX_POST_LENGTH,
        val isValid: Boolean = false,
        val isSubmitting: Boolean = false,
        val validationError: CreatePostValidationError? = null,
    )

    enum class CreatePostValidationError {
        EMPTY,
        TOO_LONG,
        PERSISTENCE,
    }

    sealed interface CreatePostUiEvent {
        data object Success : CreatePostUiEvent
        data class Error(val reason: CreatePostValidationError) : CreatePostUiEvent
    }

    private val _activeDateFilter = MutableStateFlow<LocalDate?>(null)
    val activeDateFilter: StateFlow<LocalDate?> = _activeDateFilter.asStateFlow()

    private val _createPostUiState = MutableStateFlow(CreatePostUiState())
    val createPostUiState: StateFlow<CreatePostUiState> = _createPostUiState.asStateFlow()

    private val _createPostEvents = MutableSharedFlow<CreatePostUiEvent>(extraBufferCapacity = 1)
    val createPostEvents: SharedFlow<CreatePostUiEvent> = _createPostEvents.asSharedFlow()

    private val allActivePosts = repository.getAllActive()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activePosts: StateFlow<List<Post>> = combine(allActivePosts, activeDateFilter) { posts, dateFilter ->
        // Filter compares calendar day to keep UX simple for date chips/datepicker.
        if (dateFilter == null) {
            posts
        } else {
            posts.filter { post -> post.createdAt.toLocalDate() == dateFilter }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val archivedPosts: StateFlow<List<Post>> = repository.getAllArchived()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val archivedUiState: StateFlow<ArchivedUiState> = archivedPosts
        .map { posts -> ArchivedUiState(posts = posts) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ArchivedUiState())

    fun createPost(content: String) {
        onCreateContentChanged(content)
        onCreatePostSubmit()
    }

    fun onCreateContentChanged(content: String) {
        val isTooLong = content.length > MAX_POST_LENGTH
        _createPostUiState.update {
            it.copy(
                content = content,
                remainingCharacters = MAX_POST_LENGTH - content.length,
                isValid = content.trim().isNotBlank() && !isTooLong,
                validationError = if (isTooLong) CreatePostValidationError.TOO_LONG else null,
            )
        }
    }

    fun onCreatePostSubmit() {
        if (_createPostUiState.value.isSubmitting) {
            return
        }

        val normalizedContent = _createPostUiState.value.content.trim()
        val validationError = validateNormalizedContent(normalizedContent)
        if (validationError != null) {
            _createPostUiState.update {
                it.copy(
                    isValid = false,
                    validationError = validationError,
                )
            }
            _createPostEvents.tryEmit(CreatePostUiEvent.Error(validationError))
            return
        }

        val post = Post(
            id = UUID.randomUUID().toString(),
            content = normalizedContent,
            createdAt = System.currentTimeMillis(),
            isArchived = false,
        )

        _createPostUiState.update { it.copy(isSubmitting = true, validationError = null) }
        viewModelScope.launch {
            runCatching {
                repository.insert(post)
            }.onSuccess {
                _createPostUiState.value = CreatePostUiState()
                _createPostEvents.emit(CreatePostUiEvent.Success)
            }.onFailure {
                _createPostUiState.update {
                    it.copy(
                        isSubmitting = false,
                        validationError = CreatePostValidationError.PERSISTENCE,
                    )
                }
                _createPostEvents.emit(CreatePostUiEvent.Error(CreatePostValidationError.PERSISTENCE))
            }
        }
    }

    fun clearCreatePostValidationError() {
        _createPostUiState.update {
            it.copy(validationError = null)
        }
    }

    /**
     * Reseta o estado de criação para o valor inicial.
     * Deve ser chamado quando a tela de criação é descartada sem publicar,
     * evitando que estado obsoleto apareça numa próxima visita à tela.
     */
    fun resetCreatePostState() {
        // Não reseta se uma submissão está em andamento para não interromper o fluxo
        if (!_createPostUiState.value.isSubmitting) {
            _createPostUiState.value = CreatePostUiState()
        }
    }

    fun archivePost(id: String) {
        viewModelScope.launch {
            repository.updateArchiveStatus(id = id, isArchived = true)
        }
    }

    fun unarchivePost(id: String) {
        viewModelScope.launch {
            repository.updateArchiveStatus(id = id, isArchived = false)
        }
    }

    fun deletePost(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }


    fun importPostsFromCsv(
        context: Context,
        csvUri: Uri,
        onResult: (Result<CsvImportResult>) -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = runCatching {
                val existingIds = repository
                    .getAllPostsSnapshot()
                    .map { it.id }
                    .toSet()

                val payload = csvRestoreService.prepareImport(
                    context = context,
                    csvUri = csvUri,
                    existingPostIds = existingIds,
                )

                // Duplicatas por ID sao filtradas antes de inserir.
                repository.insertAll(payload.postsToInsert)
                payload.result
            }

            onResult(result)
        }
    }

    fun filterByDate(date: LocalDate?) {
        _activeDateFilter.value = date
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private fun validateNormalizedContent(content: String): CreatePostValidationError? {
        return when {
            content.isBlank() -> CreatePostValidationError.EMPTY
            content.length > MAX_POST_LENGTH -> CreatePostValidationError.TOO_LONG
            else -> null
        }
    }

    companion object {
        const val MAX_POST_LENGTH = 120
    }
}


