package com.example.carmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.carmanager.notifications.ReminderWorker
import com.example.carmanager.ui.CarDetailRoute
import com.example.carmanager.ui.CarFormRoute
import com.example.carmanager.ui.DocumentFormRoute
import com.example.carmanager.ui.HomeRoute
import com.example.carmanager.ui.MaintenanceFormRoute
import com.example.carmanager.ui.car.CarDetailScreen
import com.example.carmanager.ui.forms.CarFormScreen
import com.example.carmanager.ui.forms.DocumentFormScreen
import com.example.carmanager.ui.forms.MaintenanceFormScreen
import com.example.carmanager.ui.home.HomeScreen
import com.example.carmanager.ui.theme.CarManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // test notificări (doar debug): adb shell am start -n com.example.carmanager/.MainActivity --ez runReminders true
        if (BuildConfig.DEBUG && intent.getBooleanExtra("runReminders", false)) ReminderWorker.runNow(this)
        setContent {
            CarManagerTheme {
                RequestNotificationPermission()
                CarManagerNavHost()
            }
        }
    }
}

@Composable
private fun CarManagerNavHost() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onCarClick = { nav.navigate(CarDetailRoute(it)) },
                onAddCar = { nav.navigate(CarFormRoute()) },
            )
        }
        composable<CarDetailRoute> {
            CarDetailScreen(
                onBack = back,
                onEditCar = { nav.navigate(CarFormRoute(it)) },
                onAddDocument = { nav.navigate(DocumentFormRoute(it)) },
                onEditDocument = { car, doc -> nav.navigate(DocumentFormRoute(car, doc)) },
                onAddMaintenance = { nav.navigate(MaintenanceFormRoute(it)) },
                onEditMaintenance = { car, rec -> nav.navigate(MaintenanceFormRoute(car, rec)) },
            )
        }
        composable<CarFormRoute> { CarFormScreen(onBack = back) }
        composable<DocumentFormRoute> { DocumentFormScreen(onBack = back) }
        composable<MaintenanceFormRoute> { MaintenanceFormScreen(onBack = back) }
    }
}

/** Pe Android 13+ notificările trebuie cerute explicit utilizatorului. */
@Composable
private fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
