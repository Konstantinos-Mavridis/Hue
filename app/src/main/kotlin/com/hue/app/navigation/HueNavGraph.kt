package com.hue.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hue.feature.capture.crop.CropScreen
import com.hue.feature.capture.ui.CaptureScreen
import com.hue.feature.history.ui.HistoryScreen
import com.hue.feature.matching.ui.ResultsScreen
import com.hue.app.ui.HomeScreen
import java.net.URLDecoder

@Composable
fun HueNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Home.route) {
            HomeScreen(
                onScanFabric  = { navController.navigate(Screen.Camera.route) },
                onViewHistory = { navController.navigate(Screen.History.route) }
            )
        }

        composable(Screen.Camera.route) {
            CaptureScreen(
                onImageReady = { path ->
                    navController.navigate(Screen.Crop.createRoute(path))
                }
            )
        }

        composable(
            route = Screen.Crop.route,
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStack ->
            val encoded = backStack.arguments?.getString("imagePath") ?: return@composable
            val path = URLDecoder.decode(encoded, "UTF-8")
            CropScreen(
                imagePath = path,
                onCropConfirmed = { croppedPath ->
                    navController.navigate(Screen.Results.createRoute(croppedPath)) {
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Results.route,
            arguments = listOf(navArgument("croppedPath") { type = NavType.StringType })
        ) { backStack ->
            val encoded = backStack.arguments?.getString("croppedPath") ?: return@composable
            val path = URLDecoder.decode(encoded, "UTF-8")
            ResultsScreen(
                croppedImagePath = path,
                onBack = { navController.popBackStack() },
                onSaveComplete = { /* already handles internally */ },
                onAnalyseAnother = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onScanSelected = { scanId ->
                    // Navigate to detail — handled by showing results for history item
                    navController.navigate(Screen.ScanDetail.createRoute(scanId))
                }
            )
        }

        composable(
            route = Screen.ScanDetail.route,
            arguments = listOf(navArgument("scanId") { type = NavType.LongType })
        ) { backStack ->
            val scanId = backStack.arguments?.getLong("scanId") ?: return@composable
            HistoryScanDetailScreen(
                scanId = scanId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun HistoryScanDetailScreen(scanId: Long, onBack: () -> Unit) {
    // Placeholder — in a full implementation this would show the saved analysis
    // using GetScanByIdUseCase and the ResultsScreen in read-only mode.
    androidx.compose.material3.Text("Scan #$scanId")
}
