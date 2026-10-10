package com.example.pc1.presentation.jugada

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pc1.data.LoteriaCalculos
import com.example.pc1.data.model.JugadaModel
import com.example.pc1.data.model.SorteoModel
import com.example.pc1.data.remote.FirebaseAuthManager
import com.example.pc1.data.remote.LoteriaManager
import com.example.pc1.presentation.seguimiento.NumeroCirculo
import kotlinx.coroutines.launch

@Composable
fun JugadaScreen(navController: NavController){
    var sorteo by remember { mutableStateOf<SorteoModel?>(null) }
    var sorteoExiste by remember { mutableStateOf(true) }
    var jugada by remember { mutableStateOf<JugadaModel?>(null) }
    var cargandoJugada by remember { mutableStateOf(true) }
    var numeros by remember { mutableStateOf(List(LoteriaCalculos.CANTIDAD_NUMEROS) { "" }) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    //Escucha el estado del sorteo para bloquear el registro apenas inicie
    DisposableEffect(Unit) {
        val listener = LoteriaManager.sorteoRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Toast.makeText(context, e.message ?: "Error al leer el sorteo", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            sorteoExiste = snapshot != null && snapshot.exists()
            if (snapshot != null && snapshot.exists())
                sorteo = LoteriaManager.toSorteo(snapshot)
        }
        onDispose { listener.remove() }
    }

    //Recupera la jugada guardada del usuario de la sesión
    LaunchedEffect(Unit) {
        val result = LoteriaManager.getJugada()
        if (result.isSuccess)
            jugada = result.getOrNull()
        else {
            val mensaje = result.exceptionOrNull()?.message ?: "Error desconocido"
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
        cargandoJugada = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registro de jugada", style = MaterialTheme.typography.titleLarge)
        Text("Sesión: ${FirebaseAuthManager.currentEmail()}", style = MaterialTheme.typography.bodyMedium)

        //Datos del sorteo
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!sorteoExiste)
                    Text("No existe el sorteo \"${LoteriaManager.SORTEO_ID}\" en Firestore")
                else if (sorteo == null)
                    Text("Cargando sorteo...")
                else {
                    Text("Sorteo: ${sorteo?.nombre}", style = MaterialTheme.typography.titleMedium)
                    Text("Fecha: ${sorteo?.fecha}")
                    Text("Estado: ${sorteo?.estado}")
                }
            }
        }

        if (cargandoJugada) {
            Text("Cargando jugada...")
        }
        else if (jugada != null) {
            //Jugada ya registrada: solo lectura
            Text("Tu jugada registrada", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                jugada?.numeros?.forEach { numero ->
                    NumeroCirculo(numero, resaltado = false)
                }
            }
            Text("La jugada ya no se puede modificar.", style = MaterialTheme.typography.bodySmall)
        }
        else if (sorteo?.estado == "abierto") {
            Text(
                "Elige ${LoteriaCalculos.CANTIDAD_NUMEROS} números distintos del " +
                        "${LoteriaCalculos.NUMERO_MINIMO} al ${LoteriaCalculos.NUMERO_MAXIMO}",
                style = MaterialTheme.typography.titleMedium
            )

            //Dos filas de tres campos
            for (fila in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (columna in 0 until 3) {
                        val indice = fila * 3 + columna
                        OutlinedTextField(
                            value = numeros[indice],
                            onValueChange = { nuevo ->
                                if (nuevo.length <= 2 && nuevo.all { it.isDigit() }) {
                                    numeros = numeros.toMutableList().also { it[indice] = nuevo }
                                    error = null
                                }
                            },
                            label = { Text("N° ${indice + 1}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                enabled = !guardando,
                onClick = {
                    val mensajeError = LoteriaCalculos.validarJugada(numeros)
                    if (mensajeError != null) {
                        error = mensajeError
                        return@Button
                    }
                    guardando = true
                    scope.launch {
                        val numerosJugada = numeros.map { it.trim().toInt() }
                        val result = LoteriaManager.saveJugada(numerosJugada)
                        guardando = false
                        if (result.isSuccess) {
                            jugada = JugadaModel(
                                LoteriaManager.SORTEO_ID,
                                FirebaseAuthManager.currentUid() ?: "",
                                numerosJugada
                            )
                            Toast.makeText(context, "Jugada registrada", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            //Toast message
                            val mensaje = result.exceptionOrNull()?.message ?: "Error desconocido"
                            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (guardando) "Registrando..." else "Registrar jugada")
            }
        }
        else if (sorteo != null) {
            Text(
                "El sorteo ya inició. No se pueden registrar jugadas.",
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedButton(
            onClick = { navController.navigate("seguimiento") { launchSingleTop = true } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ver seguimiento del sorteo")
        }
    }
}
