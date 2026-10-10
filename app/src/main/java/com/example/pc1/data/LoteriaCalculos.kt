package com.example.pc1.data

object LoteriaCalculos {

    const val CANTIDAD_NUMEROS = 6
    const val NUMERO_MINIMO = 1
    const val NUMERO_MAXIMO = 45

    //Devuelve el mensaje de error, o null si la jugada es válida
    fun validarJugada(textos: List<String>): String? {
        if (textos.size != CANTIDAD_NUMEROS || textos.any { it.isBlank() })
            return "Ingresa los $CANTIDAD_NUMEROS números"

        val numeros = textos.map { it.trim().toIntOrNull() ?: return "Solo se permiten números enteros" }
        if (numeros.any { it < NUMERO_MINIMO || it > NUMERO_MAXIMO })
            return "Los números deben estar entre $NUMERO_MINIMO y $NUMERO_MAXIMO"
        if (numeros.toSet().size != CANTIDAD_NUMEROS)
            return "Los números no se pueden repetir"

        return null
    }

    //Números distintos de la jugada que están entre las bolillas publicadas (sin importar el orden)
    fun contarAciertos(jugada: List<Int>, bolillas: List<Int>): Int {
        val sorteados = bolillas.toSet()
        return jugada.toSet().count { it in sorteados }
    }

    fun resultado(aciertos: Int): String =
        if (aciertos == CANTIDAD_NUMEROS) "GANADOR" else "SIGUE INTENTANDO"

    //El resultado definitivo solo se muestra si el sorteo finalizó y llegaron las 6 bolillas
    fun mensajeEstado(estado: String, bolillasPublicadas: Int, aciertos: Int): String {
        return when (estado) {
            "abierto" -> "Esperando el inicio del sorteo..."
            "en curso" -> "Sorteo en curso"
            "finalizado" ->
                if (bolillasPublicadas == CANTIDAD_NUMEROS) resultado(aciertos)
                else "Esperando resultados..."
            else -> "Cargando sorteo..."
        }
    }
}
