package com.poppost.ui.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.poppost.R
import com.poppost.viewmodel.PostViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela de criação de post.
 * Observa [PostViewModel.createPostUiState] para estado reativo do formulário
 * e coleta [PostViewModel.createPostEvents] como eventos one-shot para navegar
 * após sucesso ou exibir erro de persistência.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    viewModel: PostViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.createPostUiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    val formattedSelectedDate = remember(selectedDate) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    val errorEmpty = stringResource(R.string.create_post_error_empty)
    val errorTooLong = stringResource(R.string.create_post_error_too_long)
    val errorPersistence = stringResource(R.string.create_post_error_persistence)

    // Bloqueia o gesto/botão de voltar durante o envio para evitar navegação
    // enquanto o Room ainda está escrevendo. Fora do isSubmitting, navega normalmente.
    BackHandler(enabled = uiState.isSubmitting) { /* absorve o gesto; aguarda o submit terminar */ }

    // Reseta o estado de criação ao sair da tela sem publicar, para que a próxima
    // visita comece com campo vazio (sem recomposição de estado obsoleto).
    DisposableEffect(Unit) {
        onDispose { viewModel.resetCreatePostState() }
    }

    // Consome eventos one-shot: navega em sucesso ou exibe snackbar em erro
    LaunchedEffect(Unit) {
        viewModel.createPostEvents.collect { event ->
            when (event) {
                is PostViewModel.CreatePostUiEvent.Success -> onNavigateBack()
                is PostViewModel.CreatePostUiEvent.Error -> {
                    val message = when (event.reason) {
                        PostViewModel.CreatePostValidationError.EMPTY -> errorEmpty
                        PostViewModel.CreatePostValidationError.TOO_LONG -> errorTooLong
                        PostViewModel.CreatePostValidationError.PERSISTENCE -> errorPersistence
                    }
                    snackbarHostState.showSnackbar(message)
                    viewModel.clearCreatePostValidationError()
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDateSelected(datePickerState.selectedDateMillis ?: selectedDate)
                        showDatePicker = false
                    }
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.create_post_title)) },
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.End,
        ) {
            val fieldDescription = stringResource(R.string.cd_create_post_field)

            // isError só ativa borda vermelha quando há conteúdo — evita indicar erro
            // antes do usuário interagir com o campo (ex: campo vazio ao abrir a tela).
            val showInlineError = uiState.validationError != null && uiState.content.isNotEmpty()

            OutlinedTextField(
                value = uiState.content,
                onValueChange = { viewModel.onCreateContentChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .semantics { contentDescription = fieldDescription },
                // label fixo: sobe para cima do campo quando o usuário digita (M3)
                label = { Text(stringResource(R.string.create_post_label)) },
                placeholder = { Text(stringResource(R.string.create_post_hint)) },
                isError = showInlineError,
                supportingText = {
                    if (showInlineError) {
                        val message = when (uiState.validationError) {
                            PostViewModel.CreatePostValidationError.EMPTY -> errorEmpty
                            PostViewModel.CreatePostValidationError.TOO_LONG -> errorTooLong
                            PostViewModel.CreatePostValidationError.PERSISTENCE -> errorPersistence
                            null -> ""
                        }
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                enabled = !uiState.isSubmitting,
                maxLines = 6,
                textStyle = MaterialTheme.typography.bodyLarge,
                // Permite publicar direto pelo botão "Done" do teclado virtual
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (uiState.isValid) viewModel.onCreatePostSubmit() }
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Contador regressivo com semântica para leitores de tela
            val charsRemaining = uiState.remainingCharacters
            val counterDescription = if (charsRemaining >= 0) {
                stringResource(R.string.cd_chars_remaining, charsRemaining)
            } else {
                stringResource(R.string.cd_chars_over_limit, -charsRemaining)
            }
            Text(
                text = "$charsRemaining",
                style = MaterialTheme.typography.labelSmall,
                color = if (charsRemaining < 0)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { contentDescription = counterDescription },
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { showDatePicker = true },
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = formattedSelectedDate)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val publishButtonDescription = stringResource(R.string.cd_publish_button)
            Button(
                onClick = { viewModel.onCreatePostSubmit() },
                enabled = uiState.isValid && !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = publishButtonDescription },
            ) {
                if (uiState.isSubmitting) {
                    // Exibe indicador de carregamento enquanto o Room persiste o post
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(text = stringResource(R.string.create_post_publishing))
                    }
                } else {
                    Text(text = stringResource(R.string.create_post_publish))
                }
            }
        }
    }
}

