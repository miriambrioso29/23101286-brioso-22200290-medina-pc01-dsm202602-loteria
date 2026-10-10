# CLAUDE.md

Proyecto Android **PC1** (Kotlin + Jetpack Compose + Material 3). Paquete base: `com.example.pc1`. Tema: `PC1Theme`.

## ⚠️ Seguir los patrones del proyecto de referencia del profesor

**Referencia:** `C:\Users\MIRIAM\StudioProjects\Referencias\AppUESAN202602` (paquete `dev.lchang.appuesan`).

Todo el código nuevo de este proyecto debe seguir **los mismos patrones** de arquitectura, estructura de carpetas, librerías, uso de Firebase, navegación y estilo de código que ese proyecto. Ante cualquier duda sobre "cómo se hace algo", abrir el archivo equivalente en la referencia y replicar su forma. No introducir patrones que el profesor no usa (ViewModel, Hilt/Koin, Repository + interfaces, StateFlow, sealed classes de rutas, etc.) salvo que se pida explícitamente.

### Arquitectura

Arquitectura simple de dos capas, **sin ViewModels ni inyección de dependencias**:

- **`data/`** — modelos y acceso remoto.
  - `data/model/` → `data class` simples con `val` (ej. `CountryModel(name, ranking, imageUrl)`).
  - `data/remote/` → `object` singleton (ej. `FirebaseAuthManager`) que obtiene `FirebaseAuth.getInstance()` / `FirebaseFirestore.getInstance()` como `private val` y expone funciones `suspend` que devuelven `Result<T>`.
- **`presentation/`** — pantallas Compose agrupadas por feature, más la navegación.
  - El estado de UI vive directamente en el composable con `var x by remember { mutableStateOf(...) }`.
  - Las pantallas llaman directamente al `object` de `data/remote` (o a Firestore) desde los `onClick`.
- **`ui/theme/`** — `Color.kt`, `Theme.kt`, `Type.kt` generados por Android Studio.

### Estructura de carpetas (a replicar bajo `com/example/pc1/`)

```
MainActivity.kt                      // enableEdgeToEdge + PC1Theme { AppNavGraph() }
data/
  model/        XxxModel.kt          // data classes
  remote/       FirebaseAuthManager.kt
presentation/
  auth/         LoginScreen.kt, RegisterScreen.kt
  home/         HomeScreen.kt
  navigation/   AppNavGraph.kt, DrawerScaffold.kt
  <feature>/    <Feature>Screen.kt   // ej. realtime/FirestoreRealtimeScreen.kt, permissions/GalleryPermissionsScreen.kt
ui/theme/       Color.kt, Theme.kt, Type.kt
```

Convención de nombres: archivos y composables `XxxScreen`, modelos `XxxModel`, managers `XxxManager`. Paquetes en minúscula por feature.

### Librerías (igual que la referencia)

En `gradle/libs.versions.toml` (version catalog):
- `firebaseAuth = "24.2.0"`, `firebaseFirestore = "26.6.0"` →
  `firebase-auth = { group = "com.google.firebase", name = "firebase-auth", version.ref = "firebaseAuth" }`,
  `firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore", version.ref = "firebaseFirestore" }`

En `build.gradle.kts` raíz:
- `id("com.google.gms.google-services") version "4.5.0" apply false`

En `app/build.gradle.kts`:
- plugin `id("com.google.gms.google-services")`
- dependencias declaradas directamente (con comentario encima, como hace el profesor):
  ```kotlin
  //Navigation Compose
  implementation("androidx.navigation:navigation-compose:2.10.1")
  // Coil compose
  implementation("io.coil-kt:coil-compose:2.7.0")
  //Material Icons
  implementation("androidx.compose.material:material-icons-core:1.7.8")

  implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  ```
- Requiere `app/google-services.json` (descargado de la consola de Firebase; **aún no existe en PC1**).

`AndroidManifest.xml`: `INTERNET` (y `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` si hay galería). La referencia usa `minSdk = 34`; PC1 tiene `minSdk = 24` (no cambiarlo sin pedirlo).

### Firebase Auth + Firestore

Patrón de `data/remote/FirebaseAuthManager.kt`:

```kotlin
object FirebaseAuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun registerUser(name: String, email: String, password: String): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Invalid user")
            val user = hashMapOf("name" to name, "email" to email)
            firestore.collection("users").document(uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<Unit> { /* signInWithEmailAndPassword(...).await() */ }
}
```

- Usar `kotlinx.coroutines.tasks.await` sobre las `Task` de Firebase.
- Envolver en `try/catch` y devolver `Result.success(...)` / `Result.failure(e)`.
- Al registrar: crear usuario en Auth y guardar perfil en la colección `users` con el `uid` como id del documento, usando `hashMapOf(...)`.

