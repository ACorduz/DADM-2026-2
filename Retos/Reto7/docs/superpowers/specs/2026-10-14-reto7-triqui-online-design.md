# Reto7: Jugando Triqui Online — diseño

## Objetivo

Extender el Triqui de Reto6 para que dos jugadores jueguen, cada uno en su
propio dispositivo, mediados por un servicio en la nube (enunciado: `Reto7 -
TriquiOnline.pdf`). Requisitos del enunciado:

- Un juego tiene exactamente 2 jugadores.
- Cualquier jugador puede crear un juego.
- Al iniciar, la app muestra la lista de juegos disponibles.
- Un jugador se une a un juego disponible o crea uno nuevo.
- Solo después de unirse a un juego puede jugar.
- La interacción la media un servicio en la nube (aquí: Firebase).

El profesor calificará jugando contra el estudiante, así que la experiencia
de dos dispositivos reales (incluida la revancha) es lo prioritario.

## Contexto

- `Retos/Reto6/` es una app Kotlin/Views completa, paquete
  `com.example.reto6`, AGP 9.3.2 (DSL nuevo, Kotlin integrado),
  `minSdk 24`, `targetSdk 37`. Contiene:
  - `TicTacToeGame.kt`: modelo + IA (`HUMAN_PLAYER = 'X'`,
    `COMPUTER_PLAYER = 'O'`, `OPEN_SPOT = ' '`, `checkForWinner()` devuelve
    0 = sigue, 1 = empate, 2 = ganó X, 3 = ganó O; `setBoardState()` /
    `getBoardState()`).
  - `BoardView.kt`: vista personalizada que dibuja el tablero con bitmaps a
    partir de un `TicTacToeGame`; expone `getBoardCellWidth/Height()`.
  - `MainActivity.kt`: juego contra Android con sonidos, jugada retardada,
    menú popup (sin barra de título), diálogos de dificultad/Acerca de,
    puntajes en `SharedPreferences`.
  - `TicTacToeGameTest.kt`: tests JUnit del modelo.
- `Retos/Reto6/` aún no está commiteado en git; Reto7 se clona del árbol de
  trabajo actual.
- Los retos anteriores se construyeron clonando el reto previo y
  renombrando; Reto7 sigue el mismo patrón.

### Decisiones confirmadas con el usuario

- **Backend:** Firebase Realtime Database.
- **Setup:** el spec incluye los pasos para crear el proyecto de Firebase.
- **Modos:** se mantienen ambos — online y contra Android.
- **Identidad:** Firebase Anonymous Auth (uid estable para las reglas) +
  nombre escrito por el usuario y guardado en `SharedPreferences`.
- **Fin de partida:** revancha en la misma sala; se alterna quién empieza y
  se acumula el marcador de la sala.
- **Abandono:** `onDisconnect` + aviso al rival, que vuelve al lobby.
- **Integración Gradle:** plugin `google-services`; si no es compatible con
  AGP 9.3.2, se usa el **Plan B** (ver sección 2).

## Arquitectura

Tres Activities + una capa de datos + reglas puras:

```
LobbyActivity (LAUNCHER)
 ├── "Play vs Android" ──► MainActivity       (Reto6, casi sin cambios)
 ├── "Create game"     ──► OnlineGameActivity (mark = X, host)
 └── tap en la lista   ──► OnlineGameActivity (mark = O, guest)

OnlineGameActivity / LobbyActivity ──► GameRepository ──► Firebase RTDB
                                            │
                                            └── OnlineGameRules (Kotlin puro,
                                                 usa TicTacToeGame para
                                                 detectar ganador)
```

- `OnlineGame` + `OnlineGameRules` no dependen de Android ni de Firebase →
  se prueban con JUnit.
- `GameRepository` es la única clase que toca `FirebaseDatabase`.
- Las Activities solo traducen estado → UI y toques → llamadas al
  repositorio.

## Alcance

### 1. Clonar Reto6 en Reto7

Copiar `Retos/Reto6` a `Retos/Reto7` (sin `build/`, `.gradle/`, `.idea/`,
`local.properties`). Renombrar: paquete/namespace/applicationId
`com.example.reto6` → `com.example.reto7`, directorio de fuentes
`java/com/example/reto6/` → `java/com/example/reto7/` (main y test),
`rootProject.name` → `Reto7`, `Theme.Reto6` → `Theme.Reto7`, referencias
`com.example.reto6.BoardView` en los layouts. Verificar `assembleDebug` y
`testDebugUnitTest` antes de agregar nada nuevo.

