package com.medialert.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medialert.R
import com.medialert.presentation.navigation.PinPurpose
import com.medialert.utils.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToElderMode: () -> Unit,
    onNavigateToPinLock: (PinPurpose) -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        emergencyName = uiState.emergencyContactName
        emergencyPhone = uiState.emergencyContactPhone
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            SettingsSection(title = stringResource(R.string.settings_section_mode)) {
                // Current mode
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_user_mode)) },
                    supportingContent = {
                        Text(
                            if (uiState.userMode == AppConstants.MODE_MAYOR)
                                stringResource(R.string.mode_elder)
                            else
                                stringResource(R.string.mode_caretaker)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.userMode == AppConstants.MODE_MAYOR,
                            onCheckedChange = { isElder ->
                                if (isElder) {
                                    onNavigateToElderMode()
                                } else {
                                    onNavigateToPinLock(PinPurpose.SWITCH_MODE)
                                }
                            }
                        )
                    }
                )

                if (uiState.userMode == AppConstants.MODE_CUIDADOR) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_pin)) },
                        supportingContent = {
                            Text(if (uiState.pin != null) stringResource(R.string.pin_set) else stringResource(R.string.pin_not_set))
                        },
                        leadingContent = { Icon(Icons.Default.Lock, null) },
                        modifier = Modifier.clickable(onClick = { onNavigateToPinLock(PinPurpose.SET_PIN) })
                    )
                }
            }

            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.settings_section_emergency)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_emergency_contact)) },
                    supportingContent = {
                        if (emergencyName.isNotBlank()) Text("$emergencyName · $emergencyPhone")
                        else Text(stringResource(R.string.emergency_not_set))
                    },
                    leadingContent = { Icon(Icons.Default.ContactPhone, null) },
                    modifier = Modifier.clickable(onClick = { showEmergencyDialog = true })
                )
            }

            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_theme)) },
                        supportingContent = {
                            Text(
                                when (uiState.theme) {
                                    AppConstants.THEME_LIGHT -> stringResource(R.string.theme_light)
                                    AppConstants.THEME_DARK -> stringResource(R.string.theme_dark)
                                    else -> stringResource(R.string.theme_system)
                                }
                            )
                        },
                        leadingContent = { Icon(Icons.Default.Palette, null) },
                        modifier = Modifier.clickable(onClick = { showThemeMenu = true })
                    )
                    DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_system)) },
                            onClick = { viewModel.setTheme(AppConstants.THEME_SYSTEM); showThemeMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_light)) },
                            onClick = { viewModel.setTheme(AppConstants.THEME_LIGHT); showThemeMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_dark)) },
                            onClick = { viewModel.setTheme(AppConstants.THEME_DARK); showThemeMenu = false }
                        )
                    }
                }
            }

            HorizontalDivider()

            SettingsSection(title = stringResource(R.string.settings_section_account)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_logout)) },
                    leadingContent = { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable(onClick = { showLogoutConfirm = true })
                )
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    showLogoutConfirm = false
                    onLogout()
                }) { Text(stringResource(R.string.action_confirm_logout), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text(stringResource(R.string.settings_emergency_contact)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emergencyName,
                        onValueChange = { emergencyName = it },
                        label = { Text(stringResource(R.string.field_name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = emergencyPhone,
                        onValueChange = { emergencyPhone = it },
                        label = { Text(stringResource(R.string.field_phone)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setEmergencyContact(emergencyName, emergencyPhone)
                    showEmergencyDialog = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

private fun Modifier.clickable(onClick: () -> Unit): Modifier =
    this.then(Modifier.fillMaxWidth()).let { m ->
        androidx.compose.ui.Modifier.clickable(onClick = onClick)
    }
