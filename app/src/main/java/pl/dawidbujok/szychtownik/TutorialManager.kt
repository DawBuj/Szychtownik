package pl.dawidbujok.szychtownik

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Rozszerzenie do klasy Context, tworzące instancję DataStore o nazwie "tutorial_prefs".
// DataStore to mechanizm do trwałego zapisywania niewielkich ilości danych (klucz-wartość).
private val Context.tutorialDataStore: DataStore<Preferences> by preferencesDataStore(name = "tutorial_prefs")

/**
 * Zarządza logiką wyświetlania samouczków, np. okna "Co nowego?" po aktualizacji aplikacji.
 * Wykorzystuje DataStore do przechowywania informacji o tym, dla której wersji aplikacji samouczek został już pokazany.
 *
 * @param context Kontekst aplikacji, niezbędny do uzyskania dostępu do DataStore.
 */
class TutorialManager(private val context: Context) {

    companion object {
        // Klucz do zapisywania nazwy wersji, dla której samouczek został ostatnio pokazany.
        private val LAST_SHOWN_VERSION_KEY = stringPreferencesKey("last_shown_version_name")
    }

    /**
     * Odczytuje z DataStore nazwę wersji, dla której ostatnio pokazano samouczek.
     * @return Nazwa wersji jako String lub `null`, jeśli nic nie zapisano.
     */
    private suspend fun getLastShownVersion(): String? {
        return context.tutorialDataStore.data.map { preferences ->
            preferences[LAST_SHOWN_VERSION_KEY]
        }.first() // .first() pobiera pierwszą (i jedyną) wartość ze strumienia danych.
    }

    /**
     * Zapisuje w DataStore nazwę bieżącej wersji aplikacji, oznaczając, że samouczek został dla niej wyświetlony.
     * @param versionName Nazwa wersji do zapisania (np. "1.2.0").
     */
    suspend fun markTutorialAsShown(versionName: String) {
        context.tutorialDataStore.edit { settings ->
            settings[LAST_SHOWN_VERSION_KEY] = versionName
        }
    }

    /**
     * Sprawdza, czy samouczek dla bieżącej wersji aplikacji powinien zostać wyświetlony.
     * @param currentVersionName Nazwa bieżącej wersji aplikacji.
     * @return `true`, jeśli zapisana wersja różni się od bieżącej (lub nie istnieje), `false` w przeciwnym razie.
     */
    suspend fun shouldShowTutorial(currentVersionName: String): Boolean {
        val lastShownVersion = getLastShownVersion()
        // Samouczek powinien być pokazany, jeśli ostatnio pokazana wersja jest inna niż obecna.
        return lastShownVersion != currentVersionName
    }
}