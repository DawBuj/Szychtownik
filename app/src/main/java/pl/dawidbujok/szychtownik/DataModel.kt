package pl.dawidbujok.szychtownik

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

// --- MODELE DANYCH --- 

/**
 * Reprezentuje niestandardowy typ dnia zdefiniowany przez użytkownika (np. Urlop, L4).
 */
@Serializable
data class CustomDayType(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val description: String,
    @Serializable(with = ColorSerializer::class)
    val color: Color,
    val hasReminder: Boolean = false,
    val yearlyLimit: Int = 0
)

/**
 * Statystyki zużycia dla konkretnego typu dnia w roku.
 */
data class VacationStat(
    val dayType: CustomDayType,
    val used: Int,      // Dni, które już minęły
    val planned: Int,   // Dni zaplanowane w przyszłości
    val limit: Int
) {
    val totalBooked: Int = used + planned
    val remaining: Int? = if (limit > 0) limit - totalBooked else null
}

@Serializable
data class Note(
    val id: Int,
    val content: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val reminderDateTime: LocalDateTime? = null
)

@Serializable
data class SpecialDayInstance(
    val typeId: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val reminderDateTime: LocalDateTime? = null
)
