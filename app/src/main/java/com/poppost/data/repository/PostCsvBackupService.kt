package com.poppost.data.repository

import android.content.Context
import android.net.Uri
import com.poppost.domain.model.Post
import com.poppost.domain.model.toDateOnlyLocalDate

/**
 * Responsavel por gerar CSV de posts e gravar no URI escolhido pelo usuario via SAF.
 */
class PostCsvBackupService {

    fun exportAllPosts(
        context: Context,
        destinationUri: Uri,
        posts: List<Post>,
    ) {
        val outputStream = context.contentResolver.openOutputStream(destinationUri)
            ?: error("Nao foi possivel abrir o destino selecionado para backup.")

        outputStream.bufferedWriter().use { writer ->
            writer.appendLine(CSV_HEADER)
            posts.forEach { post ->
                writer.appendLine(
                    listOf(
                        post.id,
                        post.content,
                        post.createdAt.toString(),
                        post.date.toDateOnlyLocalDate().toString(),
                        post.isArchived.toString(),
                    ).joinToString(separator = ",") { value -> escapeCsvCell(value) },
                )
            }
        }
    }

    private fun escapeCsvCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    companion object {
        private const val CSV_HEADER = "id,content,createdAt,date,isArchived"
    }
}

