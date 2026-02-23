package pl.dawidbujok.szychtownik

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun AppIconSelectionDialog(
    appIconManager: AppIconManager,
    onDismiss: () -> Unit
) {
    var selectedIcon by remember { mutableStateOf(appIconManager.getCurrentAppIcon()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_title_choose_app_icon)) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                AppIcon.entries.forEach { icon ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { 
                            selectedIcon = icon 
                            appIconManager.setAppIcon(icon)
                        }
                    ) {
                        Image(
                            painter = painterResource(
                                id = when (icon) {
                                    AppIcon.SZYCHTOWNIK -> R.mipmap.szychtownik_foreground
                                    AppIcon.KUŹNIA -> R.mipmap.koznia_foreground
                                    AppIcon.TEKSID1 -> R.mipmap.teksid1_foreground
                                }
                            ),
                            contentDescription = stringResource(id = R.string.icon_description_for_app, icon.name),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 2.dp, 
                                    color = if(selectedIcon == icon) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(icon.name.lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.dialog_action_close))
            }
        }
    )
}