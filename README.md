# PC01 · Lotería — Desarrollo de aplicaciones móviles 2026-02

| Integrante | Código |
|---|---|
| Miriam Brioso Blas | 23101286 |
| Mijhail Medina Tello | 22200290 |

Aplicación Android (Kotlin + Jetpack Compose) con Firebase Authentication y Cloud Firestore para registrar una jugada de 6 números y seguir un sorteo en tiempo real.

## Estructura en Firestore

```
sorteos/sorteo-prueba                  → nombre (string), fecha (string o timestamp), estado ("abierto" | "en curso" | "finalizado")
sorteos/sorteo-prueba/bolillas/{id}    → numero (number), orden (number, 1..6)
sorteos/sorteo-prueba/jugadas/{uid}    → sorteoId (string), uid (string), numeros (array de number)
users/{uid}                            → name, email
```

- La jugada usa el `uid` como id del documento: una sola jugada por usuario y sorteo.
- GANADOR / SIGUE INTENTANDO **no** se guardan en Firestore; los calcula la app (`data/LoteriaCalculos.kt`).

## Instrucciones de ejecución

1. Abrir el proyecto en Android Studio y sincronizar Gradle.
2. En la consola de Firebase del proyecto:
   - **Authentication → Sign-in method:** habilitar *Correo electrónico/contraseña*.
   - **Firestore Database:** crear la base de datos.
   - Crear la colección `sorteos` con el documento de id **`sorteo-prueba`** y los campos:
     - `nombre` (string): por ejemplo `Sorteo de prueba PC01`
     - `fecha` (string): por ejemplo `2026-10-15`
     - `estado` (string): `abierto`
3. Ejecutar la app en dos emuladores o dispositivos y crear dos cuentas.
4. Registrar las jugadas mientras el sorteo está `abierto`:
   - Usuario A: 5, 12, 18, 23, 34, 41
   - Usuario B: 5, 12, 18, 23, 34, 42
5. Abrir **Seguimiento del sorteo** en ambos dispositivos.

## Demostración (desde la consola de Firebase)

1. Cambiar `estado` de `abierto` a `en curso`.
2. En `sorteos/sorteo-prueba`, crear la subcolección `bolillas` y agregar **un documento por bolilla** (id automático), con los campos `numero` (number) y `orden` (number):
   `5/1`, `12/2`, `18/3`, `23/4`, `34/5`, `41/6`. Esperar a que aparezca en ambas pantallas antes de agregar la siguiente.
3. Tras la quinta bolilla: ambos usuarios muestran 5 aciertos y todavía no aparece el resultado.
4. Tras la sexta bolilla, cambiar `estado` a `finalizado`: A muestra **GANADOR** y B **SIGUE INTENTANDO**.
5. Cerrar y volver a abrir la app: la sesión, la jugada y las bolillas se recuperan y el resultado se recalcula.

> Importante: `numero` y `orden` deben ser de tipo **number** en la consola (no string).

## Código clave

| Tema | Archivo |
|---|---|
| Autenticación | `data/remote/FirebaseAuthManager.kt`, `presentation/auth/` |
| Persistencia y validación de la jugada (transacción que verifica estado "abierto") | `data/remote/LoteriaManager.kt` |
| Escucha en tiempo real (`addSnapshotListener` en `DisposableEffect`, `remove()` en `onDispose`) | `presentation/seguimiento/SeguimientoScreen.kt` |
| Cálculos (aciertos, resultado, mensajes) | `data/LoteriaCalculos.kt` |
| Navegación y drawer | `presentation/navigation/` |

Tests de los cálculos: `./gradlew testDebugUnitTest` (`app/src/test/java/com/example/pc1/LoteriaCalculosTest.kt`).

## Reglas de seguridad sugeridas (Firestore)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
    match /sorteos/{sorteoId} {
      allow read: if request.auth != null;
      match /bolillas/{bolillaId} {
        allow read: if request.auth != null;
      }
      match /jugadas/{uid} {
        allow read: if request.auth != null && request.auth.uid == uid;
        allow create: if request.auth != null && request.auth.uid == uid
          && get(/databases/$(database)/documents/sorteos/$(sorteoId)).data.estado == 'abierto';
      }
    }
  }
}
```
