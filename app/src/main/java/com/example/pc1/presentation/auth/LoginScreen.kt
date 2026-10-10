package com.example.pc1.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pc1.data.remote.FirebaseAuthManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Iniciar Sesión", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = email,
            onValueChange = {email = it},
            placeholder = {Text("Correo electrónico")},
            label = {Text("Correo electrónico")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = {password = it},
            placeholder = {Text("Contraseña")},
            label = {Text("Contraseña")},
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                cargando = true
                scope.launch {
                    val result = FirebaseAuthManager.loginUser(email.trim(), password)
                    cargando = false
                    if(result.isSuccess)
                        navController.navigate("jugada") {
                            //Al volver atrás no se regresa al login
                            popUpTo("login") { inclusive = true }
                        }
                    else {
                        //Toast message
                        val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = email.isNotBlank() && password.isNotBlank() && !cargando,
            modifier = Modifier.fillMaxWidth()
        ){
            Text(if (cargando) "Ingresando..." else "Ingresar")
        }

        TextButton(onClick = { navController.navigate("register") }) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}
