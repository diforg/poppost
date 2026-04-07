package com.poppost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poppost.data.repository.PostRepository
import com.poppost.domain.model.Post
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostViewModel(
    private val repository: PostRepository
) : ViewModel() {

    private val _activeDateFilter = MutableStateFlow<LocalDate?>(null)
    val activeDateFilter: StateFlow<LocalDate?> = _activeDateFilter.asStateFlow()

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

    fun createPost(content: String) {
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank() || normalizedContent.length > MAX_POST_LENGTH) {
            return
        }

        val post = Post(
            id = UUID.randomUUID().toString(),
            content = normalizedContent,
            createdAt = System.currentTimeMillis(),
            isArchived = false
        )

        viewModelScope.launch {
            repository.insert(post)
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

    fun filterByDate(date: LocalDate?) {
        _activeDateFilter.value = date
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    companion object {
        const val MAX_POST_LENGTH = 120
    }
}


