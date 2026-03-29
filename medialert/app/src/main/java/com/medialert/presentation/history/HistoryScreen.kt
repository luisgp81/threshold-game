package com.medialert.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.medialert.R
import com.medialert.domain.model.DoseEstado
import com.medialert.domain.model.DoseLog
import com.medialert.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedDoses: LazyPagingItems<DoseLog> = viewModel.pagedDoseLogs.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            // Medication filter chips
            if (uiState.medications.isNotEmpty()) {
                ScrollableFilterRow(
                    medications = uiState.medications.map { it.id to it.nombre },
                    selectedId = uiState.selectedMedicationId,
                    onSelect = { viewModel.selectMedication(it) }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(pagedDoses.itemCount) { index ->
                    val dose = pagedDoses[index]
                    if (dose != null) {
                        DoseHistoryItem(dose)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollableFilterRow(
    medications: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String?) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedId == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.filter_all)) }
            )
        }
        items(medications.size) { i ->
            val (id, name) = medications[i]
            FilterChip(
                selected = selectedId == id,
                onClick = { onSelect(id) },
                label = { Text(name, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun DoseHistoryItem(dose: DoseLog) {
    val (statusColor, statusIcon, statusText) = when (dose.estado) {
        DoseEstado.TOMADA -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "Tomada")
        DoseEstado.TOMADA_TARDE -> Triple(Color(0xFFFF9800), Icons.Default.CheckCircle, "Tomada tarde")
        DoseEstado.OMITIDA -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Cancel, "Omitida")
        DoseEstado.PENDIENTE -> Triple(MaterialTheme.colorScheme.outline, Icons.Default.HourglassEmpty, "Pendiente")
    }

    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(statusIcon, null, Modifier.size(24.dp), tint = statusColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    DateUtils.formatDateTime(dose.fechaProgramada),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                dose.fechaTomada?.let { taken ->
                    Text(
                        "Tomada: ${DateUtils.formatTime(taken)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)
        }
    }
}
