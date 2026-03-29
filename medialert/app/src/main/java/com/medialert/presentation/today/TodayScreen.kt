package com.medialert.presentation.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.medialert.R
import com.medialert.domain.model.DoseEstado
import com.medialert.domain.model.TodayDose
import com.medialert.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onNavigateToInventory: () -> Unit,
    onNavigateToMedicationDetail: (String) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            viewModel.refresh()
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.today_title)) },
                actions = {
                    IconButton(onClick = onNavigateToInventory) {
                        Icon(Icons.Default.Medication, contentDescription = stringResource(R.string.inventory_title))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            if (uiState.isLoading && uiState.doses.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (uiState.doses.isEmpty()) {
                EmptyTodayContent(onNavigateToInventory)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        DayProgressCard(
                            completed = uiState.completedDoses,
                            total = uiState.totalDoses,
                            progress = uiState.progressFraction
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(uiState.doses, key = { it.doseLog.id }) { todayDose ->
                        DoseCard(
                            todayDose = todayDose,
                            onTaken = { viewModel.markTaken(todayDose.doseLog.id) },
                            onSkip = { viewModel.markSkipped(todayDose.doseLog.id) },
                            onClick = { onNavigateToMedicationDetail(todayDose.medication.id) }
                        )
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun DayProgressCard(completed: Int, total: Int, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.today_progress_title, completed, total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (progress == 1f) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoseCard(
    todayDose: TodayDose,
    onTaken: () -> Unit,
    onSkip: () -> Unit,
    onClick: () -> Unit
) {
    val statusColor = when (todayDose.doseLog.estado) {
        DoseEstado.TOMADA, DoseEstado.TOMADA_TARDE -> Color(0xFF4CAF50)
        DoseEstado.OMITIDA -> MaterialTheme.colorScheme.error
        DoseEstado.PENDIENTE -> {
            val isOverdue = todayDose.doseLog.fechaProgramada < System.currentTimeMillis()
            if (isOverdue) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        }
    }
    val isPending = todayDose.doseLog.estado == DoseEstado.PENDIENTE

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator bar
            Box(
                Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(statusColor)
            )
            Spacer(Modifier.width(12.dp))

            // Medication photo
            AsyncImage(
                model = todayDose.medication.fotoPath ?: R.drawable.ic_pill,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))

            // Info
            Column(Modifier.weight(1f)) {
                Text(todayDose.medication.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${todayDose.medication.dosisAmount} ${todayDose.medication.dosisUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    DateUtils.formatTime(todayDose.doseLog.fechaProgramada),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }

            // Actions (only for pending doses)
            if (isPending) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledIconButton(
                        onClick = onTaken,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_taken))
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedIconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_skip), Modifier.size(16.dp))
                    }
                }
            } else {
                Icon(
                    imageVector = when (todayDose.doseLog.estado) {
                        DoseEstado.TOMADA, DoseEstado.TOMADA_TARDE -> Icons.Default.CheckCircle
                        DoseEstado.OMITIDA -> Icons.Default.Cancel
                        else -> Icons.Default.HourglassEmpty
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyTodayContent(onNavigateToInventory: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircleOutline, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.today_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.today_empty_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNavigateToInventory) {
            Text(stringResource(R.string.action_add_medication))
        }
    }
}
