package com.example.pc1.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pc1.data.remote.FirebaseAuthManager
import com.example.pc1.presentation.auth.LoginScreen
import com.example.pc1.presentation.auth.RegisterScreen
import com.example.pc1.presentation.jugada.JugadaScreen
import com.example.pc1.presentation.seguimiento.SeguimientoScreen

@Composable
fun AppNavGraph(){
    val navController = rememberNavController()

    //Si la sesión sigue abierta al reabrir la app, se va directo a la jugada
    val startDestination = remember {
        if (FirebaseAuthManager.currentUid() != null) "jugada" else "login"
    }

    NavHost(navController = navController,
            startDestination = startDestination)
    {
        composable("register") { RegisterScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("jugada") {
            DrawerScaffold(navController) {
                JugadaScreen(navController)
            }
        }
        composable("seguimiento") {
            DrawerScaffold(navController) {
                SeguimientoScreen(navController)
            }
        }
    }

}
