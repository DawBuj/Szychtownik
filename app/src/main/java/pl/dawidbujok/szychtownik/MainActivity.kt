package pl.dawidbujok.szychtownik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import pl.dawidbujok.szychtownik.ui.theme.SzychtownikTheme

/**
 * Główna i jedyna aktywność aplikacji, stanowiąca jej punkt wejścia.
 * Odpowiada za ustawienie treści interfejsu użytkownika przy użyciu Jetpack Compose
 * oraz inicjalizację procesów, takich jak sprawdzanie aktualizacji.
 */
class MainActivity : ComponentActivity() {

    // Menedżer odpowiedzialny za obsługę aktualizacji w aplikacji.
    private lateinit var updateManager: UpdateManager

    /**
     * Metoda wywoływana przy pierwszym utworzeniu aktywności.
     * W tym miejscu odbywa się cała jednorazowa konfiguracja, taka jak ustawienie widoku (content view)
     * i inicjalizacja komponentów.
     *
     * @param savedInstanceState Jeśli aktywność jest ponownie tworzona po wcześniejszym zniszczeniu,
     * ten obiekt Bundle zawiera stan, który ostatnio dostarczyła metoda onSaveInstanceState().
     * W przeciwnym razie ma wartość null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicjalizacja menedżera aktualizacji, który będzie sprawdzał dostępność nowych wersji aplikacji.
        updateManager = UpdateManager(this)

        // setContent to funkcja rozszerzająca z biblioteki androidx.activity.compose, 
        // która pozwala na budowanie interfejsu użytkownika za pomocą funkcji Composable.
        setContent {
            // Aplikowanie motywu zdefiniowanego w pliku ui/theme/Theme.kt.
            SzychtownikTheme {
                // Surface to podstawowy kontener w Material Design, który zarządza tłem, kolorem i cieniem.
                Surface(
                    modifier = Modifier.fillMaxSize(), // Modyfikator sprawia, że Surface zajmuje cały ekran.
                    color = MaterialTheme.colorScheme.background // Ustawienie koloru tła z motywu.
                ) {
                    // Wywołanie głównego komponentu Composable aplikacji, który zawiera kalendarz i całą logikę UI.
                    ShiftWorkCalendar()
                }
            }
        }
    }

    /**
     * Metoda wywoływana, gdy aktywność staje się widoczna i gotowa do interakcji z użytkownikiem.
     * Jest to dobre miejsce do uruchamiania procesów, które powinny działać, gdy aplikacja jest na pierwszym planie,
     * np. sprawdzanie aktualizacji.
     */
    override fun onResume() {
        super.onResume()
        // Sprawdź, czy dostępna jest nowa aktualizacja aplikacji.
        updateManager.checkForUpdate(this)
    }
}
