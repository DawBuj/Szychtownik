package pl.dawidbujok.szychtownik

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun CustomDayTypeItem(
    dayType: CustomDayType,
    canBeDeleted: Boolean,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(dayType.color),
                contentAlignment = Alignment.Center
            ) {
                Text(dayType.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(dayType.description)
                if (dayType.yearlyLimit > 0) {
                    Text(
                        text = stringResource(id = R.string.label_yearly_limit) + ": ${dayType.yearlyLimit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEditClicked) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.icon_description_edit))
            }
            if (canBeDeleted) {
                IconButton(onClick = onDeleteClicked) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CustomDayTypeEditDialog(
    dayType: CustomDayType?,
    onDismiss: () -> Unit,
    onSave: (CustomDayType) -> Unit
) {
    var code by remember { mutableStateOf(dayType?.code ?: "") }
    var description by remember { mutableStateOf(dayType?.description ?: "") }
    var selectedColor by remember { mutableStateOf(dayType?.color ?: Color.Transparent) }
    var hasReminder by remember { mutableStateOf(dayType?.hasReminder ?: false) }
    
    // Nowe: Obsługa limitu jako opcji
    var isLimitEnabled by remember { mutableStateOf((dayType?.yearlyLimit ?: 0) > 0) }
    var yearlyLimitString by remember { mutableStateOf(if ((dayType?.yearlyLimit ?: 0) > 0) dayType?.yearlyLimit?.toString() ?: "" else "") }
    
    var showColorPicker by remember { mutableStateOf(false) }

    val isFormValid = code.isNotBlank() && description.isNotBlank() && code.length <= 2 && selectedColor != Color.Transparent

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (dayType == null) stringResource(id = R.string.dialog_title_new_day_type) else stringResource(id = R.string.dialog_title_edit_day_type)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 2) code = it },
                    label = { Text(stringResource(id = R.string.label_code_max_chars)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.label_description)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(4.dp))
                
                // Przełącznik dla rejestru/limitu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { isLimitEnabled = !isLimitEnabled }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isLimitEnabled, onCheckedChange = { isLimitEnabled = it })
                    Text("Ustaw pulę wolnego / urlopu", style = MaterialTheme.typography.bodyMedium)
                }

                if (isLimitEnabled) {
                    OutlinedTextField(
                        value = yearlyLimitString,
                        onValueChange = { if (it.all { char -> char.isDigit() }) yearlyLimitString = it },
                        label = { Text(stringResource(id = R.string.label_yearly_limit)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { showColorPicker = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.label_color))
                    Box(Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { hasReminder = !hasReminder }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = hasReminder, onCheckedChange = { hasReminder = it })
                    Text(stringResource(id = R.string.label_remind_on_add), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = if (isLimitEnabled) (yearlyLimitString.toIntOrNull() ?: 0) else 0
                    val finalType = dayType?.copy(
                        code = code,
                        description = description,
                        color = selectedColor,
                        hasReminder = hasReminder,
                        yearlyLimit = limit
                    ) ?: CustomDayType(
                        code = code,
                        description = description,
                        color = selectedColor,
                        hasReminder = hasReminder,
                        yearlyLimit = limit
                    )
                    onSave(finalType)
                },
                enabled = isFormValid
            ) {
                Text(stringResource(id = R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )

    if (showColorPicker) {
        ColorChoiceDialog(
            code = code,
            initialColor = selectedColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { 
                selectedColor = it
                showColorPicker = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorChoiceDialog(
    code: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var showAdvancedPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_title_choose_color)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    specialDayColorOptions.forEach { colorOption ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colorOption)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onColorSelected(colorOption) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { showAdvancedPicker = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brush, contentDescription = stringResource(id = R.string.icon_description_custom_color), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.dialog_title_custom_color))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.dialog_action_close)) }
        }
    )

    if (showAdvancedPicker) {
        LibraryColorPickerDialog(
            code = code,
            initialColor = initialColor,
            onDismiss = { showAdvancedPicker = false },
            onColorSelected = { 
                onColorSelected(it)
                showAdvancedPicker = false
             }
        )
    }
}

@Composable
fun LibraryColorPickerDialog(
    code: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val controller = rememberColorPickerController()
    
    LaunchedEffect(initialColor) {
        if (initialColor != Color.Transparent) {
            controller.selectByColor(initialColor, fromUser = false)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_title_custom_color)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    controller = controller,
                    onColorChanged = {}
                )
                Spacer(modifier = Modifier.height(16.dp))
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp),
                    controller = controller,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(controller.selectedColor.value)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(controller.selectedColor.value) }
            ) {
                Text(stringResource(id = R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        }
    )
}

@Composable
fun SpecialDayReminderDialog(
    dayType: CustomDayType,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime?) -> Unit
) {
    var reminderTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    val context = LocalContext.current

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute -> 
            reminderTime = LocalTime.of(hour, minute)
            onConfirm(reminderTime)
        },
        reminderTime.hour,
        reminderTime.minute,
        true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_title_set_reminder)) },
        text = { Text(stringResource(id = R.string.prompt_special_day_reminder, dayType.description)) },
        confirmButton = {
            Button(onClick = { timePickerDialog.show() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(id = R.string.action_choose_hour))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) {
                Text(stringResource(id = R.string.action_skip))
            }
        }
    )
}
