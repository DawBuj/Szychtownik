package pl.dawidbujok.szychtownik

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Tworzy instancję DataStore (jako singleton) dla ustawień aplikacji.
// `preferencesDataStore` to funkcja rozszerzająca, która zapewnia, że w całej aplikacji istnieje tylko jedna instancja o nazwie "settings".
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Zarządza zapisywaniem i odczytywaniem ustawień użytkownika z DataStore,
 * takich jak wybrana firma, brygada oraz niestandardowe daty rozpoczęcia cyklu zmianowego.
 *
 * @param context Kontekst aplikacji, niezbędny do uzyskania dostępu do DataStore.
 */
class DataStoreManager(private val context: Context) {

    companion object {
        // Klucz do przechowywania ID aktualnie wybranej firmy.
        val SELECTED_COMPANY_ID_KEY = stringPreferencesKey("selected_company_id")

        // Klucz do przechowywania ID aktualnie wybranej brygady.
        val SELECTED_BRIGADE_ID_KEY = stringPreferencesKey("selected_brigade_id")

        // Prefiks dla kluczy przechowujących niestandardowe daty rozpoczęcia dla brygad.
        private const val CUSTOM_DATE_PREFIX = "custom_start_date_"
    }

    /**
     * Zapisuje wybór firmy i brygady dokonany przez użytkownika.
     */
    suspend fun saveSelection(companyId: String, brigadeId: String) {
        context.dataStore.edit { settings ->
            settings[SELECTED_COMPANY_ID_KEY] = companyId
            settings[SELECTED_BRIGADE_ID_KEY] = brigadeId
        }
    }

    /**
     * Pobiera strumień (Flow) z zapisanym ID wybranej firmy.
     * Flow emituje nową wartość za każdym razem, gdy ID firmy w DataStore ulegnie zmianie.
     */
    fun getSelectedCompanyId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[SELECTED_COMPANY_ID_KEY]
        }
    }

    /**
     * Pobiera strumień (Flow) z zapisanym ID wybranej brygady.
     */
    fun getSelectedBrigadeId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[SELECTED_BRIGADE_ID_KEY]
        }
    }

    /**
     * Tworzy dynamiczny klucz DataStore dla niestandardowej daty rozpoczęcia brygady.
     * @param brigadeId ID brygady, dla której tworzony jest klucz.
     */
    private fun getBrigadeStartDateKey(brigadeId: String): Preferences.Key<String> {
        return stringPreferencesKey("$CUSTOM_DATE_PREFIX$brigadeId")
    }

    /**
     * Zapisuje niestandardową datę rozpoczęcia dla konkretnej brygady.
     * Data jest konwertowana na tekst w formacie ISO (np. "2023-10-27").
     */
    suspend fun saveBrigadeStartDate(brigadeId: String, date: LocalDate) {
        context.dataStore.edit { preferences ->
            preferences[getBrigadeStartDateKey(brigadeId)] = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    /**
     * Pobiera strumień (Flow) zawierający mapę wszystkich niestandardowych dat rozpoczęcia,
     * z kluczem w postaci ID brygady.
     */
    fun getAllCustomStartDates(): Flow<Map<String, LocalDate>> {
        return context.dataStore.data.map { preferences ->
            preferences.asMap()
                // 1. Filtruj wszystkie ustawienia, aby znaleźć tylko te pasujące do naszego prefiksu dat.
                .filter { it.key.name.startsWith(CUSTOM_DATE_PREFIX) }
                // 2. Przetwórz znalezione wpisy.
                .mapNotNull { (key, value) ->
                    // Wyodrębnij ID brygady z nazwy klucza.
                    val brigadeId = key.name.removePrefix(CUSTOM_DATE_PREFIX)
                    // Sprawdź, czy wartość jest stringiem i spróbuj ją sparsować jako datę.
                    (value as? String)?.let {
                        try {
                            brigadeId to LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (e: Exception) {
                            // Obsługa błędów, jeśli dane są uszkodzone i nie można ich sparsować.
                            null
                        }
                    }
                }.toMap() // 3. Zbierz przetworzone pary (ID, data) do nowej mapy.
        }
    }
}