### 2. Integración con Firebase (Gradle)

- `gradle/libs.versions.toml`: agregar `firebase-bom` **34.19.0** (la
  serie 35.x está compilada con Kotlin 2.4 y el Kotlin integrado de AGP
  9.3.2 es 2.2, que no puede leer esos metadatos), `firebase-database`, `firebase-auth` (módulos
  principales, sin `-ktx`) y el plugin `com.google.gms.google-services`
  (última versión estable).
- `build.gradle.kts` raíz: `alias(libs.plugins.google.services) apply false`.
- `app/build.gradle.kts`: aplicar el plugin;
  `implementation(platform(libs.firebase.bom))`,
  `implementation(libs.firebase.database)`,
  `implementation(libs.firebase.auth)`.
- `settings.gradle.kts`: el repositorio `google()` de `pluginManagement` ya
  incluye `com\\.google.*`, no requiere cambios.
- `.gitignore` de Reto7: agregar `app/google-services.json` (la demo se hace
  con el APK instalado; el archivo no se versiona).
- **Verificación (primer paso de esta sección):** con `google-services.json`
  en `app/`, `assembleDebug` debe pasar.

#### Plan B — si el plugin `google-services` no funciona con AGP 9.3.2

Si el build falla por incompatibilidad del plugin (y subirlo a su última
versión no lo arregla), **no se aplica el plugin** y se inicializa Firebase
sin él. El plugin solo convierte `google-services.json` en recursos string;
eso se puede hacer a mano:

1. Quitar el plugin de `app/build.gradle.kts` y del `build.gradle.kts` raíz
   (las dependencias de Firebase se mantienen).
2. Crear `app/src/main/res/values/google_services.xml` (agregarlo al
   `.gitignore`) con los valores copiados de `google-services.json`:

   | Recurso string           | Campo de `google-services.json`                         |
   |--------------------------|---------------------------------------------------------|
   | `google_app_id`          | `client[0].client_info.mobilesdk_app_id`                |
   | `google_api_key`         | `client[0].api_key[0].current_key`                      |
   | `project_id`             | `project_info.project_id`                               |
   | `firebase_database_url`  | `project_info.firebase_url`                             |
   | `gcm_defaultSenderId`    | `project_info.project_number`                           |
   | `google_storage_bucket`  | `project_info.storage_bucket`                           |

   Con esos recursos, el `FirebaseInitProvider` de Firebase inicializa la
   app automáticamente (lee `FirebaseOptions.fromResource()`), igual que con
   el plugin. Ningún otro código cambia.
3. Si aun así `FirebaseApp.getInstance()` falla por no estar inicializado,
   inicializar explícitamente al comienzo de `LobbyActivity.onCreate()`:

   ```kotlin
   if (FirebaseApp.getApps(this).isEmpty()) {
       FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
           .setApplicationId(getString(R.string.google_app_id))
           .setApiKey(getString(R.string.google_api_key))
           .setProjectId(getString(R.string.project_id))
           .setDatabaseUrl(getString(R.string.firebase_database_url))
           .build())
   }
   ```

4. Último recurso (no preferido): flags de compatibilidad de AGP 9 en
   `gradle.properties` (p. ej. `android.newDsl=false`), porque altera la
   configuración heredada de Reto6.

El resto del diseño es idéntico con o sin plugin.

### 3. Setup del proyecto en la consola de Firebase

Pasos manuales (los hace el usuario; el plan los lista como tarea):

1. <https://console.firebase.google.com> → **Agregar proyecto** (p. ej.
   `triqui-online`); Google Analytics desactivado.
2. **Build → Authentication → Comenzar → Sign-in method → Anónimo →
   Habilitar.**
3. **Build → Realtime Database → Crear base de datos** → ubicación
   (cualquiera) → iniciar en **modo bloqueado**.
4. Pestaña **Reglas** de la Realtime Database → pegar las reglas de la
   sección 7 → **Publicar**.
5. **Configuración del proyecto → Agregar app → Android**, paquete
   `com.example.reto7` (SHA-1 no es necesario para auth anónima).
6. Descargar `google-services.json` **después** de crear la base de datos
   (así incluye `firebase_url`; si se descargó antes, volver a descargarlo)
   y copiarlo a `Retos/Reto7/app/`.

### 4. Modelo de datos (Realtime Database)

