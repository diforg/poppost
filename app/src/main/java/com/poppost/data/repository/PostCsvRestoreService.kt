package com.poppost.data.repository

import android.content.Context
import android.net.Uri
import com.poppost.domain.model.Post
import com.poppost.domain.model.normalizeDateStorage
import java.time.LocalDate

/**
 * Resultado da importacao de CSV.
 *
 * @param importedCount quantidade efetivamente inserida no banco
 * @param skippedDuplicates quantidade ignorada por duplicidade
 * @param invalidRows quantidade de linhas invalidas no CSV
 */
data class CsvImportResult(
    val importedCount: Int,
    val skippedDuplicates: Int,
    val invalidRows: Int,
)

data class CsvImportPayload(
    val postsToInsert: List<Post>,
    val result: CsvImportResult,
)

/**
 * Responsavel por restaurar posts a partir de um arquivo CSV.
 * O parser suporta campos com aspas e virgulas escapadas.
 */
class PostCsvRestoreService {

    fun prepareImport(
        context: Context,
        csvUri: Uri,
        existingPostIds: Set<String>,
    ): CsvImportPayload {
        val csvText = context.contentResolver.openInputStream(csvUri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Nao foi possivel abrir o arquivo CSV selecionado.")

        val records = splitCsvRecords(csvText)
        if (records.isEmpty()) {
            return CsvImportPayload(
                postsToInsert = emptyList(),
                result = CsvImportResult(importedCount = 0, skippedDuplicates = 0, invalidRows = 0),
            )
        }

        val startIndex = if (isHeaderRecord(records.first())) 1 else 0

        val seenIds = existingPostIds.toMutableSet()
        val postsToInsert = mutableListOf<Post>()
        var skippedDuplicates = 0
        var invalidRows = 0

        records.drop(startIndex).forEach { record ->
            if (record.isBlank()) return@forEach

            val parsedPost = parsePostRecord(record)
            if (parsedPost == null) {
                invalidRows++
                return@forEach
            }

            if (!seenIds.add(parsedPost.id)) {
                skippedDuplicates++
                return@forEach
            }

            postsToInsert.add(parsedPost)
        }

        return CsvImportPayload(
            postsToInsert = postsToInsert,
            result = CsvImportResult(
                importedCount = postsToInsert.size,
                skippedDuplicates = skippedDuplicates,
                invalidRows = invalidRows,
            ),
        )
    }

    private fun isHeaderRecord(record: String): Boolean {
        val fields = parseCsvRecord(record)
        if (fields.size < 5) return false

        val header = fields.take(5).map { it.trim().lowercase() }
        return header[0] == "id" &&
            header[1] == "content" &&
            header[2] == "createdat" &&
            header[3] == "date" &&
            header[4] == "isarchived"
    }

    private fun parsePostRecord(record: String): Post? {
        val fields = parseCsvRecord(record)
        if (fields.size < 5) return null

        val id = fields[0].trim()
        val content = fields[1]
        val createdAt = fields[2].trim().toLongOrNull()
        val date = parseDateField(fields[3])
        val isArchived = fields[4].trim().toBooleanStrictOrNull()
            ?: when (fields[4].trim()) {
                "1" -> true
                "0" -> false
                else -> null
            }

        if (id.isBlank() || createdAt == null || date == null || isArchived == null) {
            return null
        }

        return Post(
            id = id,
            content = content,
            createdAt = createdAt,
            date = date,
            isArchived = isArchived,
        )
    }

    private fun parseDateField(raw: String): Long? {
        val value = raw.trim()
        val numeric = value.toLongOrNull()
        if (numeric != null) {
            return numeric.normalizeDateStorage()
        }

        return runCatching { LocalDate.parse(value).toEpochDay() }.getOrNull()
    }

    /**
     * Quebra o texto em registros CSV respeitando aspas.
     * Linhas quebradas dentro de campo entre aspas sao mantidas no mesmo registro.
     */
    private fun splitCsvRecords(text: String): List<String> {
        val records = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when (ch) {
                '"' -> {
                    if (inQuotes && i + 1 < text.length && text[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                '\n' -> {
                    if (inQuotes) {
                        current.append(ch)
                    } else {
                        records.add(current.toString())
                        current.setLength(0)
                    }
                }

                '\r' -> {
                    // Ignora CR para suportar finais de linha \r\n.
                }

                else -> current.append(ch)
            }
            i++
        }

        if (current.isNotEmpty()) {
            records.add(current.toString())
        }

        return records
    }

    /**
     * Parseia um unico registro CSV em colunas, respeitando virgulas dentro de aspas.
     */
    private fun parseCsvRecord(record: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < record.length) {
            val ch = record[i]
            when {
                ch == '"' && inQuotes && i + 1 < record.length && record[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }

                ch == '"' -> {
                    inQuotes = !inQuotes
                }

                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.setLength(0)
                }

                else -> current.append(ch)
            }
            i++
        }

        result.add(current.toString())
        return result
    }
}



