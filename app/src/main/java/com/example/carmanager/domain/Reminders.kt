package com.example.carmanager.domain

import com.example.carmanager.data.Maintenance
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Urgency { OK, SOON, OVERDUE }

/** Câte zile mai sunt până la expirare și cât de urgent e. */
data class ExpiryStatus(val daysLeft: Long, val urgency: Urgency)

fun expiryStatus(expiry: LocalDate, today: LocalDate = LocalDate.now(), soonDays: Int = 30): ExpiryStatus {
    val days = ChronoUnit.DAYS.between(today, expiry)
    val urgency = when {
        days < 0 -> Urgency.OVERDUE
        days <= soonDays -> Urgency.SOON
        else -> Urgency.OK
    }
    return ExpiryStatus(days, urgency)
}

fun daysLeftLabel(days: Long): String = when {
    days < -1 -> "expirat de ${-days} zile"
    days == -1L -> "expirat ieri"
    days == 0L -> "expiră azi"
    days == 1L -> "expiră mâine"
    else -> "mai sunt $days zile"
}

/**
 * Interval recomandat pentru o intervenție. Backend-ul nu știe de intervale, așa că
 * le calculăm în aplicație pe baza titlului ultimei intervenții de acel tip.
 */
data class MaintenanceRule(
    val name: String,
    val keywords: List<String>,
    val everyKm: Int?,
    val everyMonths: Long?,
    val excludes: List<String> = emptyList(),
)

val DEFAULT_RULES = listOf(
    MaintenanceRule("Schimb ulei", listOf("ulei"), everyKm = 15_000, everyMonths = 12),
    MaintenanceRule("Revizie", listOf("revizie"), everyKm = 15_000, everyMonths = 12),
    MaintenanceRule("Plăcuțe frână", listOf("placute", "frana"), everyKm = 30_000, everyMonths = null, excludes = listOf("lichid")),
    MaintenanceRule("Lichid frână", listOf("lichid frana"), everyKm = null, everyMonths = 24),
    MaintenanceRule("Kit distribuție", listOf("distributie"), everyKm = 90_000, everyMonths = 60),
    MaintenanceRule("Antigel", listOf("antigel"), everyKm = null, everyMonths = 36),
)

data class UpcomingMaintenance(
    val rule: MaintenanceRule,
    val last: Maintenance,
    val dueKm: Int?,
    val dueDate: LocalDate?,
    val kmLeft: Int?,
    val daysLeft: Long?,
    val urgency: Urgency,
)

/** „Plăcuțe Frână” -> „placute frana”, ca potrivirea să nu depindă de diacritice/majuscule. */
internal fun normalize(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

private fun MaintenanceRule.matches(title: String): Boolean {
    val t = normalize(title)
    return keywords.any { it in t } && excludes.none { it in t }
}

fun upcomingMaintenance(
    records: List<Maintenance>,
    currentMileage: Int,
    today: LocalDate = LocalDate.now(),
    rules: List<MaintenanceRule> = DEFAULT_RULES,
    soonKm: Int = 1_000,
    soonDays: Int = 30,
): List<UpcomingMaintenance> = rules.mapNotNull { rule ->
    val last = records
        .filter { rule.matches(it.title) }
        .maxWithOrNull(compareBy<Maintenance>({ it.localDate }, { it.mileage }))
        ?: return@mapNotNull null
    val dueKm = rule.everyKm?.let { last.mileage + it }
    val dueDate = rule.everyMonths?.let { last.localDate.plusMonths(it) }
    val kmLeft = dueKm?.let { it - currentMileage }
    val daysLeft = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
    val urgency = when {
        (kmLeft != null && kmLeft < 0) || (daysLeft != null && daysLeft < 0) -> Urgency.OVERDUE
        (kmLeft != null && kmLeft <= soonKm) || (daysLeft != null && daysLeft <= soonDays) -> Urgency.SOON
        else -> Urgency.OK
    }
    UpcomingMaintenance(rule, last, dueKm, dueDate, kmLeft, daysLeft, urgency)
}.sortedByDescending { it.urgency.ordinal }
