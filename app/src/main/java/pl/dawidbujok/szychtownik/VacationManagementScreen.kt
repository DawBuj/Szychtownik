package pl.dawidbujok.szychtownik

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationManagementScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.vacationStats.collectAsState()
    val currentYear = LocalDate.now().year

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("${stringResource(id = R.string.title_vacations)} $currentYear") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (stats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Brak oznaczeń z ustawionym limitem.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stats) { stat ->
                    VacationStatItem(stat)
                }
            }
        }
    }
}

@Composable
fun VacationStatItem(stat: VacationStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = MaterialTheme.shapes.small,
                    color = stat.dayType.color
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stat.dayType.code,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stat.dayType.description,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            val progress = if (stat.limit > 0) stat.totalBooked.toFloat() / stat.limit.toFloat() else 0f
            
            LinearProgressIndicator(
                progress = { if (stat.limit > 0) progress.coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (progress > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Pierwszy rząd: Wykorzystano i Zaplanowano
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(id = R.string.label_used)} ${stat.used}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${stringResource(id = R.string.label_planned)} ${stat.planned}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            // Drugi rząd: Razem i Pozostało
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stringResource(id = R.string.label_total)} ${stat.totalBooked}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (stat.limit > 0) {
                    Text(
                        text = "${stringResource(id = R.string.label_remaining)} ${stat.remaining ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if ((stat.remaining ?: 0) < 0) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                }
            }
            
            if (stat.limit > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${stringResource(id = R.string.label_yearly_limit)}: ${stat.limit}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