Consumo desde la pantalla:

```kotlin
CoroutineScope(Dispatchers.Main).launch {
    val result = FirebaseAuthManager.loginUser(email, password)
    if (result.isSuccess)
        navController.navigate("home")
    else {
        val error = result.exceptionOrNull()?.message ?: "Error desconocido"
        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }
}
```

Firestore en tiempo real (ver `presentation/realtime/FirestoreRealtimeScreen.kt`):
- Referencia al documento como `private val` a nivel de archivo: `FirebaseFirestore.getInstance().collection("...").document("...")`.
- Escuchar con `addSnapshotListener` dentro de `DisposableEffect(Unit)` y quitar en `onDispose { listener.remove() }`.
- Leer campos con `snapshot.getLong(...) ?: 0L`, `snapshot.getString(...) ?: ""`.
- Escribir con `set(mapOf(...), SetOptions.merge())` y `FieldValue.increment(1)` para contadores.

### Navegación con Drawer

- `presentation/navigation/AppNavGraph.kt`: `rememberNavController()` + `NavHost(startDestination = "login")`. **Rutas como strings literales** (`"login"`, `"register"`, `"home"`, ...).
- Pantallas de auth (login/register) se registran **sin** drawer: `composable("login") { LoginScreen(navController) }`.
- Pantallas internas se envuelven en el drawer:
  ```kotlin
  composable("home") {
      DrawerScaffold(navController) { HomeScreen() }
  }
  ```
- `presentation/navigation/DrawerScaffold.kt`: `@OptIn(ExperimentalMaterial3Api::class)`, firma `DrawerScaffold(navController: NavController, content: @Composable () -> Unit)`.
  - `rememberDrawerState(DrawerValue.Closed)` + `rememberCoroutineScope()`.
  - `ModalNavigationDrawer` → `ModalDrawerSheet` con un `Text("Menu Principal")` y un `NavigationDrawerItem` por ruta (`selected = false`, `onClick = { navController.navigate("ruta") }`).
  - Contenido: `Scaffold` con `TopAppBar` cuyo `navigationIcon` es `IconButton { Icon(Icons.Default.Menu, "Menu") }` que hace `scope.launch { drawerState.open() }`; el contenido va en `Box(modifier = Modifier.padding(paddingValues)) { content() }`.
- Al agregar una pantalla nueva: crear `presentation/<feature>/<Feature>Screen.kt`, añadir su `composable("ruta")` en `AppNavGraph` envuelto en `DrawerScaffold`, y su `NavigationDrawerItem` en `DrawerScaffold`.
- Las pantallas que navegan reciben `navController: NavController` como parámetro; las que no, no reciben parámetros.

### Estilo de código / UI

- Composables como funciones top-level `@Composable fun XxxScreen(...)`, sin ViewModel.
- Layout típico de pantalla:
  ```kotlin
  Column(
      modifier = Modifier
          .padding(16.dp)
          .fillMaxSize()
          .statusBarsPadding(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
  ) { ... }
  ```
- Título con `Text("...", style = MaterialTheme.typography.titleLarge)`.
- Inputs con `OutlinedTextField(value, onValueChange = { x = it }, placeholder = { Text("...") }, label = { Text("...") }, modifier = Modifier.fillMaxWidth())`.
- Botones `Button(onClick = { ... }, enabled = <validación>, modifier = Modifier.fillMaxWidth()) { Text("...") }`.
- Errores al usuario con `Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()` usando `LocalContext.current`.
- Listas con `LazyColumn { items(lista) { item -> Card { Row { ... } } } }`; imágenes remotas con Coil `rememberAsyncImagePainter(url)`.
- Datos de prueba como `val mockXxx = listOf(...)` a nivel de archivo.
- Permisos en runtime con `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` + `ContextCompat.checkSelfPermission`.
- Textos de UI en **español** y escritos directo en el código (no en `strings.xml`).
- Comentarios cortos estilo `//Firebase auth`, `//Toast message`.

### Cosas de la referencia que NO hay que copiar (son errores)

- `DrawerScaffold` tiene un item `"favorites"` sin `composable("favorites")` en `AppNavGraph` → crashea al pulsarlo. En PC1, cada item del drawer debe tener su ruta registrada.
- `FirestoreRealtimeScreen` incrementa el campo `"valor"` pero lee `"contador"` → el contador nunca cambia. Usar el mismo nombre de campo al leer y escribir.
- Imports sin usar (ej. `modifierLocalConsumer`, `Scaffold`/`Text` en `MainActivity`): no hace falta replicarlos.
