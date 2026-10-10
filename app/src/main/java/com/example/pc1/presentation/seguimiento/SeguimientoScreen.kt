package com.example.pc1.presentation.seguimiento

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pc1.data.LoteriaCalculos
import com.example.pc1.data.model.BolillaModel
import com.example.pc1.data.model.JugadaModel
import com.example.pc1.data.model.SorteoModel
import com.example.pc1.data.remote.LoteriaManager

@Composable
fun SeguimientoScreen(navController: NavController){
    var sorteo by remember { mutableStateOf<SorteoModel?>(null) }
    var bolillas by remember { mutableStateOf<List<BolillaModel>>(emptyList()) }
    var jugada by remember { mutableStateOf<JugadaModel?>(null) }
    var cargandoJugada by remember { mutableStateOf(true) }
    var conectado by remember { mutableStateOf(false) }

    val context = LocalContext.current

    //Escucha en tiempo real: estado del sorteo y bolillas publicadas.
    //Los listeners se quitan en onDispose al salir de la pantalla.
    DisposableEffect(Unit) {
        val sorteoListener = LoteriaManager.sorteoRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                conectado = false
                Toast.makeText(context, error.message ?: "Error al leer el sorteo", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists())
                sorteo = LoteriaManager.toSorteo(snapshot)
            conectado = true
        }

        val bolillasListener = LoteriaManager.bolillasRef
            .orderBy("orden")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    conectado = false
                    return@addSnapshotListener
                }
                if (snapshot != null)
                    bolillas = snapshot.documents.mapNotNull { LoteriaManager.toBolilla(it) }
            }

        onDispose {
            sorteoListener.remove()
            bolillasListener.remove()
        }
    }

    //La jugada no cambia una vez registrada: basta con leerla una vez
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

    //Cálculos hechos en la app con la jugada guardada y las bolillas recibidas
    val numerosSorteados = bolillas.map { it.numero }
    val aciertos = LoteriaCalculos.contarAciertos(jugada?.numeros ?: emptyList(), numerosSorteados)
    val mensaje = LoteriaCalculos.mensajeEstado(sorteo?.estado ?: "", bolillas.size, aciertos)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Seguimiento del sorteo", style = MaterialTheme.typography.titleLarge)
        Text(
            if (conectado) "Conectado" else "Conectando...",
            style = MaterialTheme.typography.bodySmall
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Sorteo: ${sorteo?.nombre ?: "-"}", style = MaterialTheme.typography.titleMedium)
                Text("Fecha: ${sorteo?.fecha ?: "-"}")
                Text("Estado: ${sorteo?.estado ?: "-"}")
            }
        }

        //Bolillas en orden de salida
        Text(
            "Bolillas publicadas: ${bolillas.size} de ${LoteriaCalculos.CANTIDAD_NUMEROS}",
            style = MaterialTheme.typography.titleMedium
        )
        if (bolillas.isEmpty())
            Text("Aún no se publican bolillas")
        else
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bolillas.forEach { bolilla ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NumeroCirculo(
                            bolilla.numero,
                            resaltado = jugada?.numeros?.contains(bolilla.numero) == true
                        )
                        Text("${bolilla.orden}°", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

        //Jugada del usuario con aciertos resaltados
        Text("Tu jugada", style = MaterialTheme.typography.titleMedium)
        if (cargandoJugada)
            Text("Cargando jugada...")
        else if (jugada == null)
            Text("No registraste una jugada para este sorteo")
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                jugada?.numeros?.forEach { numero ->
                    NumeroCirculo(numero, resaltado = numero in numerosSorteados)
                }
            }
            Text("Aciertos: $aciertos", style = MaterialTheme.typography.titleMedium)
        }

        //Mensaje según el estado del sorteo / resultado final
        if (jugada != null || sorteo?.estado != "finalizado")
            Text(
                mensaje,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (mensaje == "GANADOR") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )

        OutlinedButton(
            onClick = {
                navController.navigate("jugada") {
                    popUpTo("jugada")
                    launchSingleTop = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver a mi jugada")
        }
    }
}

@Composable
fun NumeroCirculo(numero: Int, resaltado: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (resaltado) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (resaltado) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(numero.toString(), fontWeight = FontWeight.Bold)
        }
    }
}
