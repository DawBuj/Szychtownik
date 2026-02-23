package pl.dawidbujok.szychtownik

import androidx.compose.ui.graphics.Color

/**
 * Klasa danych przechowująca wszystkie informacje potrzebne do poprawnego wyświetlenia
 * pojedynczej komórki dnia w kalendarzu.
 *
 * @param type Typ zmiany (np. pierwsza, druga, wolne).
 * @param shiftNumberText Tekst do wyświetlenia w tle komórki (np. "1", "2", "U").
 * @param backgroundColor Kolor tła komórki.
 * @param isCurrentDay Czy dany dzień jest dniem dzisiejszym.
 * @param isFromCurrentMonth Czy dany dzień należy do aktualnie wyświetlanego miesiąca.
 * @param customDayType Niestandardowy typ dnia (np. urlop), jeśli jest ustawiony.
 * @param hasNote Czy dla tego dnia istnieje notatka.
 * @param hasReminder Czy dla tego dnia ustawiono jakiekolwiek przypomnienie.
 */
data class DayDisplayInfo(
    val type: ShiftType,
    val shiftNumberText: String,
    val backgroundColor: Color,
    val isCurrentDay: Boolean,
    val isFromCurrentMonth: Boolean,
    val customDayType: CustomDayType? = null,
    val hasNote: Boolean = false,
    val hasReminder: Boolean = false
)
