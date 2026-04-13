package com.poppost.domain.model

import java.time.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L
private const val LEGACY_MILLIS_THRESHOLD = 100_000_000_000L

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

fun Long.normalizeDateStorage(): Long {
    return if (this > LEGACY_MILLIS_THRESHOLD) Math.floorDiv(this, MILLIS_PER_DAY) else this
}

fun Long.toDateOnlyLocalDate(): LocalDate = LocalDate.ofEpochDay(normalizeDateStorage())

fun Long.epochDayToUtcMillis(): Long = normalizeDateStorage() * MILLIS_PER_DAY

fun Long.utcMillisToEpochDay(): Long = Math.floorDiv(this, MILLIS_PER_DAY)

