package pl.dawidbujok.szychtownik

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.boguszpawlowski.composecalendar.StaticCalendar
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.rememberCalendarState
import io.github.boguszpawlowski.composecalendar.selection.SelectionState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun getDayDisplayInfo(
    date: LocalDate,
    dayState: DayState<*>,
    selectedBrigadeConfig: BrigadeConfig,
    shiftSystem: IShiftSystem,
    specialDayEntries: Map<LocalDate, SpecialDayInstance>,
    customDayTypes: List<CustomDayType>,
    notes: Map<String, Note>
): DayDisplayInfo {
    val isCurrentDay = dayState.isCurrentDay
    val isFromCurrentMonth = dayState.isFromCurrentMonth
    val specialDayInstance = specialDayEntries[date]
    val note = notes[date.toString()]
    val hasNote = note != null
    val hasReminder = note?.reminderDateTime != null || specialDayInstance?.reminderDateTime != null

    if (!isFromCurrentMonth) {
        return DayDisplayInfo(ShiftType.NONE, "", DayOutOfMonthBackgroundColor, isCurrentDay, isFromCurrentMonth, null, hasNote, hasReminder)
    }

    if (specialDayInstance != null) {
        val dayType = customDayTypes.find { it.id == specialDayInstance.typeId }
        if (dayType != null) {
            return DayDisplayInfo(ShiftType.NONE, dayType.code, dayType.color, isCurrentDay, true, dayType, hasNote, hasReminder)
        }
    }

    val shiftType = shiftSystem.getShiftTypeForDate(date, selectedBrigadeConfig.baseShiftCycleStartDate)
    val (shiftNumberText, bgColor) = when (shiftType) {
        ShiftType.SHIFT_1 -> "1" to Shift1Color
        ShiftType.SHIFT_2 -> "2" to Shift2Color
        ShiftType.SHIFT_3 -> "3" to Shift3Color
        ShiftType.DAY_OFF -> "" to DayOffColor
        ShiftType.NONE -> "" to DefaultDayBackgroundColor
    }

    return DayDisplayInfo(shiftType, shiftNumberText, bgColor, isCurrentDay, true, null, hasNote, hasReminder)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : SelectionState> ShiftCalendarDayCell(
    dayState: DayState<T>,
    selectedBrigadeConfig: BrigadeConfig,
    shiftSystem: IShiftSystem,
    specialDayEntries: Map<LocalDate, SpecialDayInstance>,
    customDayTypes: List<CustomDayType>,
    notes: Map<String, Note>,
    onSpecialDaySet: (LocalDate, String?) -> Unit,
    onNoteClicked: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val date = dayState.date
    val displayInfo = getDayDisplayInfo(date, dayState, selectedBrigadeConfig, shiftSystem, specialDayEntries, customDayTypes, notes)
    var showSpecialDayMenu by remember { mutableStateOf(false) }
    val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY

    val fontWeight = if (displayInfo.isCurrentDay || (isSunday && displayInfo.customDayType == null)) FontWeight.ExtraBold else FontWeight.Normal
    val dayNumberActualColor = when {
        !displayInfo.isFromCurrentMonth -> DayOutOfMonthTextColor
        displayInfo.customDayType != null -> SpecialDayTextColor
        displayInfo.shiftNumberText.isNotEmpty() -> Color.Black
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = { onDayClick(date) },
                onLongClick = { if (displayInfo.isFromCurrentMonth) showSpecialDayMenu = true }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.matchParentSize().clip(CircleShape).background(displayInfo.backgroundColor))

        if (displayInfo.isCurrentDay && displayInfo.isFromCurrentMonth) {
            Box(modifier = Modifier.matchParentSize().clip(CircleShape).border(3.dp, Color.Red, CircleShape).padding(3.dp).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape))
        }

        if (displayInfo.shiftNumberText.isNotEmpty() && displayInfo.isFromCurrentMonth) {
            val isSingleCharSpecialCode = displayInfo.customDayType != null && displayInfo.shiftNumberText.length == 1
            val fontSize = if (isSingleCharSpecialCode || displayInfo.customDayType == null) 40.sp else 30.sp

            Text(
                text = displayInfo.shiftNumberText,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = if (displayInfo.customDayType != null) Color.White.copy(alpha = 0.60f) else Color.Gray.copy(alpha = 0.50f),
                textAlign = TextAlign.Center
            )
        }

        Text(text = date.dayOfMonth.toString(), color = dayNumberActualColor, fontSize = 18.sp, fontWeight = fontWeight, textAlign = TextAlign.Center)

        if (displayInfo.hasNote) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).border(1.dp, Color.White, CircleShape))
        }
        if (displayInfo.hasReminder) {
            Icon(imageVector = Icons.Default.AccessTime, contentDescription = stringResource(id = R.string.label_remind), modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(12.dp), tint = MaterialTheme.colorScheme.primary)
        }

        DropdownMenu(expanded = showSpecialDayMenu, onDismissRequest = { showSpecialDayMenu = false }) {
            DropdownMenuItem(text = { Text(stringResource(id = R.string.action_add_edit_note)) }, onClick = { onNoteClicked(date); showSpecialDayMenu = false })
            Divider()
            customDayTypes.forEach { dayType ->
                DropdownMenuItem(text = { Text("${dayType.code} - ${dayType.description}") }, onClick = { onSpecialDaySet(date, dayType.id); showSpecialDayMenu = false })
            }
            if (displayInfo.customDayType != null) {
                DropdownMenuItem(text = { Text(stringResource(id = R.string.remove_special_day_mark)) }, onClick = { onSpecialDaySet(date, null); showSpecialDayMenu = false })
            }
        }
    }
}

