package com.poppost.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DatePickerBar(
    modifier: Modifier = Modifier,
    startDate: LocalDate = LocalDate.now().minusDays(30),
    endDate: LocalDate = LocalDate.now().plusDays(60),
    selectedDate: LocalDate? = null,
    locale: Locale = Locale("pt", "BR"),
    onDateSelected: (LocalDate?) -> Unit = {},
) {
    val dates = remember(startDate, endDate) {
        generateSequence(startDate) { date ->
            date.plusDays(1).takeIf { it <= endDate }
        }.toList()
    }

    val selectedDateInRange = selectedDate?.takeIf { it in startDate..endDate }
    val fallbackDate = LocalDate.now().takeIf { it in startDate..endDate } ?: startDate
    val effectiveSelectedDate = selectedDateInRange ?: fallbackDate

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val effectiveIndex = remember(dates, effectiveSelectedDate) {
        dates.indexOfFirst { it == effectiveSelectedDate }.coerceAtLeast(0)
    }

    // Centraliza no dia efetivo apenas na primeira composição
    LaunchedEffect(Unit) {
        listState.scrollToItem(index = (effectiveIndex - 3).coerceAtLeast(0))
    }

    fun LazyListState.centerOnIndex(index: Int, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            val layoutInfo = layoutInfo
            val viewportWidth = layoutInfo.viewportSize.width
            val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

            if (itemInfo != null) {
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val viewportCenter = viewportWidth / 2
                val scrollDelta = itemCenter - viewportCenter
                animateScrollBy(scrollDelta.toFloat())
            } else {
                val avgItemSize = layoutInfo.visibleItemsInfo
                    .takeIf { it.isNotEmpty() }
                    ?.let { items ->
                        items.sumOf { it.size } / items.size +
                                layoutInfo.mainAxisItemSpacing
                    } ?: 40

                val estimatedOffset = index * avgItemSize - viewportWidth / 2 + avgItemSize / 2
                animateScrollBy((estimatedOffset - firstVisibleItemScrollOffset).toFloat())
            }
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        itemsIndexed(dates) { index, date ->
            DateCard(
                date = date,
                isSelected = date == effectiveSelectedDate, // usa effectiveSelectedDate em vez de selectedDateInRange
                locale = locale,
                onClick = {
                    onDateSelected(if (date == effectiveSelectedDate) null else date)
                    val isVisible = listState.layoutInfo.visibleItemsInfo
                        .any { it.index == index }
                    if (!isVisible) {
                        listState.centerOnIndex(index, coroutineScope)
                    }
                },
            )
        }
    }
}

@Composable
fun DateCard(
    date: LocalDate,
    isSelected: Boolean,
    locale: Locale,
    onClick: () -> Unit
) {
    val selectedCardBackground = Color(0xFFFF8543)
    val selectedCardDateBackground = Color(0xFFC76C32)
    val unselectedCardBackground = Color(0xFFD3A892)
    val unselectedCardDateBackground = Color(0xFFD2946E)
    val selectedTextColor = Color(0xFFD9D9D9)
    val unselectedTextColor = Color(0xFF4A4543)

    val textColor = if (isSelected) selectedTextColor else unselectedTextColor
    val cardBackground = if (isSelected) selectedCardBackground else unselectedCardBackground
    val cardDateBackground = if (isSelected) selectedCardDateBackground else unselectedCardDateBackground

    val dayOfWeek = date.dayOfWeek
        .getDisplayName(TextStyle.SHORT, locale)
        .replace(".", "")
        .uppercase()

    Column(
        modifier = Modifier
            .width(40.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .background(cardBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayOfWeek,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = textColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .background(cardDateBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("dd/MM")),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}