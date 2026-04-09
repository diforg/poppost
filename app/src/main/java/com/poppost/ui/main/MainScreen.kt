package com.poppost.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.poppost.R
import com.poppost.data.repository.CsvImportResult
import com.poppost.ui.components.DateFilterChips
import com.poppost.ui.components.EmptyPostsMessage
import com.poppost.ui.components.PostCard
import com.poppost.viewmodel.PostViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource


/**
 * Tela principal: lista de posts ativos com filtro por data e FAB de criação.
 *
 * @param viewModel instância compartilhada pelo NavHost
 * @param onNavigateToCreate chamado quando o FAB é pressionado
 * @param onNavigateToArchived chamado quando o ícone de arquivados é pressionado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PostViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToArchived: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val posts by viewModel.activePosts.collectAsState()
    val selectedDate by viewModel.activeDateFilter.collectAsState()
    val archivedSnackbarMessage = stringResource(id = R.string.snackbar_post_archived)
    val backupErrorMessage = stringResource(id = R.string.backup_error)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isMainMenuExpanded by remember { mutableStateOf(false) }

    val csvRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            importCsv(
                context = context,
                viewModel = viewModel,
                uri = uri,
                onFeedbackMessage = { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message = message)
                    }
                },
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.mipmap.ic_launcher_round),
                            contentDescription = stringResource(id = R.string.cd_app_topbar_icon),
                            modifier = Modifier.size(24.dp),
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToArchived) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = stringResource(id = R.string.cd_open_archived),
                        )
                    }

                    IconButton(onClick = { isMainMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(id = R.string.cd_open_main_menu),
                        )
                    }

                    DropdownMenu(
                        expanded = isMainMenuExpanded,
                        onDismissRequest = { isMainMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.menu_backup)) },
                            onClick = {
                                isMainMenuExpanded = false
                                viewModel.exportPostsToCsv(context = context) { result ->
                                    val message = result.fold(
                                        onSuccess = { file ->
                                            context.getString(R.string.backup_success, file.absolutePath)
                                        },
                                        onFailure = {
                                            backupErrorMessage
                                        },
                                    )
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message = message)
                                    }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(text = stringResource(id = R.string.menu_restore)) },
                            onClick = {
                                isMainMenuExpanded = false
                                csvRestoreLauncher.launch(arrayOf("text/csv", "text/comma-separated-values"))
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.cd_create_post),
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Chips de filtro por data – últimos 7 dias
            DateFilterChips(
                selectedDate = selectedDate,
                onDateSelected = viewModel::filterByDate,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )

            if (posts.isEmpty()) {
                val emptyMessage = if (selectedDate != null)
                    stringResource(id = R.string.empty_posts_filtered)
                else
                    stringResource(id = R.string.empty_posts_default)

                EmptyPostsMessage(
                    message = emptyMessage,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onClick = {
                                viewModel.archivePost(it.id)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message = archivedSnackbarMessage)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun importCsv(
    context: android.content.Context,
    viewModel: PostViewModel,
    uri: Uri,
    onFeedbackMessage: (String) -> Unit,
) {
    viewModel.importPostsFromCsv(
        context = context,
        csvUri = uri,
        onResult = { result ->
            val message = result.fold(
                onSuccess = { importResult: CsvImportResult ->
                    context.getString(
                        R.string.restore_success,
                        importResult.importedCount,
                        importResult.skippedDuplicates,
                        importResult.invalidRows,
                    )
                },
                onFailure = {
                    context.getString(R.string.restore_error)
                },
            )
            onFeedbackMessage(message)
        },
    )
}
