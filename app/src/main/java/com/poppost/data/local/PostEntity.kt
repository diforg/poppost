package com.poppost.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.poppost.domain.model.Post
import com.poppost.domain.model.normalizeDateStorage

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val content: String,
    val createdAt: Long,
    val date: Long,
    val isArchived: Boolean
)

fun PostEntity.toDomain(): Post =
    Post(
        id = id,
        content = content,
        createdAt = createdAt,
        date = date.normalizeDateStorage(),
        isArchived = isArchived
    )

fun Post.toEntity(): PostEntity =
    PostEntity(
        id = id,
        content = content,
        createdAt = createdAt,
        date = date.normalizeDateStorage(),
        isArchived = isArchived
    )

