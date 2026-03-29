package com.medialert.presentation.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medialert.R
import com.medialert.domain.model.*
import com.medialert.presentation.navigation.ScanMode
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationAddScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScan: (ScanMode) -> Unit,
    onSaved: () -> Unit,
    addViewModel: MedicationAddViewModel = hiltViewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var principioActivo by remember { mutableStateOf("") }
    var dosisAmount by remember { mutableStateOf("1") }
    var dosisUnit by remember { mutableStateOf("mg") }
    var stockActual by remember { mutableStateOf("30") }
    var stockMinimo by remember { mutableStateOf("5") }
    var instrucciones by remember { mutableStateOf("") }
    var formaFarmaceutica by remember { mutableStateOf(FormaFarmaceutica.PASTILLA) }
    var showFormaMenu by remember { mutableStateOf(false) }
    var isFormValid by remember { mutableStateOf(false) }

    LaunchedEffect(nombre, dosisAmount) {
        isFormValid = nombre.isNotBlank() && (dosisAmount.toDoubleOrNull() ?: 0.0) > 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_medication_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Scan buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onNavigateToScan(ScanMode.PRESCRIPTION) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DocumentScanner, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.scan_prescription), maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onNavigateToScan(ScanMode.BOX) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.scan_box), maxLines = 1)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text(stringResource(R.string.field_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = nombre.isBlank(),
                singleLine = true
            )

            OutlinedTextField(
                value = principioActivo,
                onValueChange = { principioActivo = it },
                label = { Text(stringResource(R.string.field_active_ingredient)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Forma farmacéutica
            ExposedDropdownMenuBox(
                expanded = showFormaMenu,
                onExpandedChange = { showFormaMenu = it }
            ) {
                OutlinedTextField(
                    value = formaFarmaceutica.key,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.field_form)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showFormaMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = showFormaMenu, onDismissRequest = { showFormaMenu = false }) {
                    FormaFarmaceutica.entries.forEach { forma ->
                        DropdownMenuItem(
                            text = { Text(forma.key) },
                            onClick = { formaFarmaceutica = forma; showFormaMenu = false }
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dosisAmount,
                    onValueChange = { dosisAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.field_dose)) },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dosisUnit,
                    onValueChange = { dosisUnit = it.take(10) },
                    label = { Text(stringResource(R.string.field_unit)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = stockActual,
                    onValueChange = { stockActual = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.field_stock)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = stockMinimo,
                    onValueChange = { stockMinimo = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.field_min_stock)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = instrucciones,
                onValueChange = { instrucciones = it },
                label = { Text(stringResource(R.string.field_instructions)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val medication = Medication(
                        id = UUID.randomUUID().toString(),
                        nombre = nombre.trim(),
                        principioActivo = principioActivo.trim().ifBlank { null },
                        formaFarmaceutica = formaFarmaceutica,
                        fotoPath = null,
                        dosisAmount = dosisAmount.toDoubleOrNull() ?: 1.0,
                        dosisUnit = dosisUnit.trim(),
                        stockActual = stockActual.toIntOrNull() ?: 0,
                        stockMinimo = stockMinimo.toIntOrNull() ?: 5,
                        urlProspecto = null,
                        instrucciones = instrucciones.trim().ifBlank { null },
                        estado = MedicationEstado.ACTIVO,
                        registroFotoActivo = false,
                        fechaCreacion = System.currentTimeMillis()
                    )
                    addViewModel.saveMedication(medication)
                    onSaved()
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
