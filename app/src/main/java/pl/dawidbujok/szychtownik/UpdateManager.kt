package pl.dawidbujok.szychtownik

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Zarządza procesem aktualizacji w aplikacji (in-app updates) przy użyciu biblioteki Google Play Core.
 * Pozwala na sprawdzanie dostępności nowych wersji aplikacji i inicjowanie procesu ich pobierania
 * w elastyczny sposób (w tle, bez przerywania pracy użytkownika).
 *
 * @param context Kontekst aplikacji, wymagany do utworzenia instancji `AppUpdateManager`.
 */
class UpdateManager(private val context: Context) {

    // Instancja menedżera aktualizacji z biblioteki Play Core.
    private val appUpdateManager = AppUpdateManagerFactory.create(context)

    /**
     * Sprawdza, czy w sklepie Google Play jest dostępna nowa wersja aplikacji i jeśli tak,
     * rozpoczyna proces elastycznej aktualizacji (flexible update).
     *
     * @param activity Aktywność, w kontekście której ma zostać wyświetlony przepływ aktualizacji.
     *                 Jest to wymagane przez `startUpdateFlowForResult`.
     */
    fun checkForUpdate(activity: Activity) {
        // Pobierz informacje o dostępności aktualizacji.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            // Sprawdź, czy aktualizacja jest dostępna i czy dozwolony jest typ elastyczny (FLEXIBLE).
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                
                // Jeśli tak, rozpocznij proces aktualizacji w tle.
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo, // Informacje o aktualizacji.
                    AppUpdateType.FLEXIBLE, // Typ aktualizacji: w tle.
                    activity, // Kontekst aktywności.
                    0 // Kod żądania, w przypadku elastycznej aktualizacji może być 0.
                )
            }
        }
    }
}
