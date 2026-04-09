package com.poppost.ui.archived

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.poppost.R
import com.poppost.domain.model.Post

/**
 * Bottom sheet exibido após long press em um post arquivado.
 * Apresenta as opções [Desarquivar] e [Excluir].
 *
 * Os callbacks [onUnarchiveClick] e [onDeleteClick] são tratados externamente
 * (commits 3 e 4 respectivamente) — aqui apenas a UI é declarada.
 *
 * @param post post que recebeu o long press
 * @param onDismiss chamado quando o sheet é fechado sem ação
 * @param onUnarchiveClick chamado quando o usuário escolhe "Desarquivar"
 * @param onDeleteClick chamado quando o usuário escolhe "Excluir"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedPostActionsSheet(
    post: Post,
    onDismiss: () -> Unit,
    onUnarchiveClick: (Post) -> Unit,
    onDeleteClick: (Post) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            // Título com trecho do conteúdo do post
            Text(
                text = stringResource(R.string.archived_options_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            // Opção: Desarquivar
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.archived_action_unarchive),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Unarchive,
                        contentDescription = stringResource(R.string.cd_action_unarchive),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button }
                    .clickable { onUnarchiveClick(post) },
            )

            // Opção: Excluir (destrutiva — em vermelho)
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.archived_action_delete),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_action_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button }
                    .clickable { onDeleteClick(post) },
            )
        }
    }
}

