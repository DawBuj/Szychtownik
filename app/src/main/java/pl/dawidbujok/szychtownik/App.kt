package pl.dawidbujok.szychtownik

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Główna klasa aplikacji, dziedzicząca po [Application].
 * Jest to miejsce, w którym można inicjalizować globalne zasoby i konfiguracje
 * tuż po uruchomieniu aplikacji, jeszcze przed utworzeniem jakiejkolwiek aktywności.
 */
class App : Application() {

    /**
     * Wywoływana przy tworzeniu aplikacji. Służy do jednorazowej inicjalizacji.
     */
    override fun onCreate() {
        super.onCreate()
        // Tworzy kanał powiadomień, który jest wymagany dla Androida 8.0 (Oreo) i nowszych.
        createNotificationChannel()
    }

    /**
     * Tworzy kanał powiadomień dla przypomnień o notatkach.
     * Na urządzeniach z Androidem 8.0 (API 26) i nowszych, wszystkie powiadomienia muszą być przypisane do kanału.
     * Ta metoda jest wywoływana raz, przy starcie aplikacji.
     */
    private fun createNotificationChannel() {
        // Sprawdzenie, czy wersja systemu jest wystarczająco nowa (Android Oreo lub wyższa).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Definicja kanału powiadomień.
            val channel = NotificationChannel(
                "note_channel", // ID kanału - musi być unikalne w aplikacji.
                "Przypomnienia o notatkach", // Nazwa kanału widoczna dla użytkownika w ustawieniach.
                NotificationManager.IMPORTANCE_HIGH // Priorytet powiadomień w tym kanale.
            )
            channel.description = "Kanał dla przypomnień o notatkach w aplikacji Szychtownik" // Opis kanału.

            // Pobranie systemowego menedżera powiadomień.
            val notificationManager = getSystemService(NotificationManager::class.java)
            // Zarejestrowanie kanału w systemie.
            notificationManager.createNotificationChannel(channel)
        }
    }
}