package pl.dawidbujok.szychtownik

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.temporal.ChronoUnit

//================================================================================
// ARCHITEKTURA MODELI DANYCH SYSTEMU ZMIANOWEGO
//================================================================================

/**
 * Interfejs definiujący podstawowe operacje dla dowolnego systemu zmianowego.
 * Każdy system musi określić długość cyklu i potrafić obliczyć zmianę dla danej daty.
 */
interface IShiftSystem {
    /** Całkowita liczba dni w jednym pełnym cyklu zmianowym. */
    val totalCycleDays: Int
    /**
     * Oblicza i zwraca typ zmiany dla podanej daty, uwzględniając datę początkową cyklu brygady.
     * @param date Data, dla której ma być obliczona zmiana.
     * @param brigadeStartDate Data, od której rozpoczyna się cykl zmianowy dla danej brygady.
     * @return [ShiftType] reprezentujący zmianę w danym dniu.
     */
    fun getShiftTypeForDate(date: LocalDate, brigadeStartDate: LocalDate): ShiftType
}

/**
 * Model danych reprezentujący firmę.
 * @property id Unikalny identyfikator firmy.
 * @property displayName Nazwa firmy wyświetlana w interfejsie użytkownika.
 * @property shiftSystem Implementacja systemu zmianowego ([IShiftSystem]) używanego w tej firmie.
 * @property brigades Lista brygad ([BrigadeConfig]) działających w tej firmie.
 */
data class Company(
    val id: String,
    val displayName: String,
    val shiftSystem: IShiftSystem,
    val brigades: List<BrigadeConfig>
)

/**
 * Model danych reprezentujący konfigurację brygady.
 * @property id Unikalny identyfikator brygady.
 * @property displayName Nazwa brygady wyświetlana w interfejsie użytkownika.
 * @property baseShiftCycleStartDate Bazowa (domyślna) data rozpoczęcia cyklu zmianowego dla tej brygady.
 */
data class BrigadeConfig(
    val id: String,
    val displayName: String,
    val baseShiftCycleStartDate: LocalDate
)

//================================================================================
// IMPLEMENTACJA ELASTYCZNEGO SYSTEMU ZMIANOWEGO
//================================================================================

/**
 * Elastyczna implementacja interfejsu [IShiftSystem], która oblicza zmiany na podstawie mapy wzorców.
 * @param shiftPattern Mapa, gdzie kluczem jest zakres dni w cyklu, a wartością jest typ zmiany.
 */
class FlexibleShiftSystem(private val shiftPattern: Map<IntRange, ShiftType>) : IShiftSystem {
    // Długość cyklu jest stała i wynosi 16 dni dla systemów zdefiniowanych w tej aplikacji.
    override val totalCycleDays: Int = 16

    override fun getShiftTypeForDate(date: LocalDate, brigadeStartDate: LocalDate): ShiftType {
        // Oblicz liczbę dni, które upłynęły od daty początkowej brygady.
        val daysSinceStart = ChronoUnit.DAYS.between(brigadeStartDate, date)
        // Oblicz, który to dzień cyklu, z uwzględnieniem wartości ujemnych.
        val dayInCycle = (daysSinceStart % totalCycleDays).toInt().let {
            if (it < 0) it + totalCycleDays else it
        }

        // Przeszukaj mapę wzorców, aby znaleźć zmianę dla obliczonego dnia cyklu.
        for ((range, shiftType) in shiftPattern) {
            if (dayInCycle in range) {
                return shiftType
            }
        }
        // Jeśli nie znaleziono dopasowania, zwróć domyślnie dzień wolny.
        return ShiftType.DAY_OFF
    }
}


//================================================================================
// DEFINICJA WZORCÓW ZMIAN, FIRM I BRYGAD
//================================================================================

// Wzorzec zmian dla firmy Teksid: 4 dni pracy, 1 dzień wolny, 4 dni pracy, 1 dzień wolny, 4 dni pracy, 2 dni wolne.
val teksidShiftPattern = mapOf(
    0..3 to ShiftType.SHIFT_1,
    4..4 to ShiftType.DAY_OFF,
    5..8 to ShiftType.SHIFT_2,
    9..9 to ShiftType.DAY_OFF,
    10..13 to ShiftType.SHIFT_3,
    14..15 to ShiftType.DAY_OFF
)

