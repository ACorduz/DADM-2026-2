# Reto8 (directorio de empresas con SQLite): plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** App `Retos/Reto8` que gestiona en SQLite un directorio de empresas de software, con CRUD, filtros por nombre y clasificación, y confirmación de borrado.

**Architecture:** `SQLiteOpenHelper` con una tabla `companies` (clasificación en tres columnas booleanas). La lógica pura (`CompanyFilter`, `CompanyValidator`) se prueba con JUnit. Tres Activities con Views: lista → detalle → formulario.

**Tech Stack:** Kotlin, Android Views, AGP 9.3.2 (Kotlin integrado), Material Components 1.10.0, RecyclerView 1.1.0, JUnit 4.

**Spec:** `Retos/Reto8/docs/superpowers/specs/2026-10-28-reto8-directorio-sqlite-design.md`

> Nota: este plan se escribió junto con la implementación, en la misma
> sesión. El código completo de cada paso está en los archivos que se
> enumeran, así que aquí no se repite.

## Global Constraints

- Paquete / namespace / applicationId: `com.example.reto8`; `rootProject.name = "Reto8"`; tema `Theme.Reto8`; etiqueta de la app `Reto 8`.
- `minSdk 24`, `targetSdk 37`, `compileSdk release(37)`.
- Sin Firebase ni `google-services`.
- La UI está en español y el código y los comentarios en inglés.
- Todo el SQL con datos del usuario usa argumentos `?`.
- Gradle se ejecuta desde `Retos/Reto8` con `./gradlew` (Git Bash).

---

### Task 1: Proyecto base

**Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `.gitignore`, `app/.gitignore`, wrapper de Gradle, `res/xml/*`, `res/mipmap-anydpi*/*` (copiados de Reto7). Se quitan Firebase y `google-services` y se agrega `androidx-recyclerview`.

- [x] Copiar la configuración de Gradle de Reto7 y renombrar a Reto8.

### Task 2: Modelo, filtro y validación (TDD)

**Files:**
- Test: `app/src/test/java/com/example/reto8/CompanyFilterTest.kt`, `CompanyValidatorTest.kt`
- Create: `Company.kt` (`Classification`, `Company`, `CompanyTable`, `SAMPLE_COMPANIES`), `CompanyFilter.kt`, `CompanyValidator.kt`

**Interfaces:**
- `CompanyFilter.build(nameQuery: String, classifications: Set<Classification>): Selection(where: String?, args: List<String>)`
- `CompanyValidator.validate(company: Company): Set<Field>`, con `Field { NAME, URL, PHONE, EMAIL, CLASSIFICATION }`

- [x] Escribir las pruebas.
- [x] Implementar hasta que `./gradlew testDebugUnitTest` pase.

### Task 3: SQLiteOpenHelper

**Files:** Create `CompanyDbHelper.kt`; Test `app/src/androidTest/java/com/example/reto8/CompanyDbHelperTest.kt`

**Interfaces:** `CompanyDbHelper(context, name: String? = "companies.db")`, con `insert(Company): Long`, `update(Company): Boolean`, `delete(id: Long): Boolean`, `get(id: Long): Company?` y `search(nameQuery = "", classifications = emptySet()): List<Company>`.

- [x] Implementar el helper y la siembra de ejemplos en `onCreate`.
- [ ] Correr `./gradlew connectedDebugAndroidTest` con un emulador conectado.

### Task 4: Pantallas

**Files:** `UiHelpers.kt` (`applySystemBarInsets`, `classificationLabels`, `confirmDelete`, `startExternal`), `CompanyAdapter.kt`, `MainActivity.kt`, `CompanyDetailActivity.kt`, `CompanyFormActivity.kt`, `AndroidManifest.xml`, `res/layout/{activity_main,item_company,activity_company_detail,activity_company_form}.xml`, `res/menu/detail_menu.xml`, `res/values/{strings,styles,themes,colors}.xml`, `res/values-night/themes.xml`, `res/drawable/*` (ícono de edificio y los íconos de acción).

- [x] Implementar las tres pantallas.
- [x] `./gradlew assembleDebug testDebugUnitTest lintDebug`

### Task 5: Verificación manual en emulador

- [ ] `./gradlew installDebug`, y luego seguir la lista de pruebas manuales del spec (sección Pruebas).
