package pl.dawidbujok.szychtownik

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Enum reprezentujący dostępne ikony aplikacji. Każda ikona jest zdefiniowana w manifeście Androida
 * jako `activity-alias`.
 *
 * @property aliasName Pełna, kwalifikowana nazwa klasy aliasu aktywności zdefiniowanego w `AndroidManifest.xml`.
 */
enum class AppIcon(val aliasName: String) {
    SZYCHTOWNIK("pl.dawidbujok.szychtownik.icon.Szychtownik"),
    KUŹNIA("pl.dawidbujok.szychtownik.icon.Kuznia"),
    TEKSID1("pl.dawidbujok.szychtownik.icon.Teksid");

    companion object {
        /**
         * Bezpiecznie zwraca wariant enuma na podstawie jego nazwy (np. "SZYCHTOWNIK").
         * Używane do przywracania wyboru ikony z kopii zapasowej.
         *
         * @param name Nazwa wariantu enuma.
         * @return Odpowiadający [AppIcon] lub domyślny [SZYCHTOWNIK], jeśli nie znaleziono.
         */
        fun fromName(name: String?): AppIcon {
            return entries.find { it.name == name } ?: SZYCHTOWNIK
        }
    }
}

/**
 * Zarządza dynamiczną zmianą ikony aplikacji poprzez włączanie i wyłączanie odpowiednich `activity-alias`.
 *
 * @param context Kontekst aplikacji, niezbędny do uzyskania dostępu do `PackageManager`.
 */
class AppIconManager(private val context: Context) {

    /**
     * Pobiera aktualnie aktywną ikonę aplikacji.
     * @return [AppIcon] reprezentujący aktualnie włączony alias.
     */
    fun getCurrentAppIcon(): AppIcon {
        // Sprawdź, który z aliasów zdefiniowanych w enumie jest aktualnie włączony.
        return AppIcon.entries.find { isAliasEnabled(it.aliasName) } ?: AppIcon.SZYCHTOWNIK
    }

    /**
     * Ustawia nową ikonę aplikacji, włączając wybrany alias i wyłączając wszystkie pozostałe.
     *
     * @param icon Ikona do ustawienia, wybrana z enuma [AppIcon].
     */
    fun setAppIcon(icon: AppIcon) {
        // Jeśli wybrana ikona jest już aktywna, nic nie rób.
        if (getCurrentAppIcon() == icon) return

        // Wyłącz wszystkie aliasy, które nie są wybraną ikoną.
        AppIcon.entries.forEach { appIcon ->
             if (appIcon != icon) {
                context.packageManager.setComponentEnabledSetting(
                    ComponentName(context, appIcon.aliasName),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP // Nie zabijaj aplikacji po zmianie.
                )
            }
        }
        
        // Włącz wybrany alias.
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, icon.aliasName),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    /**
     * Sprawdza, czy dany alias aktywności jest włączony.
     *
     * @param aliasName Pełna nazwa klasy aliasu.
     * @return `true` jeśli alias jest włączony, `false` w przeciwnym razie.
     */
    private fun isAliasEnabled(aliasName: String): Boolean {
        return try {
            val state = context.packageManager.getComponentEnabledSetting(ComponentName(context, aliasName))
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } catch (e: Exception) {
            false
        }
    }
}