package com.medialert.presentation.elder

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.medialert.R
import com.medialert.domain.model.DoseEstado
import com.medialert.domain.model.TodayDose
import com.medialert.presentation.today.TodayViewModel
import com.medialert.utils.DateUtils

/**
 * Elder Mode — ultra-simplified UI for older adults.
 * Large fonts, big buttons, minimal navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElderModeScreen(
    onEmergencyCall: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var emergencyPhone by remember { mutableStateOf<String?>(null) }

    // Load emergency phone from preferences if needed
    // (simplified: passed via SettingsViewModel in a real integration)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.today_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            // Emergency button
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        val phone = emergencyPhone
                        if (!phone.isNullOrBlank()) {
                            context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Call, null, Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.elder_emergency), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(64.dp))
            }
            return@Scaffold
        }

        val pendingDoses = uiState.doses.filter { it.doseLog.estado == DoseEstado.PENDIENTE }

        if (pendingDoses.isEmpty()) {
            // All doses done
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.elder_all_done),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.elder_pending, pendingDoses.size),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(pendingDoses) { dose ->
                    ElderDoseCard(
                        dose = dose,
                        onTaken = { viewModel.markTaken(dose.doseLog.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ElderDoseCard(dose: TodayDose, onTaken: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = dose.medication.fotoPath ?: R.drawable.ic_pill,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(dose.medication.nombre, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${dose.medication.dosisAmount} ${dose.medication.dosisUnit}",
                        fontSize = 20.sp
                    )
                    Text(
                        DateUtils.formatTime(dose.doseLog.fechaProgramada),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            dose.medication.instrucciones?.let { ins ->
                Spacer(Modifier.height(8.dp))
                Text(ins, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp))

            // Main action button
            Button(
                onClick = onTaken,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Check, null, Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.elder_taken), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