@Composable
fun DaysOfWeekHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun getAppVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "N/A"
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e("AppVersion", "Could not get package info", e)
        "N/A"
    }
}

@Composable
fun RequestNotificationPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) }

        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean -> hasPermission = isGranted }

        LaunchedEffect(Unit) {
            if (!hasPermission) {
                launcher.launch(permission)
            }
        }
    }
}

@Composable
fun ExactAlarmPermissionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wymagane uprawnienie") },
        text = { Text("Aby przypomnienia działały poprawnie, aplikacja potrzebuje uprawnienia do ustawiania dokładnych alarmów. Zostaniesz teraz przeniesiony do ustawień systemowych, aby je włączyć.") },
        confirmButton = {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
                onDismiss()
            }) {
                Text("Przejdź do ustawień")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftWorkCalendar(viewModel: MainViewModel = viewModel(), modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    RequestNotificationPermission(context = context)

    var showMainMenu by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }
    var expandedCompanyMenu by remember { mutableStateOf<Company?>(null) }
    var showNoteDialogForDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedDateForDetails by remember { mutableStateOf<LocalDate?>(null) }
    val appVersionName = remember(context) { getAppVersionName(context) }

    uiState.showNewFeaturesDialogForVersion?.let { versionName ->
        NewFeaturesDialog(versionName = versionName, onDismiss = { viewModel.onNewFeaturesDialogDismissed() })
    }
    
    if (uiState.showExactAlarmPermissionDialog) {
        ExactAlarmPermissionDialog(onDismiss = { viewModel.onExactAlarmPermissionDialogDismissed() })
    }

    if (uiState.showSettingsScreen) {
        SettingsScreen(viewModel = viewModel, onNavigateBack = { viewModel.onShowSettings(false) })
        return
    }

    if (uiState.showVacationManagementScreen) {
        VacationManagementScreen(viewModel = viewModel, onNavigateBack = { viewModel.onShowVacationManagementScreen(false) })
        return
    }

    val calendarState = rememberCalendarState(initialMonth = YearMonth.now())
    val currentDisplayMonth by remember { derivedStateOf { calendarState.monthState.currentMonth } }

    uiState.specialDayForReminder?.let { (date, dayType) ->
        SpecialDayReminderDialog(dayType = dayType, onDismiss = { viewModel.onSpecialDayReminderConfirmed(null) }, onConfirm = { time -> viewModel.onSpecialDayReminderConfirmed(time) })
    }

    if (uiState.showAboutDialog) {
        val uriHandler = LocalUriHandler.current
        val privacyPolicyUrl = "https://dawbuj.github.io/Szychtownik/privacy-policy.html"

        AlertDialog(
            onDismissRequest = { viewModel.onShowAboutDialog(false) },
            title = { Text(stringResource(id = R.string.dialog_about_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = "${stringResource(id = R.string.dialog_about_version_label)} $appVersionName", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                    Text(text = stringResource(id = R.string.dialog_about_copyright_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    Text(text = stringResource(id = R.string.dialog_about_copyright_text), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                    Text(text = stringResource(id = R.string.dialog_about_whats_new_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    ChangelogContent()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.privacy_policy),
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { uriHandler.openUri(privacyPolicyUrl) }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.onShowAboutDialog(false) }) { Text(stringResource(id = R.string.dialog_action_close)) } }
        )
    }

    showNoteDialogForDate?.let { date ->
        NoteEditDialog(date = date, initialNote = uiState.notes[date.toString()], onDismiss = { showNoteDialogForDate = null }, onSave = { viewModel.onSaveNote(date, it); showNoteDialogForDate = null }, onDelete = { viewModel.onDeleteNote(date); showNoteDialogForDate = null })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${currentDisplayMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }} ${currentDisplayMonth.year}") },
                navigationIcon = { IconButton(onClick = { calendarState.monthState.currentMonth = calendarState.monthState.currentMonth.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(id = R.string.previous_month)) } },
                actions = {
                    IconButton(onClick = { calendarState.monthState.currentMonth = calendarState.monthState.currentMonth.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(id = R.string.next_month)) }
                    Box {
                        IconButton(onClick = { showMainMenu = true }) { Icon(Icons.Filled.MoreVert, stringResource(id = R.string.menu_description)) }
                        DropdownMenu(expanded = showMainMenu, onDismissRequest = { showMainMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(id = R.string.menu_brigade_selection)) }, onClick = { showMainMenu = false; showSelectionMenu = true }, trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) })
                            DropdownMenuItem(text = { Text(stringResource(id = R.string.menu_vacations)) }, onClick = { viewModel.onShowVacationManagementScreen(true); showMainMenu = false })
                            DropdownMenuItem(text = { Text(stringResource(id = R.string.menu_settings)) }, onClick = { viewModel.onShowSettings(true); showMainMenu = false })
                            DropdownMenuItem(text = { Text(stringResource(id = R.string.menu_about)) }, onClick = { viewModel.onShowAboutDialog(true); showMainMenu = false })
                        }
                        DropdownMenu(expanded = showSelectionMenu, onDismissRequest = { showSelectionMenu = false }) {
                            availableCompanies.forEach { company ->
                                DropdownMenuItem(text = { Text(company.displayName) }, onClick = { expandedCompanyMenu = company }, trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) })
                            }
                        }
                        val currentlyExpandedCompany = expandedCompanyMenu
                        if (currentlyExpandedCompany != null) {
                            DropdownMenu(expanded = true, onDismissRequest = { expandedCompanyMenu = null }) {
                                currentlyExpandedCompany.brigades.forEach { brigade ->
                                    DropdownMenuItem(text = { Text(brigade.displayName) }, onClick = {
                                        showMainMenu = false; showSelectionMenu = false; expandedCompanyMenu = null
                                        if (uiState.selectedBrigade.id != brigade.id) { viewModel.onBrigadeSelected(currentlyExpandedCompany, brigade) }
                                    })
                                }
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(horizontal = 8.dp)) {
            DaysOfWeekHeader(modifier = Modifier.padding(top = 8.dp))
            StaticCalendar(
                calendarState = calendarState,
                monthHeader = { },
                daysOfWeekHeader = { },
                dayContent = { dayState ->
                    ShiftCalendarDayCell(
                        dayState = dayState,
                        selectedBrigadeConfig = uiState.selectedBrigade,
                        shiftSystem = uiState.selectedCompany.shiftSystem,
                        specialDayEntries = uiState.specialDayEntries,
                        customDayTypes = uiState.customDayTypes,
                        notes = uiState.notes,
                        onSpecialDaySet = viewModel::onSetSpecialDay,
                        onNoteClicked = { showNoteDialogForDate = it },
                        onDayClick = { date ->
                            val hasContent = uiState.notes.containsKey(date.toString()) || uiState.specialDayEntries.containsKey(date)
                            if (hasContent) { selectedDateForDetails = if (selectedDateForDetails == date) null else date }
                        }
                    )
                }
            )
            if (selectedDateForDetails != null) {
                Spacer(modifier = Modifier.height(16.dp))
                selectedDateForDetails?.let { date ->
                    val note = uiState.notes[date.toString()]
                    val specialDayInstance = uiState.specialDayEntries[date]
                    val specialDayType = specialDayInstance?.let { inst -> uiState.customDayTypes.find { it.id == inst.typeId } }

                    if (note != null || specialDayType != null) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.label_details_for_date, date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    IconButton(onClick = { selectedDateForDetails = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(id = R.string.dialog_action_close))
                                    }
                                }

                                specialDayType?.let {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Column {
                                        Text(stringResource(id = R.string.label_day_mark), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(it.description, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            specialDayInstance?.reminderDateTime?.let {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(it.format(DateTimeFormatter.ofPattern("HH:mm")))
                                                }
                                            }
                                        }
                                    }
                                }

                                note?.let {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Column {
                                        Text(stringResource(id = R.string.label_note), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(it.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                                            it.reminderDateTime?.let {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(it.format(DateTimeFormatter.ofPattern("HH:mm")))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}