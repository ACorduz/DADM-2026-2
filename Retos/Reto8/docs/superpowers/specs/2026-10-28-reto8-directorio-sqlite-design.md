# Reto8: Almacenamiento con SQLite — diseño

## Objetivo

App Android que gestiona un directorio de empresas de desarrollo de software
guardado en SQLite (enunciado: `Reto8 - Almacenamiento con SQLite.pdf`).
Requisitos del enunciado:

- Cada empresa tiene: nombre, URL de la página web, teléfono, email,
  productos y servicios, y clasificación (consultoría, desarrollo a la medida
  **y/o** fábrica de software).
- CRUD completo sobre las empresas.
- Listar las empresas y filtrar por nombre y/o clasificación.
- Confirmar con el usuario antes de eliminar una empresa.

## Decisiones confirmadas con el usuario

- **Filtro por clasificación:** varias marcadas = empresas con **cualquiera**
  de ellas (OR). Se combina con el filtro por nombre con AND.
- **Datos de ejemplo:** 5 empresas ficticias (dominios `.example`) se cargan al
  crear la base, para que la demo no arranque vacía.
- **Idioma de la UI:** español. El código y los comentarios siguen en inglés,
  como en los retos anteriores.
- **Extras:** en el detalle, tocar la web, el teléfono o el email abre el
  navegador, el marcador o el correo.

## Contexto técnico

Proyecto nuevo en `Retos/Reto8/`, paquete `com.example.reto8`, `minSdk 24`,
`targetSdk 37`. Reutiliza la configuración de Gradle de Reto7 (AGP 9.3.2 con
Kotlin integrado, version catalog, wrapper), **sin Firebase**. Kotlin y Views,
tema `Theme.MaterialComponents.DayNight.NoActionBar.Bridge` con
`MaterialToolbar` en cada pantalla. Como `targetSdk 37` obliga a dibujar de
borde a borde, `applySystemBarInsets()` acolcha la app bar y la raíz.

## Datos

Se usa `SQLiteOpenHelper` directo, como en el tutorial que cita el enunciado.
No se usa Room.

```sql
CREATE TABLE companies (
  _id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  url TEXT NOT NULL DEFAULT '', phone TEXT NOT NULL DEFAULT '',
  email TEXT NOT NULL DEFAULT '', products TEXT NOT NULL DEFAULT '',
  is_consulting INTEGER NOT NULL DEFAULT 0,
  is_custom_development INTEGER NOT NULL DEFAULT 0,
  is_software_factory INTEGER NOT NULL DEFAULT 0
);
```

La clasificación es multivalor, así que se guarda en tres columnas
booleanas. Filtrar es un `OR` simple y no hace falta una tabla intermedia.

| Archivo | Responsabilidad |
|---|---|
| `Company.kt` | `enum Classification` (columna + etiqueta), `data class Company`, `object CompanyTable` (esquema), `SAMPLE_COMPANIES` |
| `CompanyFilter.kt` | Función pura que arma el `WHERE` y sus argumentos: `name LIKE ? ESCAPE '\'` (con `%` y `_` escapados) AND `(is_x = 1 OR …)` |
| `CompanyValidator.kt` | Función pura que devuelve los campos inválidos (`enum Field`). Nombre y ≥1 clasificación son obligatorios; URL, email y teléfono se validan solo si se llenan (regex propias, porque `android.util.Patterns` no funciona en JUnit) |
| `CompanyDbHelper.kt` | `insert`, `update`, `delete`, `get`, `search(nombre, clasificaciones)` ordenado por nombre sin distinguir mayúsculas. `onCreate` siembra los ejemplos; `name = null` crea una base en memoria para las pruebas |

Toda consulta usa argumentos parametrizados, nunca concatena texto del
usuario. La base es pequeña, así que se accede en el hilo principal.

## Pantallas

1. **`MainActivity` (launcher), la lista:**
   - Campo de búsqueda por nombre.
   - Tres chips de filtro por clasificación.
   - `RecyclerView` con nombre, clasificaciones y contacto.
   - El subtítulo muestra el conteo.
   - Un mensaje vacío distingue entre "no hay empresas" y "nada coincide".
   - FAB **+** para crear.
   - Tocar una fila abre el detalle; mantenerla presionada pide confirmar el borrado.
   - Recarga en `onResume`.
2. **`CompanyDetailActivity`, el *retrieve*:**
   - Muestra todos los campos y oculta los opcionales vacíos.
   - Web, teléfono y email se pueden tocar (`ACTION_VIEW` / `ACTION_DIAL` / `ACTION_SENDTO`). Si falta el esquema, se antepone `https://`. Si no hay app, se muestra un toast.
   - En la app bar están **Editar** y **Eliminar**; eliminar pide confirmación y luego cierra la pantalla.
3. **`CompanyFormActivity`, crear y editar:**
   - Recibe un `id` opcional.
   - Usa `TextInputLayout` con el `inputType` adecuado y tres checkboxes.
   - Los errores se muestran en cada campo.
   - Al girar la pantalla no recarga desde la base, para no perder lo escrito.

**Confirmación de borrado:** un `AlertDialog` con «¿Seguro que quieres
eliminar "X"? Esta acción no se puede deshacer.» y los botones Cancelar y
Eliminar. Se comparte entre la lista y el detalle (`confirmDelete()` en
`UiHelpers.kt`).

## Pruebas

- **JUnit** (`app/src/test`): `CompanyFilterTest` y `CompanyValidatorTest`.
- **Instrumentadas** (`app/src/androidTest`): `CompanyDbHelperTest` hace el
  CRUD completo y la búsqueda sobre una base en memoria. Requieren un
  emulador o dispositivo: `./gradlew connectedDebugAndroidTest`.
- **Manual:** crear, editar, filtrar por nombre, por clasificación y por
  ambos, eliminar desde la lista y desde el detalle (también cancelando),
  abrir web, teléfono y email, y girar la pantalla en el formulario.
