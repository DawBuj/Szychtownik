package pl.dawidbujok.szychtownik

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

/**
 * Zarządza tworzeniem i przywracaniem kopii zapasowych danych użytkownika.
 * Klasa ta współpracuje z różnymi menedżerami danych (DataStore), aby zebrać wszystkie dane
 * do jednego pliku JSON, a następnie przywrócić je, obsługując przy tym migrację danych
 * ze starszych formatów kopii zapasowej.
 *
 * @param context Kontekst aplikacji, niezbędny do inicjalizacji menedżerów danych.
 */
class BackupManager(private val context: Context) {

    // region Właściwości
    // Inicjalizacja menedżerów, z których będą pobierane i do których będą zapisywane dane.
    private val dataStoreManager = DataStoreManager(context)
    private val specialDaysDataStoreManager = SpecialDaysDataStoreManager(context)
    private val appIconManager = AppIconManager(context)
    private val notesDataStoreManager = NotesDataStoreManager(context)
    // endregion

    /**
     * Tworzy ciąg znaków JSON reprezentujący bieżący stan wszystkich danych użytkownika.
     * @return Ciąg znaków JSON gotowy do zapisu jako plik kopii zapasowej.
     */
    suspend fun createBackupJson(): String {
        // Zbierz najnowsze dane ze wszystkich odpowiednich strumieni (Flow).
        val customDayTypes = specialDaysDataStoreManager.getCustomDayTypes().first()
        val specialDayEntries = specialDaysDataStoreManager.getSpecialDayEntries().first()
        val customStartDates = dataStoreManager.getAllCustomStartDates().first()
        val selectedCompanyId = dataStoreManager.getSelectedCompanyId().first()
        val selectedBrigadeId = dataStoreManager.getSelectedBrigadeId().first()
        val selectedAppIcon = appIconManager.getCurrentAppIcon()
        val notes = notesDataStoreManager.getNotes().first()

        // Zgromadź wszystkie dane w jednym obiekcie `BackupData`.
        val backupData = BackupData(
            customDayTypes = customDayTypes,
            specialDayEntries = specialDayEntries.mapKeys { it.key.toString() }, // Klucze mapy (LocalDate) muszą być stringami w JSON.
            customStartDates = customStartDates.mapValues { it.value.toString() }, // Wartości mapy (LocalDate) również konwertujemy na string.
            selectedCompanyId = selectedCompanyId,
            selectedBrigadeId = selectedBrigadeId,
            selectedAppIcon = selectedAppIcon.name, // Zapisz nazwę enuma (np. "DEFAULT") dla większej stabilności formatu.
            notes = notes
        )

        // Zserializuj obiekt `BackupData` do ciągu znaków JSON.
        return AppJson.encodeToString(backupData)
    }

    /**
     * Przywraca stan aplikacji z podanego ciągu znaków JSON.
     * Funkcja ta zawiera logikę migracji dla starszych formatów kopii zapasowej, aby zapewnić kompatybilność wsteczną.
     * @param jsonData Ciąg znaków JSON z pliku kopii zapasowej.
     */
    suspend fun restoreBackup(jsonData: String) {
        // --- LOGIKA MIGRACJI ---
        // Krok 1: Sparsuj kopię zapasową do generycznej struktury JSON, aby najpierw ją zbadać.
        val rootElement = AppJson.parseToJsonElement(jsonData)
        val rootObject = rootElement.jsonObject

        // Krok 2: Sprawdź, czy mamy do czynienia ze starym formatem. W starym formacie `SpecialDayInstance` 
        // był prostym stringiem (typeId), a nie obiektem.
        val specialDayEntries = rootObject["specialDayEntries"]?.jsonObject
        val isOldFormat = specialDayEntries?.values?.any { it is JsonPrimitive } == true

        // Krok 3: Jeśli to stary format, utwórz w pamięci nowy, zmigrowany obiekt JSON.
        val finalJsonData = if (isOldFormat) {
            val newRootObject = buildJsonObject {
                rootObject.forEach { (key, value) ->
                    if (key == "specialDayEntries") {
                        val newEntries = buildJsonObject {
                            specialDayEntries?.forEach { (date, entryValue) ->
                                if (entryValue is JsonPrimitive) {
                                    // Konwertuj starą wartość (string) na nowy obiekt `SpecialDayInstance`.
                                    val newInstance = SpecialDayInstance(typeId = entryValue.content, reminderDateTime = null)
                                    put(date, AppJson.encodeToJsonElement(newInstance))
                                } else {
                                    // Ten wpis jest już w nowym formacie, więc po prostu go skopiuj.
                                    put(date, entryValue)
                                }
                            }
                        }
                        put("specialDayEntries", newEntries)
                    } else {
                        // Skopiuj wszystkie pozostałe pola bez zmian.
                        put(key, value)
                    }
                }
            }
            newRootObject.toString()
        } else {
            jsonData // Migracja nie jest potrzebna, użyj oryginalnych danych.
        }

        // --- LOGIKA PRZYWRACANIA ---
        // Krok 4: Zdekoduj dane JSON (już w poprawnym formacie) do naszej klasy danych `BackupData`.
        val backupData = AppJson.decodeFromString<BackupData>(finalJsonData)

        // Przywróć wszystkie dane, zapisując je w odpowiednich menedżerach.
        specialDaysDataStoreManager.saveCustomDayTypes(backupData.customDayTypes)

        // Przywróć dni specjalne, konwertując klucze z powrotem na `LocalDate`.
        val restoredSpecialDayEntries = backupData.specialDayEntries.mapKeys { LocalDate.parse(it.key) }
        specialDaysDataStoreManager.setSpecialDays(restoredSpecialDayEntries)

        // Przywróć niestandardowe daty startowe.
        val customStartDates = backupData.customStartDates.mapValues { LocalDate.parse(it.value) }
        customStartDates.forEach { (brigadeId, date) ->
            dataStoreManager.saveBrigadeStartDate(brigadeId, date)
        }

        // Przywróć wybór firmy i brygady.
        if (backupData.selectedCompanyId != null && backupData.selectedBrigadeId != null) {
            dataStoreManager.saveSelection(backupData.selectedCompanyId, backupData.selectedBrigadeId)
        }

        // Przywróć ikonę aplikacji w bezpieczny sposób.
        val appIcon = AppIcon.fromName(backupData.selectedAppIcon)
        appIconManager.setAppIcon(appIcon)

        // Przywróć notatki, jeśli istnieją w kopii zapasowej.
        backupData.notes?.let {
            notesDataStoreManager.restoreNotes(it)
        }
    }
}
