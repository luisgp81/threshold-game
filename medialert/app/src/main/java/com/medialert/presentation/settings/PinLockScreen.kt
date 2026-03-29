package com.medialert.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medialert.R
import com.medialert.presentation.navigation.PinPurpose
import com.medialert.utils.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    purpose: PinPurpose,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when (purpose) {
        PinPurpose.UNLOCK_SETTINGS -> stringResource(R.string.pin_enter_title)
        PinPurpose.SET_PIN -> stringResource(R.string.pin_set_title)
        PinPurpose.SWITCH_MODE -> stringResource(R.string.pin_switch_title)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))

            Text(
                if (purpose == PinPurpose.SET_PIN && isConfirmStep)
                    stringResource(R.string.pin_confirm_prompt)
                else title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // PIN dots
            val currentPin = if (isConfirmStep) confirmPin else pin
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(AppConstants.PIN_LENGTH) { index ->
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (index < currentPin.length) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(20.dp)
                    ) {}
                }
            }

            Spacer(Modifier.height(16.dp))

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            // Number pad
            val digits = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
            val gridColumns = 3

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                digits.chunked(gridColumns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { digit ->
                            if (digit.isEmpty()) {
                                Spacer(Modifier.size(80.dp))
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        error = null
                                        if (digit == "⌫") {
                                            if (isConfirmStep && confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                                            else if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        } else {
                                            val target = if (isConfirmStep) confirmPin else pin
                                            if (target.length < AppConstants.PIN_LENGTH) {
                                                if (isConfirmStep) confirmPin += digit else pin += digit
                                            }
                                            // Auto-confirm when PIN_LENGTH reached
                                            val newTarget = if (isConfirmStep) confirmPin + (if (digit != "⌫") digit else "") else pin + (if (digit != "⌫") digit else "")
                                            if (newTarget.length == AppConstants.PIN_LENGTH) {
                                                when (purpose) {
                                                    PinPurpose.UNLOCK_SETTINGS, PinPurpose.SWITCH_MODE -> {
                                                        if (viewModel.verifyPin(newTarget)) onSuccess()
                                                        else {
                                                            pin = ""
                                                            error = "PIN incorrecto"
                                                        }
                                                    }
                                                    PinPurpose.SET_PIN -> {
                                                        if (!isConfirmStep) {
                                                            isConfirmStep = true
                                                        } else {
                                                            if (pin == newTarget) {
                                                                viewModel.setPin(pin)
                                                                onSuccess()
                                                            } else {
                                                                pin = ""; confirmPin = ""; isConfirmStep = false
                                                                error = "Los PINs no coinciden"
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Text(digit, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