```
/games/{gameId}            (gameId = clave de push())
    hostUid:     String     uid del creador (juega con X)
    hostName:    String
    guestUid:    String?    uid del invitado (juega con O); ausente si espera
    guestName:   String?
    status:      "waiting" | "playing" | "abandoned"
    board:       String     9 caracteres: 'X', 'O' o ' ' (OPEN_SPOT)
    turn:        "X" | "O"
    firstPlayer: "X" | "O"  quién empezó la ronda actual
    winner:      Long       0 sigue, 1 empate, 2 ganó X, 3 ganó O
    scores:      { x: Long, o: Long, ties: Long }
    rematch:     { x: Boolean, o: Boolean }
    createdAt:   Long       ServerValue.TIMESTAMP al crear
```

- El creador siempre es X y el invitado siempre O; lo que alterna en las
  revanchas es quién empieza (`firstPlayer`).
- `winner` reutiliza los códigos de `TicTacToeGame.checkForWinner()`.

### 5. `OnlineGame.kt` y `OnlineGameRules.kt` (Kotlin puro)

`data class OnlineGame` con todos los campos anteriores (`board: String`,
`turn: Char`, `firstPlayer: Char`, `winner: Int`, `scoreX/scoreO/ties: Int`,
`rematchX/rematchO: Boolean`, etc.).

- `fun toMap(): Map<String, Any?>` y `companion fun fromMap(map: Map<*, *>?):
  OnlineGame?` (tolerante a `Long` vs `Int` y a campos ausentes; devuelve
  `null` si faltan campos obligatorios). Se usa mapeo manual en lugar de
  `getValue(OnlineGame::class.java)` porque los `val` de Kotlin no tienen
  setters para el mapeador de Firebase.
- Helpers: `markOf(uid): Char?`, `opponentName(myMark)`.

`object OnlineGameRules` — cada función devuelve el nuevo estado o `null` si
la operación no es válida:

- `newGame(hostUid, hostName): OnlineGame` — tablero vacío, `turn = 'X'`,
  `firstPlayer = 'X'`, `status = waiting`, marcador en 0.
- `join(game, uid, name): OnlineGame?` — `null` si `status != waiting`, ya
  hay invitado o `uid == hostUid`. Si no: asigna invitado y
  `status = playing`.
- `applyMove(game, mark, pos): OnlineGame?` — `null` si `status != playing`,
  `winner != 0`, `turn != mark`, `pos` fuera de 0..8 o casilla ocupada. Si
  no: coloca la marca, calcula `winner` con `TicTacToeGame`
  (`setBoardState(board.toCharArray())` + `checkForWinner()`), alterna
  `turn` y, si la ronda terminó, incrementa `scoreX`, `scoreO` o `ties`.
- `requestRematch(game, mark): OnlineGame?` — `null` si `status != playing`
  o `winner == 0`. Marca `rematch[mark] = true`; si ambos quedan en `true`:
  tablero vacío, `firstPlayer` alterna, `turn = firstPlayer`, `winner = 0`,
  `rematch` vuelve a `false/false`.

### 6. `GameRepository.kt`

Envuelve `FirebaseDatabase.getInstance().reference.child("games")`. Callbacks
simples en el hilo principal (estilo del curso, sin corrutinas).

- `observeWaitingGames(onChange: (List<Pair<String, OnlineGame>>) -> Unit):
  ValueEventListener` — consulta `orderByChild("status").equalTo("waiting")`;
  ordena por `createdAt` descendente en el cliente.
- `createGame(uid, name, onResult: (gameId: String?) -> Unit)` — `push()` +
  `setValue(newGame.toMap() + ("createdAt" to ServerValue.TIMESTAMP))`.
- `joinGame(gameId, uid, name, onResult: (Boolean) -> Unit)`,
  `makeMove(gameId, mark, pos, onResult: (Boolean) -> Unit)`,
  `requestRematch(gameId, mark)` — todas con `runTransaction`:
  `fromMap(currentData.value)`; si es `null` → `Transaction.success(currentData)`
  (Firebase reintenta con el valor del servidor; si el nodo de verdad no
  existe, `onComplete` lo detecta); si la regla devuelve `null` →
  `Transaction.abort()`; si no → `currentData.value = nuevo.toMap()`.
  `joinGame` reporta éxito solo si `committed` y el snapshot final tiene
  `guestUid == uid`.
- `observeGame(gameId, onChange: (OnlineGame?) -> Unit): ValueEventListener`
  (`null` = el juego fue borrado).
- `removeListener(...)`, y las operaciones de presencia de la sección 9:
  `armDisconnectForWaiting(gameId)`, `armDisconnectForPlaying(gameId)`,
  `leaveGame(gameId, currentStatus)`.

