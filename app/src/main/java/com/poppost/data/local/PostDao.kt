package com.poppost.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("SELECT * FROM posts WHERE isArchived = 0 ORDER BY date DESC")
    fun getAllActive(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE isArchived != 0 ORDER BY date DESC")
    fun getAllArchived(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY date DESC")
    suspend fun getAllPostsSnapshot(): List<PostEntity>

    @Query("UPDATE posts SET isArchived = :isArchived WHERE id = :id")
    suspend fun updateArchiveStatus(id: String, isArchived: Boolean): Int

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: String): Int
}

