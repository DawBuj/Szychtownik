package pl.dawidbujok.szychtownik

import kotlinx.serialization.Serializable

/**
 * Reprezentuje kontener na wszystkie dane użytkownika, które są przeznaczone do serializacji
 * w procesie tworzenia kopii zapasowej (backup) i przywracania danych.
 * Obiekt tej klasy jest konwertowany do formatu JSON.
 *
 * @property customDayTypes Lista niestandardowych typów dni zdefiniowanych przez użytkownika.
 * @property specialDayEntries Mapa wpisów o dniach specjalnych (klucz to data w formacie String).
 * @property customStartDates Mapa niestandardowych dat rozpoczęcia cyklu dla poszczególnych brygad (klucz to ID brygady, wartość to data w formacie String).
 * @property selectedCompanyId ID ostatnio wybranej przez użytkownika firmy.
 * @property selectedBrigadeId ID ostatnio wybranej przez użytkownika brygady.
 * @property selectedAppIcon Nazwa (alias) wybranej przez użytkownika ikony aplikacji. Pole jest opcjonalne (nullable).
 * @property notes Mapa notatek użytkownika (klucz to data w formacie String). Pole jest opcjonalne (nullable).
 */
@Serializable
data class BackupData(
    val customDayTypes: List<CustomDayType>,
    val specialDayEntries: Map<String, SpecialDayInstance>,
    val customStartDates: Map<String, String>,
    val selectedCompanyId: String?,
    val selectedBrigadeId: String?,
    val selectedAppIcon: String? = null, // To pole jest teraz opcjonalne.
    val notes: Map<String, Note>? = null // To pole jest teraz opcjonalne.
)
