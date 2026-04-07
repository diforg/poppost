package com.poppost.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chipFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

/**
 * Linha horizontal de chips para os últimos [dayCount] dias.
 * Selecionar um dia ativa o filtro; selecionar novamente (ou "Todos") limpa.
 */
@Composable
fun DateFilterChips(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    dayCount: Int = 7,
) {
    val today = LocalDate.now()
    val dates = (0 until dayCount).map { today.minusDays(it.toLong()) }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Chip "Todos" – remove filtro
        FilterChip(
            selected = selectedDate == null,
            onClick = { onDateSelected(null) },
            label = { Text("Todos") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )

        dates.forEach { date ->
            val label = if (date == today) "Hoje" else date.format(chipFormatter)
            FilterChip(
                selected = selectedDate == date,
                onClick = {
                    onDateSelected(if (selectedDate == date) null else date)
                },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

