package com.poppost.domain.model

/**
 * Canonical domain model used by upper layers (ViewModel/UI).
 */
data class Post(
    val id: String,
    val content: String,
    val createdAt: Long,
    val isArchived: Boolean
)

