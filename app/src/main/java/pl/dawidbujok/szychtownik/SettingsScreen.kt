package pl.dawidbujok.szychtownik

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val appIconManager = remember { AppIconManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var showAppIconDialog by remember { mutableStateOf(false) }
    var showCustomDayManagementScreen by remember { mutableStateOf(false) }

    var brigadeStartDate by remember(uiState.selectedBrigade) {
        mutableStateOf(uiState.selectedBrigade.baseShiftCycleStartDate)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                coroutineScope.launch {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(viewModel.exportBackup().toByteArray())
                    }
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.reader().readText()
                    viewModel.importBackup(json)
                }
            }
        }
    )

    val datePickerDialog = DatePickerDialog(context, { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
        val newDate = LocalDate.of(year, month + 1, dayOfMonth)
        brigadeStartDate = newDate
        viewModel.onSaveBrigadeStartDate(uiState.selectedBrigade.id, newDate)
    }, brigadeStartDate.year, brigadeStartDate.monthValue - 1, brigadeStartDate.dayOfMonth)

    if (showCustomDayManagementScreen) {
        CustomDayManagementScreen(
            viewModel = viewModel,
            onNavigateBack = { showCustomDayManagementScreen = false }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.title_settings)) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.previous_month)) } }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding() + 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(stringResource(id = R.string.settings_title_brigade), style = MaterialTheme.typography.titleLarge)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(id = R.string.label_start_date_for_brigade, uiState.selectedBrigade.displayName), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(brigadeStartDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")), fontSize = 18.sp)
                            Icon(Icons.Filled.DateRange, stringResource(id = R.string.icon_description_change_date))
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth().clickable { showCustomDayManagementScreen = true }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.title_custom_day_types), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
            
            item {
                Text(stringResource(id = R.string.settings_title_general), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top=16.dp))
                Card(Modifier.fillMaxWidth().clickable { showAppIconDialog = true }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.label_choose_icon), modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
            item {
                Text(stringResource(id = R.string.settings_title_backup), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top=16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column {
                        Row(Modifier.fillMaxWidth().clickable { exportLauncher.launch("szychtownik_backup.json") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.action_export_settings), modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Upload, contentDescription = stringResource(id = R.string.icon_description_export))
                        }
                        Divider()
                        Row(Modifier.fillMaxWidth().clickable { importLauncher.launch("application/json") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.action_import_settings), modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Download, contentDescription = stringResource(id = R.string.icon_description_import))
                        }
                    }
                }
            }
        }
    }

    if (showAppIconDialog) {
        AppIconSelectionDialog(
            appIconManager = appIconManager,
            onDismiss = { showAppIconDialog = false }
        )
    }
}