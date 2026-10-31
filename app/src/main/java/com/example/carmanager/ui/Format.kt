package com.example.carmanager.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RO = Locale.forLanguageTag("ro-RO")
private val DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun LocalDate.display(): String = format(DATE_FMT)
fun String.displayDate(): String = runCatching { LocalDate.parse(this).display() }.getOrDefault(this)

fun Int.km(): String = NumberFormat.getIntegerInstance(RO).format(this) + " km"

fun Double.lei(): String = NumberFormat.getNumberInstance(RO).apply {
    // 1250 -> „1.250 lei”, 1250.5 -> „1.250,50 lei”
    minimumFractionDigits = if (this@lei % 1.0 == 0.0) 0 else 2
    maximumFractionDigits = 2
}.format(this) + " lei"

// DatePicker din Material3 lucrează cu milisecunde UTC.
fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun Long.utcMillisToLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
