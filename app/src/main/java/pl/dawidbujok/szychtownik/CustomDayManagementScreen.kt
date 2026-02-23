package pl.dawidbujok.szychtownik

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Ekran (Composable) do zarządzania niestandardowymi typami dni (np. Urlop, L4).
 * Umożliwia użytkownikowi dodawanie, edytowanie i usuwanie własnych oznaczeń.
 *
 * @param viewModel Główny ViewModel aplikacji, dostarczający dane i logikę.
 * @param onNavigateBack Funkcja zwrotna wywoływana, gdy użytkownik chce wrócić do poprzedniego ekranu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDayManagementScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    // Pobranie stanu UI z ViewModelu.
    val uiState by viewModel.uiState.collectAsState()
    // Stan kontrolujący widoczność okna dialogowego edycji/tworzenia.
    var showEditDialog by remember { mutableStateOf(false) }
    // Stan przechowujący typ dnia, który jest aktualnie edytowany.
    var dayTypeToEdit by remember { mutableStateOf<CustomDayType?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Niestandardowe oznaczenia") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć") } }
            )
        },
        floatingActionButton = {
            // Przycisk do dodawania nowego typu dnia.
            FloatingActionButton(onClick = {
                // Ograniczenie liczby niestandardowych typów dni, aby uniknąć zaśmiecenia interfejsu.
                if (uiState.customDayTypes.size < 9) {
                    dayTypeToEdit = null // Ustawienie na null oznacza tworzenie nowego typu.
                    showEditDialog = true
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj nowy typ dnia")
            }
        }
    ) { innerPadding ->
        // Lista wyświetlająca wszystkie niestandardowe typy dni.
        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            items(uiState.customDayTypes, key = { it.id }) { dayType ->
                // Użycie reużywalnego komponentu do wyświetlenia pojedynczego elementu listy.
                CustomDayTypeItem(
                    dayType = dayType,
                    // Domyślny typ urlopu nie może być usunięty.
                    canBeDeleted = dayType.id != SpecialDaysDataStoreManager.defaultVacationType.id,
                    onEditClicked = {
                        dayTypeToEdit = dayType
                        showEditDialog = true
                    },
                    onDeleteClicked = { viewModel.onDeleteCustomDayType(dayType) }
                )
            }
        }
    }

    // Warunkowe wyświetlanie okna dialogowego edycji.
    if (showEditDialog) {
        CustomDayTypeEditDialog(
            dayType = dayTypeToEdit, // Przekazanie typu do edycji lub null dla nowego.
            onDismiss = { showEditDialog = false },
            onSave = {
                // Wywołanie funkcji ViewModelu do zapisu, z flagą informującą, czy jest to nowy element.
                viewModel.onSaveCustomDayType(it, dayTypeToEdit == null)
                showEditDialog = false
            }
        )
    }
}