### 7. Reglas de seguridad (Realtime Database)

```json
{
  "rules": {
    "games": {
      ".read": "auth != null",
      ".indexOn": ["status"],
      "$gameId": {
        ".write": "auth != null && ((!data.exists() && newData.child('hostUid').val() === auth.uid) || data.child('hostUid').val() === auth.uid || data.child('guestUid').val() === auth.uid || (data.child('status').val() === 'waiting' && !data.child('guestUid').exists() && newData.child('guestUid').val() === auth.uid))"
      }
    }
  }
}
```

Solo usuarios autenticados leen; solo el creador o el invitado escriben en
su juego, más quien se une a un juego en espera. Validar el turno en las
reglas queda **fuera de alcance**: el turno se valida en la transacción del
cliente (`OnlineGameRules`).

### 8. Pantallas

Las cadenas nuevas van en `strings.xml` en inglés, igual que las de Reto6.

**`LobbyActivity` (nueva, LAUNCHER)** — `activity_lobby.xml` (+ `layout-land`
si hace falta), con el mismo botón de menú en esquina que Reto6:

- Al abrir: `FirebaseAuth.signInAnonymously()` si no hay usuario. Mientras
  tanto, texto "Connecting…" y botones deshabilitados. Si falla: "Couldn't
  connect. Check your internet connection." + botón "Retry".
- Si no hay nombre en `SharedPreferences`: diálogo con `EditText` (no vacío,
  trim, máx. 20 caracteres) antes de poder crear/unirse. El menú tiene
  "Change name" y "About".
- Encabezado "Playing as <name>".
- Botones "Create game" y "Play vs Android".
- `ListView` con "<hostName>'s game" de los juegos en espera, excluyendo los
  del propio uid; vista vacía "No games available. Create one!". El listener
  se registra en `onStart()` y se quita en `onStop()`.
- Tocar un juego → `joinGame` → éxito: abre `OnlineGameActivity` (mark `O`);
  fallo: Toast "That game is no longer available".
- "Create game" → `createGame` → abre `OnlineGameActivity` (mark `X`).

**`MainActivity` (Reto6)** — deja de ser LAUNCHER (`exported="false"`); se
abre desde el lobby y "atrás" vuelve al lobby. Sin otros cambios.

**`OnlineGameActivity` (nueva)** — `activity_online_game.xml` (+
`layout-land`) basado en `activity_main.xml`: `BoardView`, texto de
información, marcador y un botón "Rematch" visible solo al terminar la
ronda. Menú: "Leave game", "About".

- Extras: `gameId`, `myMark`. Mantiene un `TicTacToeGame` local que solo
  sirve para que `BoardView` dibuje (`setBoardState(board.toCharArray())` +
  `invalidate()` en cada snapshot).
- Listener de `observeGame` registrado en `onCreate()` y quitado en
  `onDestroy()`; todo el estado viene de Firebase, por eso rotar no necesita
  `onSaveInstanceState`.
- Texto de información según el estado:
  - `waiting`: "Waiting for an opponent…"
  - `playing`, mi turno: "Your turn (X)" / "Your turn (O)"
  - `playing`, turno rival: "<rival>'s turn"
  - terminada: "You won!", "<rival> won!" o "It's a tie." + línea de revancha
    ("Waiting for <rival>…" si ya la pedí; "<rival> wants a rematch!" si la
    pidió el rival).
- Marcador: "You: n · Ties: n · <rival>: n" (mismos tres `TextView` que
  Reto6).
- Toque: solo se acepta si `status == playing`, `winner == 0`,
  `turn == myMark`, casilla libre y no hay una jugada pendiente
  (`mMovePending`, se limpia en el `onResult`). Mismo cálculo de celda que
  Reto6.
- Sonidos: al recibir un snapshot, se compara con el tablero anterior; la
  casilla nueva suena `human_move` si es mía y `computer_move` si es del
  rival. El primer snapshot no suena.
- "Rematch" → `requestRematch`.

### 9. Presencia y abandono

- **Creador en espera:** al crear, `armDisconnectForWaiting` registra
  `gameRef.onDisconnect().removeValue()`.
- **Partida en curso:** cuando el creador observa `status == playing`
  (y el invitado, tras unirse), `armDisconnectForPlaying` cancela lo
  anterior y registra `statusRef.onDisconnect().setValue("abandoned")`.
