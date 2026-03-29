package com.medialert.presentation.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.medialert.R
import com.medialert.domain.model.MedicationEstado
import com.medialert.presentation.navigation.ScanMode
import com.medialert.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailScreen(
    medicationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToScan: (ScanMode) -> Unit,
    viewModel: MedicationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val medication = uiState.medication ?: return

    var showEditStock by remember { mutableStateOf(false) }
    var newStockValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(medication.nombre) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToScan(ScanMode.BOX) }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.verify_box))
                    }
                    IconButton(onClick = { viewModel.archiveMedication() }) {
                        Icon(Icons.Default.Archive, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card with photo
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = medication.fotoPath ?: R.drawable.ic_pill,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(medication.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        medication.principioActivo?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        Text(
                            "${medication.dosisAmount} ${medication.dosisUnit} · ${medication.formaFarmaceutica.key}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Stock card
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.stock_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showEditStock = true; newStockValue = medication.stockActual.toString() }) {
                            Text(stringResource(R.string.action_edit))
                        }
                    }
                    Text(
                        stringResource(R.string.stock_units, medication.stockActual),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (medication.stockActual <= medication.stockMinimo) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                    uiState.daysRemaining?.let { days ->
                        Text(
                            stringResource(R.string.stock_days_remaining, days, DateUtils.formatDate(DateUtils.stockEndDate(medication.stockActual, 1f))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        stringResource(R.string.stock_minimum, medication.stockMinimo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Schedules card
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.schedule_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (uiState.schedules.isEmpty()) {
                        Text(stringResource(R.string.no_schedule), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        uiState.schedules.forEach { schedule ->
                            Text(
                                "• ${schedule.tipo.key}: ${schedule.horarios.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Instructions
            medication.instrucciones?.let { instructions ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.instructions_label), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(instructions, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Prospectus button
            medication.urlProspecto?.let { url ->
                OutlinedButton(
                    onClick = { /* Navigate to WebView */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInBrowser, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.view_prospectus))
                }
            }
        }
    }

    if (showEditStock) {
        AlertDialog(
            onDismissRequest = { showEditStock = false },
            title = { Text(stringResource(R.string.edit_stock_title)) },
            text = {
                OutlinedTextField(
                    value = newStockValue,
                    onValueChange = { newStockValue = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.stock_label)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    newStockValue.toIntOrNull()?.let { viewModel.updateStock(it) }
                    showEditStock = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditStock = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
