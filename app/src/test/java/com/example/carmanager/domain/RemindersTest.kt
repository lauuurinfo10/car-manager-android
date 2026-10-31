package com.example.carmanager.domain

import com.example.carmanager.data.Maintenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RemindersTest {
    private val today = LocalDate.of(2026, 10, 31)
    private fun m(id: Int, title: String, date: String, km: Int) = Maintenance(id, 1, title, date, km)

    @Test fun expiryStatusBuckets() {
        assertEquals(Urgency.OVERDUE, expiryStatus(today.minusDays(1), today).urgency)
        assertEquals(Urgency.SOON, expiryStatus(today, today).urgency)
        assertEquals(Urgency.SOON, expiryStatus(today.plusDays(30), today).urgency)
        assertEquals(Urgency.OK, expiryStatus(today.plusDays(31), today).urgency)
        assertEquals(12L, expiryStatus(today.plusDays(12), today).daysLeft)
    }

    @Test fun oilChangeDueByKmUsesLatestRecord() {
        val records = listOf(
            m(1, "Schimb ulei", "2025-01-10", 100_000),
            m(2, "schimb ULEI + filtre", "2026-06-01", 114_500),
        )
        val oil = upcomingMaintenance(records, currentMileage = 129_000, today = today).single { it.rule.name == "Schimb ulei" }
        assertEquals(2, oil.last.id)
        assertEquals(129_500, oil.dueKm)
        assertEquals(500, oil.kmLeft)
        assertEquals(LocalDate.of(2027, 6, 1), oil.dueDate)
        assertEquals(Urgency.SOON, oil.urgency)
    }

    @Test fun overdueByDateEvenIfKmOk() {
        val records = listOf(m(1, "Revizie", "2025-09-01", 50_000))
        val rev = upcomingMaintenance(records, currentMileage = 52_000, today = today).single()
        assertEquals(Urgency.OVERDUE, rev.urgency)
    }

    @Test fun diacriticsAndBrakeFluidNotConfusedWithPads() {
        val records = listOf(m(1, "Lichid frână", "2026-01-01", 80_000), m(2, "Plăcuțe frână față", "2026-02-01", 81_000))
        val names = upcomingMaintenance(records, 81_000, today).associate { it.rule.name to it.last.id }
        assertEquals(1, names["Lichid frână"])
        assertEquals(2, names["Plăcuțe frână"])
    }

    @Test fun unknownTitlesIgnoredAndSortedByUrgency() {
        val records = listOf(
            m(1, "Spălătorie", "2026-10-01", 10_000),
            m(2, "Antigel", "2026-01-01", 10_000),
            m(3, "Schimb ulei", "2025-01-01", 1_000),
        )
        val list = upcomingMaintenance(records, 20_000, today)
        assertEquals(listOf("Schimb ulei", "Antigel"), list.map { it.rule.name })
        assertTrue(list.first().urgency == Urgency.OVERDUE)
    }
}