// Wzorzec zmian dla firmy Kuźnia: 4 dni pracy, 2 dni wolne, 4 dni pracy, 1 dzień wolny, 4 dni pracy, 1 dzień wolny.
val kuzniaShiftPattern = mapOf(
    0..3 to ShiftType.SHIFT_3,
    4..5 to ShiftType.DAY_OFF,
    6..9 to ShiftType.SHIFT_2,
    10..10 to ShiftType.DAY_OFF,
    11..14 to ShiftType.SHIFT_1,
    15..15 to ShiftType.DAY_OFF
)

// Definicja obiektu firmy Teksid wraz z jej brygadami i ich datami startowymi.
val teksidCompany = Company(
    id = "teksid",
    displayName = "Teksid",
    shiftSystem = FlexibleShiftSystem(teksidShiftPattern),
    brigades = listOf(
        BrigadeConfig("teksid_a", "Brygada A", LocalDate.of(2020, 1, 3)),
        BrigadeConfig("teksid_b", "Brygada B", LocalDate.of(2020, 1, 3).plusDays(4)),
        BrigadeConfig("teksid_c", "Brygada C", LocalDate.of(2020, 1, 3).plusDays(12)),
        BrigadeConfig("teksid_d", "Brygada D", LocalDate.of(2020, 1, 3).plusDays(8))
    )
)

// Data startowa dla brygady A w Kuźni.
val kuzniaStartDateForBrigadeA = LocalDate.of(2019, 12, 31)
// Definicja obiektu firmy Kuźnia wraz z jej brygadami i ich datami startowymi.
val kuzniaCompany = Company(
    id = "kuznia",
    displayName = "Kuźnia",
    shiftSystem = FlexibleShiftSystem(kuzniaShiftPattern),
    brigades = listOf(
        BrigadeConfig("kuznia_a", "Brygada A", kuzniaStartDateForBrigadeA),
        BrigadeConfig("kuznia_b", "Brygada B", kuzniaStartDateForBrigadeA.plusDays(4)),
        BrigadeConfig("kuznia_c", "Brygada C", kuzniaStartDateForBrigadeA.plusDays(12)),
        BrigadeConfig("kuznia_d", "Brygada D", kuzniaStartDateForBrigadeA.plusDays(8))
    )
)

// Lista wszystkich dostępnych firm w aplikacji.
val availableCompanies = listOf(teksidCompany, kuzniaCompany)

//================================================================================
// STAŁE I DEFINICJE KOLORÓW
//================================================================================

// Kolory używane do stylizacji kalendarza.
val SpecialDayTextColor = Color.White
val Shift1Color = Color(0xFFE3F2FD) // Kolor tła dla zmiany 1
val Shift2Color = Color(0xFFFFF9C4) // Kolor tła dla zmiany 2
val Shift3Color = Color(0xFFF8BBD0) // Kolor tła dla zmiany 3
val DayOffColor = Color.Transparent // Kolor tła dla dnia wolnego (brak koloru)
val DefaultDayBackgroundColor = Color.Transparent // Domyślny kolor tła dnia
val DayOutOfMonthBackgroundColor = Color.LightGray.copy(alpha = 0.3f) // Kolor tła dla dni spoza bieżącego miesiąca
val DayOutOfMonthTextColor = Color.Gray.copy(alpha = 0.5f) // Kolor tekstu dla dni spoza bieżącego miesiąca

// Predefiniowana paleta kolorów do wyboru dla niestandardowych typów dni.
val specialDayColorOptions = listOf(
    Color(0xFF60A9CE),
    Color(0xFFF08080),
    Color(0xFF9ACD32),
    Color(0xFF00008B),
    Color(0xFF8B0000),
    Shift1Color,
    Shift2Color,
    Shift3Color
)

/**
 * Typ wyliczeniowy reprezentujący wszystkie możliwe rodzaje zmian oraz dni wolne.
 */
enum class ShiftType { SHIFT_1, SHIFT_2, SHIFT_3, DAY_OFF, NONE }
