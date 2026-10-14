# Reto7 Parte 1 (base + Firebase) — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dejar `Retos/Reto7` como un clon funcional de Reto6 conectado a un proyecto de Firebase (secciones 1–3 del spec), listo para construir el modo online.

**Architecture:** Clon de Reto6 renombrado a `com.example.reto7`; Firebase BoM + `firebase-database` + `firebase-auth` vía version catalog; plugin `com.google.gms.google-services` (o Plan B del spec si no es compatible con AGP 9.3.2). El setup en la consola de Firebase lo hace el usuario.

**Tech Stack:** Kotlin, Android Views, AGP 9.3.2, Gradle version catalog, Firebase BoM 34.19.0 (35.x requiere Kotlin 2.4; el Kotlin integrado de AGP 9.3.2 es 2.2), plugin google-services 4.5.0.

**Spec:** `Retos/Reto7/docs/superpowers/specs/2026-10-14-reto7-triqui-online-design.md` (secciones 1, 2 y 3)

## Global Constraints

- Paquete / namespace / applicationId: `com.example.reto7`; `rootProject.name = "Reto7"`; tema `Theme.Reto7`.
- `minSdk 24`, `targetSdk 37`, `compileSdk release(37)` — sin cambios respecto a Reto6.
- Firebase: módulos principales (`firebase-database`, `firebase-auth`), sin `-ktx`.
- `app/google-services.json` no se versiona (va en `.gitignore`).
- Gradle se ejecuta desde `Retos/Reto7` con `./gradlew` (Git Bash).
- Commits terminan con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

### Task 1: Clonar Reto6 en Reto7 y renombrar

**Files:**
- Create: `Retos/Reto7/**` (copia de `Retos/Reto6` sin `build/`, `.gradle/`, `.idea/`, `local.properties`, `.kotlin/`)
- Modify: `Retos/Reto7/settings.gradle.kts`, `Retos/Reto7/app/build.gradle.kts`, `AndroidManifest.xml`, `values/themes.xml`, `values-night/themes.xml`, `layout/activity_main.xml`, `layout-land/activity_main.xml`, todos los `.kt`
- Move: `app/src/{main,test}/java/com/example/reto6/` → `.../reto7/`

**Interfaces:**
- Produces: proyecto `Reto7` que compila, con `com.example.reto7.{MainActivity, BoardView, TicTacToeGame}`.

- [x] **Step 1: Copiar el árbol**

```bash
cd /d/Escritorio/DADM-2026-2/Retos
mkdir Reto7-tmp && cd Reto6 && tar --exclude=./build --exclude=./app/build --exclude=./.gradle --exclude=./.idea --exclude=./.kotlin --exclude=./local.properties -cf - . | (cd ../Reto7-tmp && tar xf -)
cd .. && cp -r Reto7-tmp/. Reto7/ && rm -rf Reto7-tmp
```

(`Reto7/` ya existe por `docs/`; se fusiona.)

- [x] **Step 2: Mover paquetes y renombrar**

```bash
cd /d/Escritorio/DADM-2026-2/Retos/Reto7
mv app/src/main/java/com/example/reto6 app/src/main/java/com/example/reto7
mv app/src/test/java/com/example/reto6 app/src/test/java/com/example/reto7
grep -rl --exclude-dir=docs -e 'reto6' -e 'Reto6' . | xargs sed -i 's/reto6/reto7/g; s/Reto6/Reto7/g'
grep -rn --exclude-dir=docs -i 'reto6' . || echo "OK: no quedan referencias"
```

Expected: `OK: no quedan referencias`.

- [x] **Step 3: Crear `local.properties` para el SDK**

```bash
cp ../Reto6/local.properties .
```

- [x] **Step 4: Verificar build y tests**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, tests de `TicTacToeGameTest` en verde.

- [x] **Step 5: Commit**

```bash
git add Retos/Reto7 && git commit -m "Reto7: clone Reto6 as starting point"
```

