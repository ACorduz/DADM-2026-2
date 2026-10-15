# Reto4: Menus and Dialog Boxes — design

## Goal

Extend the Reto3 Tic-Tac-Toe app with a richer options menu (New Game /
Difficulty / Quit) backed by `AlertDialog`s, per the "Menus and Dialog
Boxes" challenge (Frank McCown, Harding University, see `Reto4 - Menus
and dialog boxes.pdf`). Includes both of the PDF's "Extra Challenge"
items: a custom app icon, and an "About" menu option/dialog identifying
the programmer.

## Context

- `Retos/Reto4/` is currently empty (no scaffold, nothing staged in git).
- `Retos/Reto3/` (commit `98e09f2`) is a complete, working, reviewed
  Kotlin/Views Tic-Tac-Toe app — package `com.example.reto3` — that
  already implements everything this challenge builds on top of:
  `TicTacToeGame.kt` (model), `activity_main.xml` (board layout),
  `MainActivity.kt` (game loop, "New Game" menu, alternating starter,
  score counters).
- Per the user: Reto4 is a continuation of Reto3, so the existing code
  should be reused by copying it into `Retos/Reto4` and renamed, rather
  than re-created from scratch.
- Decisions confirmed with the user:
  - Dialogs use the PDF's literal `onCreateDialog(int)` /
    `showDialog(int)` pattern, even though both are deprecated on
    `Activity` — accepted risk, see Risks below.
  - Menu/app icons are hand-authored simple vector drawables (no PNG
    assets), since this environment can't produce raster images.
  - Both "Extra Challenge" items are in scope: custom app icon, and an
    About dialog crediting "ACorduz" as the programmer.

## Scope

1. **Clone Reto3 into Reto4.** Copy the full working tree from
   `Retos/Reto3` (at commit `98e09f2`) into `Retos/Reto4`, excluding
   `Retos/Reto3/docs/superpowers/` (Reto3's own planning artifacts —
   Reto4 gets its own spec/plan under its own `docs/superpowers/`).
   Rename throughout: package/namespace `com.example.reto3` →
   `com.example.reto4`, `rootProject.name` `Reto3` → `Reto4`, style name
   `Theme.Reto3` → `Theme.Reto4`, `app_name` string, and the Java/Kotlin
   source directory `java/com/example/reto3/` →
   `java/com/example/reto4/`. Verify the cloned project still builds
   (`assembleDebug`) and all existing tests still pass
   (`testDebugUnitTest`) before any new Reto4-specific code is added.

2. **`TicTacToeGame.kt` — difficulty levels.**
   - Add `enum class DifficultyLevel { Easy, Harder, Expert }`.
   - Add `private var mDifficultyLevel: DifficultyLevel =
     DifficultyLevel.Expert` with public `getDifficultyLevel()` /
     `setDifficultyLevel(level: DifficultyLevel)`.
   - Refactor `getComputerMove()` into a dispatcher matching the PDF's
     given logic exactly:
     - `Easy` → `getRandomMove()`
     - `Harder` → `getWinningMove()`, else `getRandomMove()`
     - `Expert` → `getWinningMove()`, else `getBlockingMove()`, else
       `getRandomMove()`
   - Extract three new `private` methods from the existing
     `getComputerMove()` body (today it already does exactly this
     win-check/block-check/random-fallback sequence inline): `private
     fun getRandomMove(): Int`, `private fun getWinningMove(): Int`,
     `private fun getBlockingMove(): Int`. Each temporarily mutates
     `mBoard` to test a candidate and restores it before returning, same
     "try/check/revert" pattern already used. `getWinningMove()` /
     `getBlockingMove()` return `-1` when no such move exists (matching
     the PDF's `move == -1` checks in the dispatcher).
   - `boardCell()` (the existing `internal` test-only accessor) is
     unchanged and keeps serving both old and new tests.

3. **Menu** (`res/menu/options_menu.xml`): four items — New Game,
   Difficulty, Quit, About (About is the extra-challenge addition) —
   each with a title string resource and an `android:icon` pointing at
   a hand-authored vector drawable (`ic_new_game`, `ic_difficulty`,
   `ic_quit`, `ic_about`).

4. **Dialogs in `MainActivity.kt`**, literal PDF pattern:
   - `onCreateOptionsMenu()` inflates `R.menu.options_menu`.
   - `onOptionsItemSelected()` switches on item id: `new_game` →
     `startNewGame()`; `ai_difficulty` →
     `showDialog(DIALOG_DIFFICULTY_ID)`; `quit` →
     `showDialog(DIALOG_QUIT_ID)`; `about` →
     `showDialog(DIALOG_ABOUT_ID)`.
   - `onCreateDialog(id: Int): Dialog` builds three `AlertDialog`s:
     - **Difficulty**: `setSingleChoiceItems` over
       [Easy, Harder, Expert] strings, radio pre-selected from
       `mGame.getDifficultyLevel().ordinal`; on selection, calls
       `mGame.setDifficultyLevel(DifficultyLevel.values()[item])`,
       shows a `Toast` with the chosen level name, dismisses.
     - **Quit**: message + Yes (calls `finish()`) / No (`null`) buttons.
     - **About**: inflates `res/layout/about_dialog.xml` via
       `LayoutInflater`, `setView(...)`, single "OK" button. Content:
       app name, "By ACorduz", one short description line, and an
       `ImageView` reusing the app's icon drawable.
   - The old Reto3 `menu/main_menu.xml` (single "New Game" item) and its
     `onCreateOptionsMenu`/`onOptionsItemSelected` are fully replaced by
     the above — not kept side by side.

5. **Custom app icon** (extra challenge #1): replace the content of
   `res/drawable/ic_launcher_foreground.xml` with a simple vector
   drawing a tic-tac-toe grid (two vertical + two horizontal stroked
   lines), keeping the existing adaptive-icon plumbing
   (`ic_launcher_background.xml`, `mipmap-anydpi-v26/*.xml`) unchanged.

6. **`strings.xml`**: add all new text — menu item titles, difficulty
   dialog title/options, quit question, yes/no, about dialog text.

## Out of scope

- Persisting the chosen difficulty level across app restarts (matches
  Reto3's precedent of no cross-restart persistence).
- Real PNG/raster icon assets — vector drawables stand in for the PDF's
  hand-drawn PNGs.
- Rotation/config-change state preservation (same accepted limitation
  carried over from Reto3).
- Actually running the app — verification is `assembleDebug` +
  `testDebugUnitTest` locally; interactive verification (radio-button
  pre-selection, Toast text, dialog appearance, icons rendering) is a
  manual checklist for the user in Android Studio, same as Reto3.

## Risks

- `Activity.onCreateDialog(int)` / `showDialog(int)` /
  `dismissDialog(int)` are deprecated (API 13+) but historically remain
  present on `Activity` (Android does not remove public APIs). Expected
  to compile with deprecation warnings on `compileSdk 37`. If a task's
  implementer finds these methods no longer resolve, that is a stop-and-
  escalate condition, not a license to silently switch to a modern
  `DialogFragment`/inline-`AlertDialog` pattern — the user explicitly
  chose PDF fidelity here.
