package pl.dawidbujok.szychtownik

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import java.time.LocalDate

// Definicja DataStore dla ustawień aplikacji. Zmiana nazwy (np. na v6) może być sposobem na wymuszenie migracji lub wyczyszczenia danych po dużych zmianach.
private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings_v6")

/**
 * Zarządza zapisem i odczytem danych związanych z dniami specjalnymi i ich niestandardowymi typami.
 * Przechowuje zarówno definicje typów dni (np. "Urlop"), jak i konkretne ich wystąpienia w kalendarzu.
 *
 * @param context Kontekst aplikacji, niezbędny do uzyskania dostępu do DataStore.
 */
class SpecialDaysDataStoreManager(private val context: Context) {

    companion object {
        // Klucze do przechowywania danych w DataStore jako stringi JSON.
        private val CUSTOM_DAY_TYPES_KEY = stringPreferencesKey("custom_day_types_json_v2")
        private val SPECIAL_DAYS_ENTRIES_KEY = stringPreferencesKey("special_days_entries_json_v5")

        // Stałe ID dla domyślnych typów dni, aby można je było identyfikować (np. w celu zablokowania usunięcia).
        private const val VACATION_ID = "default-id-vacation"
        private const val W4_ID = "default-id-w4"
        private const val XX_ID = "default-id-xx"

        // Domyślne typy dni, które są dostępne od pierwszego uruchomienia aplikacji.
        val defaultVacationType = CustomDayType(
            id = VACATION_ID,
            code = "U",
            description = "Urlop",
            color = Color(0xFF60A9CE),
            hasReminder = true // Domyślnie urlop ma włączoną opcję przypomnienia.
        )
        private val defaultW4Type = CustomDayType(
            id = W4_ID,
            code = "W4",
            description = "Wolne za 4-brygadówkę",
            color = Color(0xFFF08080)
        )
        private val defaultXXType = CustomDayType(
            id = XX_ID,
            code = "XX",
            description = "Wolne niepłatne",
            color = Color(0xFF9ACD32)
        )
        // Lista początkowych typów dni do zapisania przy pierwszym uruchomieniu.
        val initialDayTypes = listOf(defaultVacationType, defaultW4Type, defaultXXType)
    }

    // Blok `init` jest wykonywany przy tworzeniu instancji klasy.
    init {
        // Użyj `runBlocking` do wykonania operacji suspendującej wewnątrz konstruktora.
        // Należy tego używać ostrożnie, ale tutaj jest to akceptowalne, ponieważ jest to jednorazowa operacja inicjalizacyjna.
        runBlocking {
            // Jeśli lista niestandardowych typów dni jest pusta, zapisz początkowe wartości.
            if (getCustomDayTypes().first().isEmpty()) {
                saveCustomDayTypes(initialDayTypes)
            }
        }
    }

    /**
     * Pobiera strumień (Flow) z listą wszystkich niestandardowych typów dni.
     */
    fun getCustomDayTypes(): Flow<List<CustomDayType>> {
        return context.appSettingsDataStore.data.map { preferences ->
            preferences[CUSTOM_DAY_TYPES_KEY]?.let {
                try {
                    AppJson.decodeFromString<List<CustomDayType>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
    }

    /**
     * Zapisuje całą listę niestandardowych typów dni do DataStore.
     */
    suspend fun saveCustomDayTypes(types: List<CustomDayType>) {
        context.appSettingsDataStore.edit { settings ->
            settings[CUSTOM_DAY_TYPES_KEY] = AppJson.encodeToString(types)
        }
    }

    /**
     * Pobiera strumień (Flow) z mapą wszystkich wpisów o dniach specjalnych.
     */
    fun getSpecialDayEntries(): Flow<Map<LocalDate, SpecialDayInstance>> {
        return context.appSettingsDataStore.data.map { preferences ->
            preferences[SPECIAL_DAYS_ENTRIES_KEY]?.let {
                try {
                    // Deserializuj mapę, gdzie klucze są stringami, a następnie przekonwertuj je na obiekty LocalDate.
                    AppJson.decodeFromString<Map<String, SpecialDayInstance>>(it)
                        .mapKeys { (dateString, _) -> LocalDate.parse(dateString) }
                } catch (e: Exception) {
                    emptyMap()
                }
            } ?: emptyMap()
        }
    }

    /**
     * Zapisuje (nadpisuje) całą mapę dni specjalnych.
     */
    suspend fun setSpecialDays(entries: Map<LocalDate, SpecialDayInstance>) {
        // Przekonwertuj klucze LocalDate na String przed serializacją do JSON.
        val stringMap = entries.mapKeys { it.key.toString() }
        context.appSettingsDataStore.edit {
            it[SPECIAL_DAYS_ENTRIES_KEY] = AppJson.encodeToString(stringMap)
        }
    }

    /**
     * Ustawia lub aktualizuje pojedynczy dzień specjalny.
     */
    suspend fun setSpecialDay(date: LocalDate, instance: SpecialDayInstance) {
        val currentEntries = getSpecialDayEntries().first().toMutableMap()
        currentEntries[date] = instance
        setSpecialDays(currentEntries)
    }

    /**
     * Usuwa wpis o dniu specjalnym dla podanej daty.
     */
    suspend fun removeSpecialDay(date: LocalDate) {
        val currentEntries = getSpecialDayEntries().first().toMutableMap()
        currentEntries.remove(date)
        setSpecialDays(currentEntries)
    }

    /**
     * Usuwa wszystkie wystąpienia dni specjalnych danego typu z kalendarza.
     * @param typeId ID typu dnia, którego wszystkie instancje mają zostać usunięte.
     */
    suspend fun removeAllEntriesForType(typeId: String) {
        val currentEntries = getSpecialDayEntries().first().toMutableMap()
        val keysToRemove = currentEntries.filterValues { it.typeId == typeId }.keys
        keysToRemove.forEach { currentEntries.remove(it) }
        setSpecialDays(currentEntries)
    }
}