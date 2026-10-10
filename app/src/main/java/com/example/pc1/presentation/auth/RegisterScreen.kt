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
fun RegisterScreen(navController: NavController)
{
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val camposCompletos = name.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && confirmPassword.isNotBlank()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registro de usuario", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = name,
            onValueChange = {name = it},
            placeholder = {Text("Nombre Completo")},
            label = {Text("Nombre Completo")},
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = {email = it},
            placeholder = {Text("Correo")},
            label = {Text("Correo")},
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = {password = it},
            placeholder = {Text("Contraseña (mínimo 6 caracteres)")},
            label = {Text("Contraseña")},
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {confirmPassword = it},
            placeholder = {Text("Confirmar Contraseña")},
            label = {Text("Confirmar Contraseña")},
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = confirmPassword.isNotEmpty() && password != confirmPassword,
            supportingText = {
                if (confirmPassword.isNotEmpty() && password != confirmPassword)
                    Text("Las contraseñas no coinciden")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            enabled = camposCompletos && password == confirmPassword && !cargando,
            onClick = {
                cargando = true
                scope.launch {
                    val result = FirebaseAuthManager.registerUser(name.trim(), email.trim(), password)
                    cargando = false
                    if(result.isSuccess) {
                        //Firebase inicia sesión al crear la cuenta; se cierra para ingresar desde el login
                        FirebaseAuthManager.logout()
                        Toast.makeText(context, "Cuenta creada, inicia sesión", Toast.LENGTH_SHORT).show()
                        navController.popBackStack("login", inclusive = false)
                    }
                    else {
                        //Toast message
                        val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        {
            Text(if (cargando) "Registrando..." else "Registrar")
        }

        TextButton(onClick = { navController.popBackStack("login", inclusive = false) }) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}
