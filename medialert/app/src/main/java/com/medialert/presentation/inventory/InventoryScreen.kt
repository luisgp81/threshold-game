package com.medialert.presentation.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.medialert.domain.model.Medication
import com.medialert.domain.model.MedicationEstado
import com.medialert.presentation.navigation.ScanMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToScan: (ScanMode) -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.inventory_title)) })
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showAddMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add_medication))
                }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.scan_prescription)) },
                        onClick = { showAddMenu = false; onNavigateToScan(ScanMode.PRESCRIPTION) },
                        leadingIcon = { Icon(Icons.Default.DocumentScanner, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.scan_box)) },
                        onClick = { showAddMenu = false; onNavigateToScan(ScanMode.BOX) },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_manual)) },
                        onClick = { showAddMenu = false; onNavigateToAdd() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            // Filter chips
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                FilterChip(
                    selected = uiState.filter == MedicationEstado.ACTIVO,
                    onClick = { viewModel.setFilter(MedicationEstado.ACTIVO) },
                    label = { Text(stringResource(R.string.filter_active)) }
                )
                FilterChip(
                    selected = uiState.filter == MedicationEstado.PAUSADO,
                    onClick = { viewModel.setFilter(MedicationEstado.PAUSADO) },
                    label = { Text(stringResource(R.string.filter_paused)) }
                )
                FilterChip(
                    selected = uiState.filter == MedicationEstado.FINALIZADO,
                    onClick = { viewModel.setFilter(MedicationEstado.FINALIZADO) },
                    label = { Text(stringResource(R.string.filter_finished)) }
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.medications.isEmpty()) {
                EmptyInventory(onNavigateToAdd)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.medications, key = { it.id }) { medication ->
                        MedicationCard(
                            medication = medication,
                            onClick = { onNavigateToDetail(medication.id) },
                            onArchive = { viewModel.archiveMedication(medication.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationCard(
    medication: Medication,
    onClick: () -> Unit,
    onArchive: () -> Unit
) {
    val isLowStock = medication.stockActual <= medication.stockMinimo

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = medication.fotoPath ?: R.drawable.ic_pill,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(medication.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                medication.principioActivo?.let { pa ->
                    Text(pa, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLowStock) {
                        Icon(Icons.Default.Warning, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = stringResource(R.string.stock_units, medication.stockActual),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Status badge
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        when (medication.estado) {
                            MedicationEstado.ACTIVO -> stringResource(R.string.filter_active)
                            MedicationEstado.PAUSADO -> stringResource(R.string.filter_paused)
                            MedicationEstado.FINALIZADO -> stringResource(R.string.filter_finished)
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyInventory(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Medication, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.inventory_empty_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd) { Text(stringResource(R.string.action_add_medication)) }
    }
}
