package com.poppost.ui.main

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poppost.ui.components.DateFilterChips
import com.poppost.ui.components.EmptyPostsMessage
import com.poppost.ui.components.PostCard
import com.poppost.viewmodel.PostViewModel

/**
 * Tela principal: lista de posts ativos com filtro por data e FAB de criação.
 *
 * @param viewModel instância compartilhada via [PostViewModelFactory]
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
    val posts by viewModel.activePosts.collectAsState()
    val selectedDate by viewModel.activeDateFilter.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PopPost",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToArchived) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Arquivados",
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
                Icon(imageVector = Icons.Default.Add, contentDescription = "Novo post")
            }
        },
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
                    "Nenhum post nesta data.\nTente outro dia ou remova o filtro."
                else
                    "Nenhum post ainda.\nToque em + para criar o primeiro!"

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
                            // Arquiva ao clicar – feedback via Snackbar chega na Feature 6
                            onClick = { viewModel.archivePost(it.id) },
                        )
                    }
                }
            }
        }
    }
}

