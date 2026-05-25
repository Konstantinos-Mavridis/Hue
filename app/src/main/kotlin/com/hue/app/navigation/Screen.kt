package com.hue.app.navigation

import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Home    : Screen("home")
    object Camera  : Screen("camera")
    object Crop    : Screen("crop/{imagePath}") {
        fun createRoute(imagePath: String) =
            "crop/${URLEncoder.encode(imagePath, "UTF-8")}"
    }
    object Results : Screen("results/{croppedPath}") {
        fun createRoute(croppedPath: String) =
            "results/${URLEncoder.encode(croppedPath, "UTF-8")}"
    }
    object History : Screen("history")
    object ScanDetail : Screen("scan/{scanId}") {
        fun createRoute(scanId: Long) = "scan/$scanId"
    }
}
