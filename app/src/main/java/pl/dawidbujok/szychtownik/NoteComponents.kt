package pl.dawidbujok.szychtownik

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun NoteEditDialog(
    date: LocalDate,
    initialNote: Note?,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit,
    onDelete: () -> Unit
) {
    var noteContent by remember { mutableStateOf(initialNote?.content ?: "") }
    var hasReminder by remember { mutableStateOf(initialNote?.reminderDateTime != null) }
    var reminderTime by remember { mutableStateOf(initialNote?.reminderDateTime?.toLocalTime() ?: LocalTime.of(8, 0)) }
    val context = LocalContext.current

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            reminderTime = LocalTime.of(hourOfDay, minute)
        },
        reminderTime.hour,
        reminderTime.minute,
        true // 24-hour format
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_title_note_for_date, date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))) },
        text = {
            Column {
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text(stringResource(id = R.string.label_note_content)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = hasReminder,
                        onCheckedChange = { hasReminder = it }
                    )
                    Text(stringResource(id = R.string.label_remind))
                }
                if (hasReminder) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp)
                    ) {
                        Text(stringResource(id = R.string.label_hour, reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"))))
                        IconButton(onClick = { timePickerDialog.show() }) {
                            Icon(Icons.Default.AccessTime, contentDescription = stringResource(id = R.string.icon_description_set_time))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newNote = Note(
                    id = initialNote?.id ?: date.hashCode(),
                    content = noteContent,
                    reminderDateTime = if (hasReminder) reminderTime.atDate(date) else null
                )
                onSave(newNote)
            }) {
                Text(stringResource(id = R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (initialNote != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.action_delete))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        }
    )
}
