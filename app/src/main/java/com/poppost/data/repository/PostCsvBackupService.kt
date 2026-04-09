package com.poppost.data.repository

import android.content.Context
import android.os.Environment
import com.poppost.domain.model.Post
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Responsavel por gerar arquivo CSV com todos os posts do app.
 * O arquivo e salvo em armazenamento externo privado do app, sem permissao extra.
 */
class PostCsvBackupService {

    fun exportAllPosts(context: Context, posts: List<Post>): File {
        val backupsDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            BACKUP_FOLDER_NAME,
        )

        if (!backupsDir.exists()) {
            backupsDir.mkdirs()
        }

        val fileName = "poppost-backup-${currentTimestampForFileName()}.csv"
        val outputFile = File(backupsDir, fileName)

        outputFile.bufferedWriter().use { writer ->
            writer.appendLine(CSV_HEADER)
            posts.forEach { post ->
                writer.appendLine(
                    listOf(
                        post.id,
                        post.content,
                        post.createdAt.toString(),
                        post.isArchived.toString(),
                    ).joinToString(separator = ",") { value -> escapeCsvCell(value) },
                )
            }
        }

        return outputFile
    }

    private fun currentTimestampForFileName(): String {
        return LocalDateTime.now().format(FILE_DATE_FORMATTER)
    }

    private fun escapeCsvCell(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    companion object {
        private const val BACKUP_FOLDER_NAME = "poppost-backups"
        private const val CSV_HEADER = "id,content,createdAt,isArchived"
        private val FILE_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}

