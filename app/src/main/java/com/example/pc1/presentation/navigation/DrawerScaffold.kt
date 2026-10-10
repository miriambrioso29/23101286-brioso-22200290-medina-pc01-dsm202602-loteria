package com.example.pc1.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pc1.data.remote.FirebaseAuthManager
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScaffold(navController: NavController,
                   content: @Composable () -> Unit)
{
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    //Cierra el drawer y navega dejando "jugada" como base del historial
    fun irA(ruta: String) {
        scope.launch { drawerState.close() }
        navController.navigate(ruta) {
            popUpTo("jugada")
            launchSingleTop = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Menu Principal", modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium)
                Text(FirebaseAuthManager.currentEmail(), modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                //NavigationDrawerItem
                NavigationDrawerItem(
                    label = { Text("Mi jugada")},
                    selected = false,
                    onClick = { irA("jugada") }
                )
                NavigationDrawerItem(
                    label = { Text("Seguimiento del sorteo")},
                    selected = false,
                    onClick = { irA("seguimiento") }
                )
                NavigationDrawerItem(
                    label = { Text("Cerrar sesión")},
                    selected = false,
                    onClick = {
                        FirebaseAuthManager.logout()
                        //Limpia todo el historial para no volver a pantallas privadas
                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }

    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {Text("Lotería PC1")},
                    navigationIcon = {
                        IconButton(
                            onClick =  {
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    })
            }
        ) {
            paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)){
                content()
            }
        }
    }
}
