package pl.dawidbujok.szychtownik

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

@Composable
fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    initialColor: Color
) {
    val controller = rememberColorPickerController()

    LaunchedEffect(initialColor) {
        controller.selectByColor(initialColor, false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.dialog_title_custom_color)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    controller = controller
                )
                Spacer(modifier = Modifier.height(16.dp))

                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    controller = controller
                )
                Spacer(modifier = Modifier.height(16.dp))

                AlphaSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    controller = controller
                )
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(controller.selectedColor.value) }) {
                Text(stringResource(id = R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        }
    )
}