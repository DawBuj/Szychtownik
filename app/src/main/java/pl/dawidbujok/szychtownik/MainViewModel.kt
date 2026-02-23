package pl.dawidbujok.szychtownik

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

data class CalendarUiState(
    val selectedCompany: Company = availableCompanies.first(),
    val selectedBrigade: BrigadeConfig = selectedCompany.brigades.first(),
    val specialDayEntries: Map<LocalDate, SpecialDayInstance> = emptyMap(),
    val customDayTypes: List<CustomDayType> = emptyList(),
    val customStartDates: Map<String, LocalDate> = emptyMap(),
    val notes: Map<String, Note> = emptyMap(),
    val showSettingsScreen: Boolean = false,
    val showAboutDialog: Boolean = false,
    val specialDayForReminder: Pair<LocalDate, CustomDayType>? = null,
    val showNewFeaturesDialogForVersion: String? = null,
    val showExactAlarmPermissionDialog: Boolean = false,
    val showVacationManagementScreen: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)
    private val specialDaysDataStoreManager = SpecialDaysDataStoreManager(application)
    private val backupManager = BackupManager(application)
    private val notesDataStoreManager = NotesDataStoreManager(application)
    private val alarmScheduler = AlarmScheduler(application)
    private val tutorialManager = TutorialManager(application)

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _vacationStats = MutableStateFlow<List<VacationStat>>(emptyList())
    val vacationStats: StateFlow<List<VacationStat>> = _vacationStats.asStateFlow()

    init {
        viewModelScope.launch {
            val settingsFlow = combine(
                dataStoreManager.getSelectedCompanyId(),
                dataStoreManager.getSelectedBrigadeId(),
                dataStoreManager.getAllCustomStartDates(),
            ) { companyId, brigadeId, customDates ->
                Triple(companyId, brigadeId, customDates)
            }

            val dataFlow = combine(
                specialDaysDataStoreManager.getSpecialDayEntries(),
                specialDaysDataStoreManager.getCustomDayTypes(),
                notesDataStoreManager.getNotes()
            ) { specialEntries, customTypes, notes ->
                Triple(specialEntries, customTypes, notes)
            }

            combine(settingsFlow, dataFlow) { settings, data ->
                val (companyId, brigadeId, customDates) = settings
                val (specialEntries, customTypes, notes) = data

                val company = availableCompanies.find { it.id == companyId } ?: availableCompanies.first()
                val initialBrigade = company.brigades.find { it.id == brigadeId } ?: company.brigades.first()
                val finalBrigade = customDates[initialBrigade.id]?.let {
                    initialBrigade.copy(baseShiftCycleStartDate = it)
                } ?: initialBrigade

                _uiState.update {
                    it.copy(
                        selectedCompany = company,
                        selectedBrigade = finalBrigade,
                        customStartDates = customDates,
                        specialDayEntries = specialEntries,
                        customDayTypes = customTypes,
                        notes = notes
                    )
                }
            }.collect {}
        }

        viewModelScope.launch {
            uiState.map { state ->
                calculateVacationStats(state.customDayTypes, state.specialDayEntries)
            }.collect { stats ->
                _vacationStats.value = stats
            }
        }

        showTutorialIfNewVersion()
    }

    private fun calculateVacationStats(types: List<CustomDayType>, entries: Map<LocalDate, SpecialDayInstance>): List<VacationStat> {
        val today = LocalDate.now()
        val currentYear = today.year
        
        return types.filter { it.yearlyLimit > 0 }.map { dayType ->
            val entriesInYear = entries.filter { (date, instance) -> 
                instance.typeId == dayType.id && date.year == currentYear 
            }
            
            val used = entriesInYear.count { (date, _) -> date.isBefore(today) || date.isEqual(today) }
            val planned = entriesInYear.count { (date, _) -> date.isAfter(today) }

            VacationStat(
                dayType = dayType,
                used = used,
                planned = planned,
                limit = dayType.yearlyLimit
            )
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

    private fun showTutorialIfNewVersion() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentVersion = getAppVersionName(getApplication())
            if (tutorialManager.shouldShowTutorial(currentVersion)) {
                _uiState.update { it.copy(showNewFeaturesDialogForVersion = currentVersion) }
            }
        }
    }

    fun onNewFeaturesDialogDismissed() {
        viewModelScope.launch {
            _uiState.value.showNewFeaturesDialogForVersion?.let { version ->
                tutorialManager.markTutorialAsShown(version)
                _uiState.update { state -> state.copy(showNewFeaturesDialogForVersion = null) }
            }
        }
    }

    fun onBrigadeSelected(company: Company, brigade: BrigadeConfig) {
        val finalBrigade = _uiState.value.customStartDates[brigade.id]?.let { customDate ->
            brigade.copy(baseShiftCycleStartDate = customDate)
        } ?: brigade

        _uiState.update {
            it.copy(
                selectedCompany = company,
                selectedBrigade = finalBrigade
            )
        }

        viewModelScope.launch {
            dataStoreManager.saveSelection(company.id, brigade.id)
        }
    }

    fun onSaveBrigadeStartDate(brigadeId: String, date: LocalDate) {
        viewModelScope.launch {
            dataStoreManager.saveBrigadeStartDate(brigadeId, date)
        }
    }

    fun onSetSpecialDay(date: LocalDate, typeId: String?) {
        viewModelScope.launch {
            if (typeId == null) {
                _uiState.value.specialDayEntries[date]?.also { existingInstance ->
                    if (existingInstance.reminderDateTime != null) {
                        alarmScheduler.cancelSpecialDay(date, existingInstance.typeId)
                    }
                }
                specialDaysDataStoreManager.removeSpecialDay(date)
            } else {
                val dayType = _uiState.value.customDayTypes.find { it.id == typeId }
                if (dayType?.hasReminder == true) {
                    _uiState.update { it.copy(specialDayForReminder = Pair(date, dayType)) }
                } else {
                    val instance = SpecialDayInstance(typeId = typeId)
                    specialDaysDataStoreManager.setSpecialDay(date, instance)
                }
            }
        }
    }

    fun onSpecialDayReminderConfirmed(time: LocalTime?) {
        val reminderData = _uiState.value.specialDayForReminder ?: return
        val (date, dayType) = reminderData

        if (time != null && !alarmScheduler.canScheduleExactAlarms()) {
            _uiState.update { it.copy(showExactAlarmPermissionDialog = true, specialDayForReminder = null) }
            return
        }

        viewModelScope.launch {
            val instance = SpecialDayInstance(
                typeId = dayType.id,
                reminderDateTime = time?.atDate(date)
            )
            specialDaysDataStoreManager.setSpecialDay(date, instance)

            instance.reminderDateTime?.let {
                alarmScheduler.scheduleSpecialDay(instance, dayType, date)
            }
        }
        _uiState.update { it.copy(specialDayForReminder = null) }
    }

    fun onSaveCustomDayType(dayType: CustomDayType, isNew: Boolean) {
        viewModelScope.launch {
            val currentTypes = _uiState.value.customDayTypes
            val newTypes = if (isNew) {
                currentTypes + dayType
            } else {
                currentTypes.map { if (it.id == dayType.id) dayType else it }
            }
            specialDaysDataStoreManager.saveCustomDayTypes(newTypes)
        }
    }

    fun onDeleteCustomDayType(dayType: CustomDayType) {
        viewModelScope.launch {
            val updatedList = _uiState.value.customDayTypes.filterNot { it.id == dayType.id }
            specialDaysDataStoreManager.saveCustomDayTypes(updatedList)
            specialDaysDataStoreManager.removeAllEntriesForType(dayType.id)
        }
    }

    fun onSaveNote(date: LocalDate, note: Note) {
        if (note.reminderDateTime != null && !alarmScheduler.canScheduleExactAlarms()) {
            _uiState.update { it.copy(showExactAlarmPermissionDialog = true) }
            return
        }

        viewModelScope.launch {
            notesDataStoreManager.saveNote(date, note)
            alarmScheduler.schedule(note)
        }
    }

    fun onDeleteNote(date: LocalDate) {
        viewModelScope.launch {
            _uiState.value.notes[date.toString()]?.let { note ->
                alarmScheduler.cancel(note.id)
                notesDataStoreManager.deleteNote(date)
            }
        }
    }

    fun onShowSettings(show: Boolean) {
        _uiState.update { it.copy(showSettingsScreen = show) }
    }

    fun onShowAboutDialog(show: Boolean) {
        _uiState.update { it.copy(showAboutDialog = show) }
    }

    fun onShowVacationManagementScreen(show: Boolean) {
        _uiState.update { it.copy(showVacationManagementScreen = show) }
    }
    
    fun onExactAlarmPermissionDialogDismissed() {
        _uiState.update { it.copy(showExactAlarmPermissionDialog = false) }
    }

    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        backupManager.createBackupJson()
    }

    fun importBackup(jsonData: String) {
        viewModelScope.launch {
            backupManager.restoreBackup(jsonData)
        }
    }
}
