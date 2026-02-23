package pl.dawidbujok.szychtownik

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ChangelogContent() {
    Column {
        Text(
            stringResource(id = R.string.dialog_about_whats_new_content),
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
fun NewFeaturesDialog(
    versionName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_title_whats_new_version, versionName)) },
        text = {
            Column {
                Text(stringResource(id = R.string.text_new_features_intro), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                ChangelogContent()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_understood))
            }
        }
    )
}