- **Salida voluntaria** (botón atrás vía `OnBackPressedDispatcher`, menú
  "Leave game", o `onDestroy()` con `isFinishing`): `leaveGame` cancela el
  `onDisconnect` y, según el estado actual: `waiting` → borra el juego;
  `playing` → `status = abandoned`; `abandoned` → borra el juego (el último
  en salir limpia). Una rotación (`isFinishing == false`) no cuenta como
  salida.
- **Rival se fue:** al observar `status == abandoned` (o el juego borrado),
  diálogo no cancelable "Your opponent left the game." → OK → `leaveGame` +
  `finish()` al lobby.
- El lobby solo muestra `status == waiting`, así que los juegos
  abandonados nunca aparecen en la lista.

### 10. Manifest y recursos

- `AndroidManifest.xml`: permiso `INTERNET`; `LobbyActivity` como LAUNCHER;
  `MainActivity` y `OnlineGameActivity` con `exported="false"`.
- Nuevos layouts: `activity_lobby.xml`, `activity_online_game.xml` (+ sus
  `layout-land`), `dialog_name.xml` (EditText), `lobby_menu.xml`,
  `online_menu.xml`.
- El texto del diálogo "About" se actualiza para mencionar el modo online.

## Pruebas

**Unitarias (JUnit, `testDebugUnitTest`):**

- `TicTacToeGameTest` — se mantiene sin cambios.
- `OnlineGameRulesTest`:
  - `newGame` produce tablero vacío, `waiting`, turno X.
  - `join`: éxito; rechazo si ya hay invitado, si `status != waiting` o si
    el uid es el del creador.
  - `applyMove`: jugada válida alterna el turno; rechazo fuera de turno, en
    casilla ocupada, con `pos` fuera de rango, con partida terminada o en
    `waiting`.
  - Victoria de X, victoria de O y empate actualizan `winner` y el marcador
    correcto.
  - `requestRematch`: una sola petición no reinicia; ambas reinician el
    tablero, alternan `firstPlayer`/`turn` y conservan el marcador; rechazo
    si la ronda no ha terminado.
- `OnlineGameTest`: ida y vuelta `toMap()` → `fromMap()`; `fromMap` acepta
  `Long` donde se espera `Int` y devuelve `null` con campos obligatorios
  faltantes.

**Manual (2 dispositivos o 2 emuladores):**

1. Primera ejecución pide nombre; "Change name" lo cambia.
2. A crea juego → B lo ve en la lista con el nombre de A; A ve "Waiting for
   an opponent…".
3. B se une → el juego desaparece de la lista de un tercer dispositivo; ambos
   ven quién tiene el turno.
4. Dos dispositivos intentan unirse al mismo juego a la vez → solo uno
   entra; el otro ve el Toast.
5. Tocar en turno del rival o en casilla ocupada no hace nada.
6. Victoria, derrota y empate se muestran correctamente en ambos lados, con
   sonidos.
7. Revancha: con un solo jugador pidiéndola no pasa nada; con ambos se
   reinicia, empieza el otro jugador y el marcador se acumula.
8. Rotar en medio de una partida no la abandona ni pierde estado.
9. A sale con "atrás" → B ve "Your opponent left the game." y vuelve al
   lobby.
10. Cerrar A forzosamente o activar modo avión → B recibe el aviso (cuando
    Firebase detecta la desconexión).
11. Creador sale estando en espera → el juego desaparece de la lista.
12. "Play vs Android" funciona como en Reto6.

## Fuera de alcance

- Validación del turno en las reglas de seguridad.
- Reconexión a una partida después de cerrar la app.
- Chat, invitaciones, historial o ranking global.
- Limpieza programada de juegos huérfanos (Cloud Functions).
- Login con Google u otra identidad no anónima.

## Riesgos

- **Plugin `google-services` vs AGP 9.3.2:** verificado: el plugin 4.5.0
  funciona con AGP 9.3.2 (no se necesita el Plan B).
- **Versión de Kotlin de Firebase:** subir el BoM a 35.x exige subir el
  Kotlin del proyecto a 2.4; mientras tanto se queda en 34.19.0.
- **Latencia de `onDisconnect`:** si se pierde la red sin cerrar el socket,
  Firebase puede tardar hasta ~1 minuto en detectarlo; el aviso al rival
  llega con ese retraso. Es aceptable.
- **Juego borrado mientras se intenta unirse:** la transacción con valor
  `null` termina sin `guestUid` propio → se trata como "no disponible".
- **`google-services.json` fuera de git:** clonar el repo no basta para
  compilar; quien compile necesita el archivo (o los recursos del Plan B).
