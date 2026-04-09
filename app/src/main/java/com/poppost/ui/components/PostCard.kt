package com.poppost.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.poppost.R
import com.poppost.domain.model.Post
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter
    .ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())

/**
 * Card reutilizável que exibe conteúdo e data formatada de um [Post].
 * Recebe [onClick] genérico – quem chama decide a ação (arquivar, desarquivar, etc.).
 */
@Composable
fun PostCard(
    post: Post,
    onClick: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val formattedDate = Instant
        .ofEpochMilli(post.createdAt)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            onClickLabel = stringResource(id = R.string.cd_archive_post),
        ) { onClick(post) }
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

