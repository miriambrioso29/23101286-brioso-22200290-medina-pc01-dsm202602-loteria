package com.example.pc1.data.remote

import com.example.pc1.data.model.BolillaModel
import com.example.pc1.data.model.JugadaModel
import com.example.pc1.data.model.SorteoModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

object LoteriaManager {

    //Sorteo de prueba creado desde la consola de Firebase
    const val SORTEO_ID = "sorteo-prueba"

    private val firestore = FirebaseFirestore.getInstance()

    //sorteos/{sorteoId}
    val sorteoRef = firestore.collection("sorteos").document(SORTEO_ID)

    //sorteos/{sorteoId}/bolillas/{autoId}
    val bolillasRef = sorteoRef.collection("bolillas")

    //sorteos/{sorteoId}/jugadas/{uid}
    private val jugadasRef = sorteoRef.collection("jugadas")

    suspend fun getJugada(): Result<JugadaModel?> {
        return try {
            val uid = FirebaseAuthManager.currentUid() ?: throw Exception("No hay sesión iniciada")
            val snapshot = jugadasRef.document(uid).get().await()
            if (!snapshot.exists()) return Result.success(null)

            val numeros = (snapshot.get("numeros") as? List<*>)
                ?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()
            Result.success(JugadaModel(SORTEO_ID, uid, numeros))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveJugada(numeros: List<Int>): Result<Unit> {
        return try {
            val uid = FirebaseAuthManager.currentUid() ?: throw Exception("No hay sesión iniciada")
            val jugadaRef = jugadasRef.document(uid)

            //Transacción: verifica estado y jugada previa en el mismo momento en que guarda
            firestore.runTransaction { transaction ->
                val sorteo = transaction.get(sorteoRef)
                if (!sorteo.exists())
                    throw Exception("El sorteo no existe")
                if (estadoDe(sorteo) != "abierto")
                    throw Exception("El sorteo ya inició, no se pueden registrar jugadas")
                if (transaction.get(jugadaRef).exists())
                    throw Exception("Ya registraste una jugada para este sorteo")

                val jugada = hashMapOf(
                    "sorteoId" to SORTEO_ID,
                    "uid" to uid,
                    "numeros" to numeros
                )
                transaction.set(jugadaRef, jugada)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun toSorteo(snapshot: DocumentSnapshot): SorteoModel {
        return SorteoModel(
            id = snapshot.id,
            nombre = snapshot.getString("nombre") ?: "",
            fecha = fechaDe(snapshot.get("fecha")),
            estado = estadoDe(snapshot)
        )
    }

    fun toBolilla(snapshot: DocumentSnapshot): BolillaModel? {
        val numero = (snapshot.get("numero") as? Number)?.toInt() ?: return null
        val orden = (snapshot.get("orden") as? Number)?.toInt() ?: return null
        return BolillaModel(numero, orden)
    }

    private fun estadoDe(snapshot: DocumentSnapshot): String =
        (snapshot.getString("estado") ?: "").trim().lowercase()

    //La fecha puede guardarse como texto o como timestamp en la consola
    private fun fechaDe(valor: Any?): String {
        return when (valor) {
            is Timestamp -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(valor.toDate())
            null -> ""
            else -> valor.toString()
        }
    }
}
