package com.poppost.data.repository

import com.poppost.data.local.PostDao
import com.poppost.data.local.toDomain
import com.poppost.data.local.toEntity
import com.poppost.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PostRepository(
    private val postDao: PostDao
) {
    fun getAllActive(): Flow<List<Post>> =
        postDao.getAllActive().map { entities -> entities.map { it.toDomain() } }

    fun getAllArchived(): Flow<List<Post>> =
        postDao.getAllArchived().map { entities ->
            entities
                .map { it.toDomain() }
                .filter { it.isArchived }
        }

    suspend fun insert(post: Post) {
        postDao.insert(post.toEntity())
    }

    suspend fun updateArchiveStatus(id: String, isArchived: Boolean): Int {
        return postDao.updateArchiveStatus(id = id, isArchived = isArchived)
    }
}

