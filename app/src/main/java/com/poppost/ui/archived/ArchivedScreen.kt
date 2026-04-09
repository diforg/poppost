package com.poppost.ui.archived

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.poppost.R
import com.poppost.domain.model.Post
import com.poppost.ui.components.EmptyPostsMessage
import com.poppost.ui.components.PostCard
import com.poppost.viewmodel.PostViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(
    viewModel: PostViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val archivedUiState by viewModel.archivedUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val unarchivedMessage = stringResource(id = R.string.snackbar_post_unarchived)
    val deletedMessage = stringResource(id = R.string.snackbar_post_deleted)

    // Post selecionado via long press; nulo quando o sheet está fechado.
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var postPendingDeletion by remember { mutableStateOf<Post?>(null) }

    // Exibe o bottom sheet de opções quando um post é selecionado via long press.
    // onUnarchiveClick e onDeleteClick serão implementados nos commits 3 e 4.
    selectedPost?.let { post ->
        ArchivedPostActionsSheet(
            post = post,
            onDismiss = { selectedPost = null },
            onUnarchiveClick = {
                viewModel.unarchivePost(it.id)
                selectedPost = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = unarchivedMessage)
                }
            },
            onDeleteClick = {
                selectedPost = null
                postPendingDeletion = it
            },
        )
    }

    postPendingDeletion?.let { post ->
        AlertDialog(
            onDismissRequest = { postPendingDeletion = null },
            title = { Text(text = stringResource(id = R.string.archived_delete_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.archived_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePost(post.id)
                        postPendingDeletion = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message = deletedMessage)
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.archived_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { postPendingDeletion = null }) {
                    Text(text = stringResource(id = R.string.archived_delete_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.archived_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (archivedUiState.isEmpty) {
            EmptyPostsMessage(
                message = stringResource(id = R.string.empty_archived_posts),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(archivedUiState.posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        // Long press seleciona o post; commit 2 exibirá as opções.
                        onLongClick = { selectedPost = it },
                    )
                }
            }
        }
    }
}
