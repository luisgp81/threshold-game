package com.medialert.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.medialert.presentation.auth.AuthScreen
import com.medialert.presentation.elder.ElderModeScreen
import com.medialert.presentation.history.HistoryScreen
import com.medialert.presentation.inventory.InventoryScreen
import com.medialert.presentation.inventory.MedicationDetailScreen
import com.medialert.presentation.inventory.MedicationAddScreen
import com.medialert.presentation.scan.ScanScreen
import com.medialert.presentation.settings.PinLockScreen
import com.medialert.presentation.settings.SettingsScreen
import com.medialert.presentation.today.TodayScreen

@Composable
fun MediAlertNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(NavRoutes.TODAY) {
                        popUpTo(NavRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.TODAY) {
            TodayScreen(
                onNavigateToInventory = { navController.navigate(NavRoutes.INVENTORY) },
                onNavigateToMedicationDetail = { id ->
                    navController.navigate(NavRoutes.medicationDetail(id))
                }
            )
        }

        composable(NavRoutes.INVENTORY) {
            InventoryScreen(
                onNavigateToDetail = { id ->
                    navController.navigate(NavRoutes.medicationDetail(id))
                },
                onNavigateToAdd = { navController.navigate(NavRoutes.MEDICATION_ADD) },
                onNavigateToScan = { mode ->
                    navController.navigate(NavRoutes.scan(mode))
                }
            )
        }

        composable(
            route = NavRoutes.MEDICATION_DETAIL,
            arguments = listOf(navArgument("medicationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getString("medicationId") ?: return@composable
            MedicationDetailScreen(
                medicationId = medicationId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScan = { mode ->
                    navController.navigate(NavRoutes.scan(mode))
                }
            )
        }

        composable(NavRoutes.MEDICATION_ADD) {
            MedicationAddScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScan = { mode ->
                    navController.navigate(NavRoutes.scan(mode))
                },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.SCAN,
            arguments = listOf(navArgument("scanMode") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeStr = backStackEntry.arguments?.getString("scanMode") ?: "BOX"
            val mode = ScanMode.valueOf(modeStr)
            ScanScreen(
                scanMode = mode,
                onNavigateBack = { navController.popBackStack() },
                onScanComplete = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ELDER_MODE) {
            ElderModeScreen(
                onEmergencyCall = { /* handled in screen */ }
            )
        }

        composable(NavRoutes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateToElderMode = { navController.navigate(NavRoutes.ELDER_MODE) },
                onNavigateToPinLock = { purpose ->
                    navController.navigate(NavRoutes.pinLock(purpose))
                },
                onLogout = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.PIN_LOCK,
            arguments = listOf(navArgument("purpose") { type = NavType.StringType })
        ) { backStackEntry ->
            val purposeStr = backStackEntry.arguments?.getString("purpose") ?: "UNLOCK_SETTINGS"
            val purpose = PinPurpose.valueOf(purposeStr)
            PinLockScreen(
                purpose = purpose,
                onSuccess = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
