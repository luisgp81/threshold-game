package com.medialert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.medialert.data.repository.UserPreferencesRepository
import com.medialert.presentation.navigation.MediAlertNavGraph
import com.medialert.presentation.navigation.NavRoutes
import com.medialert.ui.theme.MediAlertTheme
import com.medialert.utils.AppConstants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsRepository: UserPreferencesRepository
    @Inject lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Determine start destination
        val isLoggedIn = firebaseAuth.currentUser != null
        val userMode = runBlocking { prefsRepository.userMode.first() }

        val startDestination = when {
            !isLoggedIn -> NavRoutes.AUTH
            userMode == AppConstants.MODE_MAYOR -> NavRoutes.ELDER_MODE
            else -> NavRoutes.TODAY
        }

        setContent {
            val theme by prefsRepository.theme.collectAsState(initial = AppConstants.THEME_SYSTEM)
            val darkTheme = when (theme) {
                AppConstants.THEME_DARK -> true
                AppConstants.THEME_LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            MediAlertTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                MediAlertNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