---

### Task 2: Agregar Firebase (Gradle)

**Files:**
- Modify: `Retos/Reto7/gradle/libs.versions.toml`, `Retos/Reto7/build.gradle.kts`, `Retos/Reto7/app/build.gradle.kts`, `Retos/Reto7/.gitignore`, `Retos/Reto7/app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `FirebaseDatabase` y `FirebaseAuth` disponibles en `app`; alias de catálogo `libs.firebase.bom`, `libs.firebase.database`, `libs.firebase.auth`, `libs.plugins.google.services`.

- [x] **Step 1: Version catalog** — en `gradle/libs.versions.toml` agregar:

```toml
# [versions]
firebaseBom = "34.19.0"
googleServices = "4.5.0"

# [libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-database = { group = "com.google.firebase", name = "firebase-database" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }

# [plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

- [x] **Step 2: Plugins y dependencias**

`build.gradle.kts` (raíz):

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
}
```

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}
// ...
dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)
    // (dependencias existentes)
}
```

- [x] **Step 3: `.gitignore` y permiso INTERNET**

Agregar al final de `Retos/Reto7/.gitignore`:

```
# Firebase config (not versioned; see the Reto7 spec, section 3)
app/google-services.json
app/src/main/res/values/google_services.xml
```

En `AndroidManifest.xml`, antes de `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- [x] **Step 4: Verificar compatibilidad del plugin con AGP 9.3.2**

Run: `./gradlew assembleDebug`
Expected (sin `google-services.json` todavía): falla en la tarea `processDebugGoogleServices` con `File google-services.json is missing`. Ese error demuestra que el plugin cargó y se integró con AGP 9.3.2 → compatible.

Si en cambio falla al configurar el proyecto (errores de API de AGP, `BaseExtension`, `applicationVariants`, etc.): aplicar el **Plan B** del spec (sección 2): quitar `alias(libs.plugins.google.services)` de ambos `build.gradle.kts` (y su entrada `[plugins]`), y en la Task 3 crear `app/src/main/res/values/google_services.xml` a mano.

- [x] **Step 5: Verificar que el resto compila sin el plugin aplicado**

Comentar temporalmente `alias(libs.plugins.google.services)` en `app/build.gradle.kts`, correr `./gradlew assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL` (las dependencias de Firebase resuelven); descomentar.

- [x] **Step 6: Commit**

```bash
git add Retos/Reto7 && git commit -m "Reto7: add Firebase Realtime Database and Auth dependencies"
```

---

### Task 3: Setup en la consola de Firebase (manual, usuario)

**Files:**
- Create (no versionado): `Retos/Reto7/app/google-services.json` (o, con Plan B, `app/src/main/res/values/google_services.xml`)

- [x] **Step 1:** <https://console.firebase.google.com> → **Agregar proyecto** (p. ej. `triqui-online`), Google Analytics desactivado.
- [x] **Step 2:** **Build → Authentication → Comenzar → Sign-in method → Anónimo → Habilitar.**
- [x] **Step 3:** **Build → Realtime Database → Crear base de datos** → cualquier ubicación → **modo bloqueado**.
- [x] **Step 4:** Pestaña **Reglas** → pegar y **Publicar**:

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

- [x] **Step 5:** **Configuración del proyecto → Agregar app → Android**, paquete `com.example.reto7` (sin SHA-1).
- [x] **Step 6:** Descargar `google-services.json` (después del Step 3, para que incluya `firebase_url`) y copiarlo a `Retos/Reto7/app/`.
- [x] **Step 7: Verificar**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`. Comprobar que `git status` no muestra `google-services.json`.

---

## Siguiente

Las secciones 4–10 del spec (modelo `OnlineGame`, `OnlineGameRules`, `GameRepository`, lobby, pantalla online, presencia) se planifican en la Parte 2, una vez completada la Task 3.
