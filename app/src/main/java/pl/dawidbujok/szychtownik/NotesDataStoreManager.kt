package pl.dawidbujok.szychtownik

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import java.time.LocalDate

// Utworzenie instancji DataStore o nazwie "notes_data_v2" do przechowywania danych o notatkach.
private val Context.notesDataStore: DataStore<Preferences> by preferencesDataStore(name = "notes_data_v2")

/**
 * Zarządza operacjami zapisu, odczytu i usuwania notatek użytkownika.
 * W przeciwieństwie do innych menedżerów, ten przechowuje wszystkie notatki jako jedną, zserializowaną mapę
 * w pojedynczym wpisie w DataStore. Jest to efektywne, gdy operacje na notatkach nie są bardzo częste.
 *
 * @param context Kontekst aplikacji, niezbędny do uzyskania dostępu do DataStore.
 */
class NotesDataStoreManager(private val context: Context) {

    companion object {
        // Klucz, pod którym w DataStore przechowywana jest cała mapa notatek jako pojedynczy string JSON.
        private val NOTES_KEY = stringPreferencesKey("notes_map_json_v2")
    }

    // Serializer do konwersji mapy [String, Note] na format JSON i z powrotem.
    private val serializer = MapSerializer(String.serializer(), Note.serializer())

    /**
     * Pobiera strumień (Flow) zawierający mapę wszystkich notatek.
     * @return Flow emitujący mapę, gdzie kluczem jest data w formacie String, a wartością obiekt [Note].
     */
    fun getNotes(): Flow<Map<String, Note>> {
        return context.notesDataStore.data.map { preferences ->
            preferences[NOTES_KEY]?.let {
                try {
                    // Użyj globalnej instancji AppJson do deserializacji stringa JSON na mapę.
                    AppJson.decodeFromString(serializer, it)
                } catch (e: Exception) {
                    // W przypadku błędu deserializacji (np. uszkodzone dane), zwróć pustą mapę.
                    emptyMap()
                }
            } ?: emptyMap() // Jeśli nie ma żadnych danych pod kluczem, również zwróć pustą mapę.
        }
    }

    /**
     * Prywatna funkcja pomocnicza do aktualizacji mapy notatek.
     * Odczytuje bieżący stan, wykonuje na nim przekazaną akcję i zapisuje zaktualizowaną mapę z powrotem.
     * @param updateAction Lambda z operacją do wykonania na mapie notatek.
     */
    private suspend fun updateNotes(updateAction: (MutableMap<String, Note>) -> Unit) {
        context.notesDataStore.edit { preferences ->
            val currentNotes = getNotes().first().toMutableMap()
            updateAction(currentNotes)
            preferences[NOTES_KEY] = AppJson.encodeToString(serializer, currentNotes)
        }
    }

    /**
     * Zapisuje lub aktualizuje notatkę dla podanej daty.
     */
    suspend fun saveNote(date: LocalDate, note: Note) {
        updateNotes { notes ->
            notes[date.toString()] = note
        }
    }

    /**
     * Usuwa notatkę dla podanej daty.
     */
    suspend fun deleteNote(date: LocalDate) {
        updateNotes { notes ->
            notes.remove(date.toString())
        }
    }
    
    /**
     * Przywraca (nadpisuje) całą bazę notatek z podanej mapy (używane przy przywracaniu kopii zapasowej).
     */
    suspend fun restoreNotes(notes: Map<String, Note>) {
        context.notesDataStore.edit {
            it[NOTES_KEY] = AppJson.encodeToString(serializer, notes)
        }
    }
}