package jp.uhimania.qrreader.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.uhimania.qrreader.ui.NavigationRoutes.READER_ROUTE
import jp.uhimania.qrreader.ui.NavigationRoutes.SCANNED_LIST_ROUTE
import jp.uhimania.qrreader.ui.NavigationViews.READER_SCREEN
import jp.uhimania.qrreader.ui.NavigationViews.SCANNED_LIST_SCREEN

object NavigationViews {
    const val SCANNED_LIST_SCREEN = "scanned_list"
    const val READER_SCREEN = "reader"
}

object NavigationRoutes {
    const val SCANNED_LIST_ROUTE = SCANNED_LIST_SCREEN
    const val READER_ROUTE = READER_SCREEN
}

@Composable
fun QRReaderNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = SCANNED_LIST_ROUTE,
        modifier = modifier
    ) {
        composable(SCANNED_LIST_ROUTE) {
            ScannedListScreen(
                onStartScanning = { navController.navigate(READER_ROUTE) }
            )
        }
        composable(READER_ROUTE) {
            QRReaderScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
