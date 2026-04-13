package com.poppost.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.poppost.R
import com.poppost.domain.model.Post
import com.poppost.domain.model.toDateOnlyLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy", Locale.getDefault())

/**
 * Card reutilizável que exibe conteúdo e data formatada de um [Post].
 *
 * @param onClick  ação executada no toque simples (opcional — ex.: arquivar).
 * @param onLongClick  ação executada no toque longo (opcional — ex.: abrir menu de opções).
 *
 * Utiliza [combinedClickable] para suportar click e long-press simultaneamente.
 * Quando nenhum dos dois é fornecido, o card fica não-interativo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    modifier: Modifier = Modifier,
    onClick: ((Post) -> Unit)? = null,
    onLongClick: ((Post) -> Unit)? = null,
) {
    val formattedDate = post.date
        .toDateOnlyLocalDate()
        .format(dateFormatter)

    val interactionModifier = if (onClick != null || onLongClick != null) {
        val longPressLabel = stringResource(R.string.cd_long_press_archived_post)
        val clickLabel = stringResource(R.string.cd_archive_post)
        Modifier.combinedClickable(
            onClickLabel = if (onClick != null) clickLabel else null,
            onLongClickLabel = if (onLongClick != null) longPressLabel else null,
            onClick = { onClick?.invoke(post) },
            onLongClick = { onLongClick?.invoke(post) },
        )
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(interactionModifier),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD2946E),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF4A4543),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4A4543),
            )
        }
    }
}
