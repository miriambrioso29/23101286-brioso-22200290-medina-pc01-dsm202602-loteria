package com.example.pc1

import com.example.pc1.data.LoteriaCalculos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LoteriaCalculosTest {

    private val jugadaA = listOf(5, 12, 18, 23, 34, 41)
    private val jugadaB = listOf(5, 12, 18, 23, 34, 42)
    private val bolillas = listOf(5, 12, 18, 23, 34, 41)

    @Test
    fun usuarioA_seisAciertos_ganador() {
        val aciertos = LoteriaCalculos.contarAciertos(jugadaA, bolillas)
        assertEquals(6, aciertos)
        assertEquals("GANADOR", LoteriaCalculos.mensajeEstado("finalizado", 6, aciertos))
    }

    @Test
    fun usuarioB_cincoAciertos_sigueIntentando() {
        val aciertos = LoteriaCalculos.contarAciertos(jugadaB, bolillas)
        assertEquals(5, aciertos)
        assertEquals("SIGUE INTENTANDO", LoteriaCalculos.mensajeEstado("finalizado", 6, aciertos))
    }

    @Test
    fun despuesDeQuintaBolilla_ambosConCincoAciertos_sinResultado() {
        val cincoBolillas = bolillas.take(5)
        assertEquals(5, LoteriaCalculos.contarAciertos(jugadaA, cincoBolillas))
        assertEquals(5, LoteriaCalculos.contarAciertos(jugadaB, cincoBolillas))
        assertEquals("Sorteo en curso", LoteriaCalculos.mensajeEstado("en curso", 5, 5))
    }

    @Test
    fun finalizadoSinSeisBolillas_esperaResultados() {
        assertEquals("Esperando resultados...", LoteriaCalculos.mensajeEstado("finalizado", 5, 5))
    }

    @Test
    fun abierto_mensajeDeEspera() {
        assertEquals("Esperando el inicio del sorteo...", LoteriaCalculos.mensajeEstado("abierto", 0, 0))
    }

    @Test
    fun ordenNoAfectaAciertos() {
        assertEquals(6, LoteriaCalculos.contarAciertos(jugadaA, bolillas.reversed()))
    }

    @Test
    fun validarJugada() {
        assertNull(LoteriaCalculos.validarJugada(listOf("5", "12", "18", "23", "34", "41")))
        assertNotNull(LoteriaCalculos.validarJugada(listOf("5", "12", "18", "23", "34")))       //faltan
        assertNotNull(LoteriaCalculos.validarJugada(listOf("5", "5", "18", "23", "34", "41")))  //repetido
        assertNotNull(LoteriaCalculos.validarJugada(listOf("0", "12", "18", "23", "34", "41"))) //menor a 1
        assertNotNull(LoteriaCalculos.validarJugada(listOf("5", "12", "18", "23", "34", "46"))) //mayor a 45
        assertNotNull(LoteriaCalculos.validarJugada(listOf("5", "", "18", "23", "34", "41")))   //vacío
    }
}
