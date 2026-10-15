# Reto4 Menus and Dialog Boxes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the Reto3 Tic-Tac-Toe app into a new `Retos/Reto4` project with a 4-item options menu (New Game / Difficulty / Quit / About), three `AlertDialog`s, three AI difficulty levels, and a custom app icon, per the "Menus and Dialog Boxes" PDF.

**Architecture:** Clone the working `Retos/Reto3` project into `Retos/Reto4` and rename its package, then extend it in place: `TicTacToeGame.kt` gains a `DifficultyLevel` enum and three private move-selection strategies behind the existing `getComputerMove()` entry point; `MainActivity.kt` gains a 4-item menu and the PDF's literal (deprecated) `onCreateDialog`/`showDialog` dialog pattern. No new classes — same two-file model/UI split as Reto3.

**Tech Stack:** Kotlin, AndroidX AppCompat + Material Components (no Compose), Gradle Kotlin DSL, JUnit4, same AGP/SDK versions as `Retos/Reto3`.

**Spec:** `Retos/Reto4/docs/superpowers/specs/2026-09-21-reto4-menus-dialogs-design.md`

## Global Constraints

- `compileSdk 37`, `minSdk 24`, `targetSdk 37` (unchanged from Reto3).
- Kotlin + classic Android Views/XML only — no Jetpack Compose.
- `namespace`/`applicationId` = `com.example.reto4`; activity class name = `MainActivity`; style name `Theme.Reto4`.
- Dialogs use the PDF's literal deprecated pattern: `protected/override fun onCreateDialog(id: Int): Dialog?` + `showDialog(id)`, with `DIALOG_DIFFICULTY_ID = 0`, `DIALOG_QUIT_ID = 1`, `DIALOG_ABOUT_ID = 2`. Do not substitute `DialogFragment` or an inline-`AlertDialog`-without-`onCreateDialog` pattern — if these deprecated APIs fail to resolve at compile time, that is a stop-and-escalate condition (see Risks in the spec), not a license to redesign.
- `TicTacToeGame`'s public surface grows to: `clearBoard()`, `setMove(player: Char, location: Int)`, `getComputerMove(): Int`, `checkForWinner(): Int`, `getDifficultyLevel(): DifficultyLevel`, `setDifficultyLevel(difficultyLevel: DifficultyLevel)`, plus the existing `internal boardCell(location: Int): Char` test-only accessor. `getRandomMove()`, `getWinningMove()`, `getBlockingMove()` are `private`. `checkForWinner()` codes are unchanged: `0`/`1`/`2`/`3`.
- `DifficultyLevel` is `enum class DifficultyLevel { Easy, Harder, Expert }` (in that declared order — the difficulty dialog's radio-button array order and `.ordinal` pre-selection logic depend on this exact order). Default level is `Expert`.
- Menu/app icons are hand-authored vector drawables (`res/drawable/*.xml`) — no PNG/raster assets.
- No SDK-based instrumented/UI tests — verification is `./gradlew.bat assembleDebug` + `./gradlew.bat testDebugUnitTest`, plus a manual checklist for the user in Android Studio.
- Local Android SDK is at `C:\Users\Karol\AppData\Local\Android\Sdk`.
- Work happens directly on `main` in the `D:\Escritorio\DADM-2026-2` repo (explicitly approved by the repo owner for this solo coursework repo).

---

### Task 1: Clone Reto3 into Reto4, rename package, verify it still builds and tests pass

**Files:**
- Create: `Retos/Reto4/` — the entire Gradle project, copied from `Retos/Reto3/` (excluding `Retos/Reto3/docs/`, `Retos/Reto3/app/build/`, `.gradle/`, `.idea/`) and renamed.

**Interfaces:**
- Produces: a buildable, renamed clone of Reto3 — package `com.example.reto4`, `rootProject.name = "Reto4"`, `Theme.Reto4` — with `TicTacToeGame.kt`, `MainActivity.kt`, and `TicTacToeGameTest.kt` (13 passing tests, unchanged behavior) at their new package paths. Later tasks extend these same files.

- [ ] **Step 1: Copy the project tree**

```bash
cd "/d/Escritorio/DADM-2026-2/Retos"
mkdir -p Reto4
cp -r Reto3/.gitignore Reto3/app Reto3/gradle Reto3/build.gradle.kts Reto3/gradle.properties Reto3/gradlew Reto3/gradlew.bat Reto3/local.properties Reto3/settings.gradle.kts Reto4/
rm -rf Reto4/app/build
cd Reto4
chmod +x gradlew
```

- [ ] **Step 2: Move the Kotlin sources to the new package path**

```bash
mkdir -p app/src/main/java/com/example/reto4
mv app/src/main/java/com/example/reto3/MainActivity.kt app/src/main/java/com/example/reto3/TicTacToeGame.kt app/src/main/java/com/example/reto4/
rmdir app/src/main/java/com/example/reto3

mkdir -p app/src/test/java/com/example/reto4
mv app/src/test/java/com/example/reto3/TicTacToeGameTest.kt app/src/test/java/com/example/reto4/
rmdir app/src/test/java/com/example/reto3
```

- [ ] **Step 3: Rename the package, theme, and root project name everywhere they appear**

```bash
sed -i 's/com\.example\.reto3/com.example.reto4/g' \
  app/build.gradle.kts \
  app/src/main/AndroidManifest.xml \
  app/src/main/java/com/example/reto4/MainActivity.kt \
  app/src/main/java/com/example/reto4/TicTacToeGame.kt \
  app/src/test/java/com/example/reto4/TicTacToeGameTest.kt

sed -i 's/Theme\.Reto3/Theme.Reto4/g' \
  app/src/main/AndroidManifest.xml \
  app/src/main/res/values/themes.xml \
  app/src/main/res/values-night/themes.xml

sed -i 's/rootProject.name = "Reto3"/rootProject.name = "Reto4"/' settings.gradle.kts
```

- [ ] **Step 4: Verify the renamed clone builds and its tests pass**

Run: `cd "/d/Escritorio/DADM-2026-2/Retos/Reto4" && ./gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew.bat testDebugUnitTest`
Expected: `13 tests completed, 0 failed` (same 13 tests as Reto3 — `clearBoard`, `setMove`, `checkForWinner` × diagonals/rows/columns/tie, `getComputerMove` win/block/random — now compiled under `com.example.reto4`).

- [ ] **Step 5: Sanity-check the rename left no leftover `reto3` references**

Run: `grep -rn "reto3\|Reto3" "/d/Escritorio/DADM-2026-2/Retos/Reto4/app/src" "/d/Escritorio/DADM-2026-2/Retos/Reto4/app/build.gradle.kts" "/d/Escritorio/DADM-2026-2/Retos/Reto4/settings.gradle.kts"`
Expected: no output (no matches). If something is found, fix it — this means a file that needed renaming was missed in Step 3.

- [ ] **Step 6: Commit**

```bash
git -C "/d/Escritorio/DADM-2026-2" add \
  Retos/Reto4/.gitignore \
  Retos/Reto4/app \
  Retos/Reto4/build.gradle.kts \
  Retos/Reto4/gradle \
  Retos/Reto4/gradle.properties \
  Retos/Reto4/gradlew \
  Retos/Reto4/gradlew.bat \
  Retos/Reto4/settings.gradle.kts
git -C "/d/Escritorio/DADM-2026-2" commit -m "Reto4: clone Reto3 scaffold and rename to com.example.reto4"
```

(`local.properties` is gitignored and won't be staged; `docs/` is intentionally not added here.)

---

### Task 2: `TicTacToeGame` difficulty levels, with tests (TDD)

**Files:**
- Modify: `Retos/Reto4/app/src/main/java/com/example/reto4/TicTacToeGame.kt`
- Modify: `Retos/Reto4/app/src/test/java/com/example/reto4/TicTacToeGameTest.kt` (append new tests; keep all 13 existing ones)

**Interfaces:**
- Consumes: nothing new — still pure Kotlin.
- Produces: `TicTacToeGame.DifficultyLevel` enum (`Easy`, `Harder`, `Expert`, in that order), `getDifficultyLevel(): DifficultyLevel`, `setDifficultyLevel(difficultyLevel: DifficultyLevel)` — this is what `MainActivity.kt` (Task 5) reads/writes from the difficulty dialog. `getComputerMove()`'s public signature and behavior at the default `Expert` level are unchanged, so all 13 existing tests must keep passing unmodified.

- [ ] **Step 1: Write the new failing tests**

Add these test methods to the existing `TicTacToeGameTest` class in `Retos/Reto4/app/src/test/java/com/example/reto4/TicTacToeGameTest.kt` (add the import `org.junit.Assert.assertTrue` if not already present — it already is; do not remove or modify any of the 13 existing `@Test` methods):

```kotlin
@Test
fun difficultyLevel_defaultsToExpert() {
    assertEquals(TicTacToeGame.DifficultyLevel.Expert, game.getDifficultyLevel())
}

@Test
fun setDifficultyLevel_changesGetDifficultyLevel() {
    game.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy)
    assertEquals(TicTacToeGame.DifficultyLevel.Easy, game.getDifficultyLevel())
}

@Test
fun getComputerMove_harderLevel_takesTheWinWhenOneIsAvailable() {
    game.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Harder)
    game.setMove(TicTacToeGame.COMPUTER_PLAYER, 0)
    game.setMove(TicTacToeGame.COMPUTER_PLAYER, 1)
    game.setMove(TicTacToeGame.HUMAN_PLAYER, 3)
    game.setMove(TicTacToeGame.HUMAN_PLAYER, 4)
    assertEquals(2, game.getComputerMove())
}

@Test
fun getComputerMove_harderLevel_doesNotAlwaysBlock() {
    game.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Harder)
    game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
    game.setMove(TicTacToeGame.HUMAN_PLAYER, 1)
    game.setMove(TicTacToeGame.HUMAN_PLAYER, 5)
    game.setMove(TicTacToeGame.HUMAN_PLAYER, 6)
    game.setMove(TicTacToeGame.COMPUTER_PLAYER, 3)
    game.setMove(TicTacToeGame.COMPUTER_PLAYER, 4)
    game.setMove(TicTacToeGame.COMPUTER_PLAYER, 7)
    // Open cells: 2 (blocks X's row 0-1-2) and 8 (doesn't). Harder never
    // calls getBlockingMove(), so across many trials it must sometimes
    // land on 8 via the random fallback.
    var sawNonBlockingMove = false
    for (i in 0 until 30) {
        if (game.getComputerMove() == 8) {
            sawNonBlockingMove = true
            break
        }
    }
    assertTrue(sawNonBlockingMove)
}

@Test
fun getComputerMove_easyLevel_doesNotAlwaysTakeAvailableWin() {
    game.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy)
    game.setMove(TicTacToeGame.COMPUTER_PLAYER, 0)
    game.setMove(TicTacToeGame.COMPUTER_PLAYER, 1)
    game.setMove(TicTacToeGame.COMPUTER_PLAYER, 5)
    game.setMove(TicTacToeGame.COMPUTER_PLAYER, 6)
    game.setMove(TicTacToeGame.HUMAN_PLAYER, 3)
    game.setMove(TicTacToeGame.HUMAN_PLAYER, 4)
    game.setMove(TicTacToeGame.HUMAN_PLAYER, 7)
    // Open cells: 2 (wins for O via row 0-1-2) and 8 (doesn't). Easy
    // never calls getWinningMove(), so across many trials it must
    // sometimes land on 8 via the random fallback.
    var sawNonWinningMove = false
    for (i in 0 until 30) {
        if (game.getComputerMove() == 8) {
            sawNonWinningMove = true
            break
        }
    }
    assertTrue(sawNonWinningMove)
}
```

- [ ] **Step 2: Run the tests to verify the new ones fail (the code doesn't exist yet)**

Run: `cd "/d/Escritorio/DADM-2026-2/Retos/Reto4" && ./gradlew.bat testDebugUnitTest`
Expected: FAIL — compile error, `DifficultyLevel`/`getDifficultyLevel`/`setDifficultyLevel` unresolved references.

- [ ] **Step 3: Implement the difficulty levels**

Replace the contents of `Retos/Reto4/app/src/main/java/com/example/reto4/TicTacToeGame.kt`:

```kotlin
package com.example.reto4

import kotlin.random.Random

/**
 * Tic-Tac-Toe game model. Owns the board and the computer's AI, whose
 * strength depends on the current DifficultyLevel. All UI concerns live
 * in MainActivity, which only talks to this class through its public
 * methods.
 */
class TicTacToeGame {

    /** The computer's difficulty levels. */
    enum class DifficultyLevel { Easy, Harder, Expert }

    private val mBoard = CharArray(BOARD_SIZE) { OPEN_SPOT }
    private val mRand = Random(System.currentTimeMillis())
    private var mDifficultyLevel: DifficultyLevel = DifficultyLevel.Expert

    companion object {
        const val HUMAN_PLAYER = 'X'
        const val COMPUTER_PLAYER = 'O'
        const val OPEN_SPOT = ' '
        const val BOARD_SIZE = 9

        private val WIN_COMBOS = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
    }

    /** Clears the board of all X's and O's by setting all spots to OPEN_SPOT. */
    fun clearBoard() {
        for (i in 0 until BOARD_SIZE) {
            mBoard[i] = OPEN_SPOT
        }
    }

    /**
     * Sets the given player at the given location on the game board.
     * The location must be available, or the board will not be changed.
     */
    fun setMove(player: Char, location: Int) {
        if (mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player
        }
    }

    fun getDifficultyLevel(): DifficultyLevel = mDifficultyLevel

    fun setDifficultyLevel(difficultyLevel: DifficultyLevel) {
        mDifficultyLevel = difficultyLevel
    }

    /**
     * Returns the best move for the computer to make, given the current
     * difficulty level. You must call setMove() to actually make the
     * computer move to that location.
     */
    fun getComputerMove(): Int {
        var move = -1

        if (mDifficultyLevel == DifficultyLevel.Easy) {
            move = getRandomMove()
        } else if (mDifficultyLevel == DifficultyLevel.Harder) {
            move = getWinningMove()
            if (move == -1) move = getRandomMove()
        } else if (mDifficultyLevel == DifficultyLevel.Expert) {
            // Try to win, but if that's not possible, block.
            // If that's not possible, move anywhere.
            move = getWinningMove()
            if (move == -1) move = getBlockingMove()
            if (move == -1) move = getRandomMove()
        }

        return move
    }

    /** Returns a move that lets the computer win immediately, or -1 if none exists. */
    private fun getWinningMove(): Int {
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                val current = mBoard[i]
                mBoard[i] = COMPUTER_PLAYER
                val winner = checkForWinner()
                mBoard[i] = current
                if (winner == 3) return i
            }
        }
        return -1
    }

    /** Returns a move that blocks the human from winning next turn, or -1 if none is needed. */
    private fun getBlockingMove(): Int {
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                val current = mBoard[i]
                mBoard[i] = HUMAN_PLAYER
                val winner = checkForWinner()
                mBoard[i] = current
                if (winner == 2) return i
            }
        }
        return -1
    }

    /** Returns a random open location on the board. */
    private fun getRandomMove(): Int {
        var move: Int
        do {
            move = mRand.nextInt(BOARD_SIZE)
        } while (mBoard[move] != OPEN_SPOT)
        return move
    }

    /**
     * Checks for a winner and returns a status value indicating who has won.
     * @return 0 if no winner or tie yet, 1 if it's a tie, 2 if X won, or 3 if O won.
     */
    fun checkForWinner(): Int {
        for (combo in WIN_COMBOS) {
            val (a, b, c) = Triple(combo[0], combo[1], combo[2])
            if (mBoard[a] == HUMAN_PLAYER && mBoard[b] == HUMAN_PLAYER && mBoard[c] == HUMAN_PLAYER) {
                return 2
            }
            if (mBoard[a] == COMPUTER_PLAYER && mBoard[b] == COMPUTER_PLAYER && mBoard[c] == COMPUTER_PLAYER) {
                return 3
            }
        }
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) return 0
        }
        return 1
    }

    /** Test-only accessor; not part of the public game API. */
    internal fun boardCell(location: Int): Char = mBoard[location]
}
```

- [ ] **Step 4: Run the tests to verify everything passes**

Run: `./gradlew.bat testDebugUnitTest`
Expected: PASS — all 18 tests green (the original 13 plus the 5 new ones).

- [ ] **Step 5: Commit**

```bash
git -C "/d/Escritorio/DADM-2026-2" add \
  Retos/Reto4/app/src/main/java/com/example/reto4/TicTacToeGame.kt \
  Retos/Reto4/app/src/test/java/com/example/reto4/TicTacToeGameTest.kt
git -C "/d/Escritorio/DADM-2026-2" commit -m "Reto4: add AI difficulty levels to TicTacToeGame with tests"
```

---

### Task 3: Vector icons (menu items + app launcher) and the options menu resource

**Files:**
- Create: `Retos/Reto4/app/src/main/res/drawable/ic_new_game.xml`
- Create: `Retos/Reto4/app/src/main/res/drawable/ic_difficulty.xml`
- Create: `Retos/Reto4/app/src/main/res/drawable/ic_quit.xml`
- Create: `Retos/Reto4/app/src/main/res/drawable/ic_about.xml`
- Modify: `Retos/Reto4/app/src/main/res/drawable/ic_launcher_foreground.xml` (replace content — custom app icon, extra challenge #1)
- Create: `Retos/Reto4/app/src/main/res/menu/options_menu.xml`

**Interfaces:**
- Produces: drawable resources `@drawable/ic_new_game`, `@drawable/ic_difficulty`, `@drawable/ic_quit`, `@drawable/ic_about` (the last one also consumed by Task 4's `about_dialog.xml`), and `R.menu.options_menu` with item ids `new_game`, `ai_difficulty`, `quit`, `about` — all consumed by Task 5's `MainActivity.kt`.

- [ ] **Step 1: Create the four menu icon vector drawables**

Create `Retos/Reto4/app/src/main/res/drawable/ic_new_game.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M12,5V1L7,6l5,5V7c3.31,0 6,2.69 6,6c0,2.97 -2.16,5.44 -5,5.91v2.02c3.95,-0.49 7,-3.85 7,-7.93C20,8.58 16.42,5 12,5zM6,13c0,-1.65 0.67,-3.15 1.76,-4.24L6.34,7.34C4.9,8.79 4,10.79 4,13c0,4.08 3.05,7.44 7,7.93v-2.02c-2.84,-0.47 -5,-2.94 -5,-5.91z" />
</vector>
```

Create `Retos/Reto4/app/src/main/res/drawable/ic_difficulty.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M19.14,12.94c0.04,-0.3 0.06,-0.61 0.06,-0.94c0,-0.32 -0.02,-0.64 -0.07,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87c-0.12,0.21 -0.08,0.47 0.12,0.61l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z" />
</vector>
```

Create `Retos/Reto4/app/src/main/res/drawable/ic_quit.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z" />
</vector>
```

Create `Retos/Reto4/app/src/main/res/drawable/ic_about.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF000000"
        android:pathData="M11,7h2v2h-2zM11,11h2v6h-2zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8s8,3.59 8,8S16.41,20 12,20z" />
</vector>
```

- [ ] **Step 2: Replace the app launcher's foreground with a custom tic-tac-toe icon**

Replace the contents of `Retos/Reto4/app/src/main/res/drawable/ic_launcher_foreground.xml` (this keeps the existing adaptive-icon background/plumbing — `ic_launcher_background.xml` and the `mipmap-anydpi-v26/*.xml` files — unchanged, only the foreground artwork changes):

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="6"
        android:strokeLineCap="round"
        android:pathData="M43,25 L43,83 M65,25 L65,83 M25,43 L83,43 M25,65 L83,65" />
</vector>
```

- [ ] **Step 3: Create the options menu**

Create `Retos/Reto4/app/src/main/res/menu/options_menu.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/new_game"
        android:title="@string/menu_new_game"
        android:icon="@drawable/ic_new_game" />
    <item
        android:id="@+id/ai_difficulty"
        android:title="@string/menu_difficulty"
        android:icon="@drawable/ic_difficulty" />
    <item
        android:id="@+id/quit"
        android:title="@string/menu_quit"
        android:icon="@drawable/ic_quit" />
    <item
        android:id="@+id/about"
        android:title="@string/menu_about"
        android:icon="@drawable/ic_about" />
</menu>
```

`@string/menu_new_game` already exists (carried over from Reto3's clone in Task 1); `menu_difficulty`, `menu_quit`, and `menu_about` do not exist yet — they're added in Task 4.

- [ ] **Step 4: Verify resources compile — expect a specific, known failure**

Run: `cd "/d/Escritorio/DADM-2026-2/Retos/Reto4" && ./gradlew.bat assembleDebug`
Expected: FAILS at this point — `@string/menu_difficulty`, `@string/menu_quit`, and `@string/menu_about` don't exist yet. That's expected; Task 4 adds them. Confirm the failure is specifically "resource not found" for those three names and nothing else (i.e. the vector drawables and menu XML are otherwise valid — if the error mentions anything else, such as a malformed `pathData` or a bad drawable reference, that's a real problem to fix before moving on).

- [ ] **Step 5: Commit**

```bash
git -C "/d/Escritorio/DADM-2026-2" add \
  Retos/Reto4/app/src/main/res/drawable/ic_new_game.xml \
  Retos/Reto4/app/src/main/res/drawable/ic_difficulty.xml \
  Retos/Reto4/app/src/main/res/drawable/ic_quit.xml \
  Retos/Reto4/app/src/main/res/drawable/ic_about.xml \
  Retos/Reto4/app/src/main/res/drawable/ic_launcher_foreground.xml \
  Retos/Reto4/app/src/main/res/menu/options_menu.xml
git -C "/d/Escritorio/DADM-2026-2" commit -m "Reto4: add menu icons, custom app icon, and options menu resource"
```

---

### Task 4: Strings and the About dialog layout

**Files:**
- Modify: `Retos/Reto4/app/src/main/res/values/strings.xml`
- Create: `Retos/Reto4/app/src/main/res/layout/about_dialog.xml`

**Interfaces:**
- Produces: string resources `menu_difficulty`, `menu_quit`, `menu_about`, `difficulty_choose`, `difficulty_easy`, `difficulty_harder`, `difficulty_expert`, `quit_question`, `yes`, `no`, `about_title`, `about_by`, `about_description`, `about_ok` — all consumed by Task 5's `MainActivity.kt`, plus `about_title`/`about_by`/`about_description` consumed by this task's own `about_dialog.xml`.

- [ ] **Step 1: Add the new strings**

Replace the contents of `Retos/Reto4/app/src/main/res/values/strings.xml` (this keeps every existing Reto3 string — game status/score messages — and adds the new menu/dialog ones):

```xml
<resources>
    <string name="app_name">Tic-Tac-Toe</string>

    <string name="first_human">You go first.</string>
    <string name="first_computer">Android went first. Your turn.</string>
    <string name="turn_human">Your turn.</string>
    <string name="turn_computer">Android\'s turn.</string>
    <string name="result_tie">It\'s a tie.</string>
    <string name="result_human_wins">You won!</string>
    <string name="result_computer_wins">Android won!</string>

    <string name="score_human">Human: %1$d</string>
    <string name="score_ties">Ties: %1$d</string>
    <string name="score_computer">Android: %1$d</string>
    <string name="score_human_default">Human: 0</string>
    <string name="score_ties_default">Ties: 0</string>
    <string name="score_computer_default">Android: 0</string>

    <string name="menu_new_game">New Game</string>
    <string name="menu_difficulty">Difficulty</string>
    <string name="menu_quit">Quit</string>
    <string name="menu_about">About</string>

    <string name="difficulty_choose">Choose a difficulty level:</string>
    <string name="difficulty_easy">Easy</string>
    <string name="difficulty_harder">Harder</string>
    <string name="difficulty_expert">Expert</string>

    <string name="quit_question">Are you sure you want to quit?</string>
    <string name="yes">Yes</string>
    <string name="no">No</string>

    <string name="about_title">Tic-Tac-Toe</string>
    <string name="about_by">By ACorduz</string>
    <string name="about_description">Choose from one of three difficulty levels. Don\'t let the Android beat you!</string>
    <string name="about_ok">OK</string>
</resources>
```

- [ ] **Step 2: Create the About dialog layout**

Create `Retos/Reto4/app/src/main/res/layout/about_dialog.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:gravity="center_horizontal"
    android:padding="24dp">

    <ImageView
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:src="@drawable/ic_about"
        android:contentDescription="@string/about_title" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:text="@string/about_title"
        android:textSize="20sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/about_by"
        android:textSize="16sp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:gravity="center_horizontal"
        android:text="@string/about_description"
        android:textSize="14sp" />

</LinearLayout>
```

- [ ] **Step 3: Verify resources compile**

Run: `cd "/d/Escritorio/DADM-2026-2/Retos/Reto4" && ./gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL` (Task 3's `options_menu.xml` now resolves; the placeholder-free `MainActivity.kt` from Task 2 doesn't reference `options_menu.xml` or `about_dialog.xml` yet, so nothing exercises them at runtime, but everything must compile).

- [ ] **Step 4: Commit**

```bash
git -C "/d/Escritorio/DADM-2026-2" add \
  Retos/Reto4/app/src/main/res/values/strings.xml \
  Retos/Reto4/app/src/main/res/layout/about_dialog.xml
git -C "/d/Escritorio/DADM-2026-2" commit -m "Reto4: add menu/dialog strings and About dialog layout"
```

---

### Task 5: Wire up the menu and dialogs in `MainActivity`, verification

**Files:**
- Modify: `Retos/Reto4/app/src/main/java/com/example/reto4/MainActivity.kt`
- Delete: `Retos/Reto4/app/src/main/res/menu/main_menu.xml` (superseded by Task 3's `options_menu.xml`; nothing references it after this task)

**Interfaces:**
- Consumes: `TicTacToeGame.DifficultyLevel`/`getDifficultyLevel()`/`setDifficultyLevel()` from Task 2; `R.menu.options_menu` and its item ids (`new_game`, `ai_difficulty`, `quit`, `about`) from Task 3; all the string resources and `R.layout.about_dialog` from Task 4.
- Produces: the finished app — nothing downstream depends on this file.

- [ ] **Step 1: Replace `MainActivity.kt`**

Replace the contents of `Retos/Reto4/app/src/main/java/com/example/reto4/MainActivity.kt`:

```kotlin
package com.example.reto4

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mGame: TicTacToeGame
    private lateinit var mBoardButtons: Array<Button>
    private lateinit var mInfoTextView: TextView
    private lateinit var mHumanScoreView: TextView
    private lateinit var mTiesScoreView: TextView
    private lateinit var mComputerScoreView: TextView

    private var mGameOver = false
    private var mHumanGoesFirst = true

    private var mHumanScore = 0
    private var mTiesScore = 0
    private var mComputerScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBoardButtons = arrayOf(
            findViewById(R.id.one), findViewById(R.id.two), findViewById(R.id.three),
            findViewById(R.id.four), findViewById(R.id.five), findViewById(R.id.six),
            findViewById(R.id.seven), findViewById(R.id.eight), findViewById(R.id.nine)
        )
        mInfoTextView = findViewById(R.id.information)
        mHumanScoreView = findViewById(R.id.human_score)
        mTiesScoreView = findViewById(R.id.ties_score)
        mComputerScoreView = findViewById(R.id.computer_score)

        mGame = TicTacToeGame()

        startNewGame()
    }

    // Sets up (or resets) the board for a new game. Alternates who goes
    // first each time it's called, per the "extra challenge".
    private fun startNewGame() {
        mGame.clearBoard()
        mGameOver = false

        for (i in mBoardButtons.indices) {
            mBoardButtons[i].text = ""
            mBoardButtons[i].isEnabled = true
            mBoardButtons[i].setTextColor(Color.BLACK)
            mBoardButtons[i].setOnClickListener { onBoardButtonClicked(i) }
        }

        if (mHumanGoesFirst) {
            mInfoTextView.setText(R.string.first_human)
        } else {
            val move = mGame.getComputerMove()
            setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            mInfoTextView.setText(R.string.first_computer)
        }
        mHumanGoesFirst = !mHumanGoesFirst
    }

    private fun onBoardButtonClicked(location: Int) {
        if (mGameOver || !mBoardButtons[location].isEnabled) {
            return
        }

        setMove(TicTacToeGame.HUMAN_PLAYER, location)

        var winner = mGame.checkForWinner()
        if (winner == 0) {
            mInfoTextView.setText(R.string.turn_computer)
            val move = mGame.getComputerMove()
            setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            winner = mGame.checkForWinner()
        }

        when (winner) {
            0 -> mInfoTextView.setText(R.string.turn_human)
            1 -> {
                mInfoTextView.setText(R.string.result_tie)
                mTiesScore++
                endGame()
            }
            2 -> {
                mInfoTextView.setText(R.string.result_human_wins)
                mHumanScore++
                endGame()
            }
            else -> {
                mInfoTextView.setText(R.string.result_computer_wins)
                mComputerScore++
                endGame()
            }
        }
    }

    private fun endGame() {
        mGameOver = true
        updateScoreDisplay()
    }

    private fun updateScoreDisplay() {
        mHumanScoreView.text = getString(R.string.score_human, mHumanScore)
        mTiesScoreView.text = getString(R.string.score_ties, mTiesScore)
        mComputerScoreView.text = getString(R.string.score_computer, mComputerScore)
    }

    // Updates the model, disables the button, and colors X green / O red.
    private fun setMove(player: Char, location: Int) {
        mGame.setMove(player, location)
        val button = mBoardButtons[location]
        button.isEnabled = false
        button.text = player.toString()
        button.setTextColor(
            if (player == TicTacToeGame.HUMAN_PLAYER) Color.rgb(0, 200, 0)
            else Color.rgb(200, 0, 0)
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_game -> {
                startNewGame()
                return true
            }
            R.id.ai_difficulty -> {
                showDialog(DIALOG_DIFFICULTY_ID)
                return true
            }
            R.id.quit -> {
                showDialog(DIALOG_QUIT_ID)
                return true
            }
            R.id.about -> {
                showDialog(DIALOG_ABOUT_ID)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(id: Int): Dialog? {
        val builder = AlertDialog.Builder(this)

        when (id) {
            DIALOG_DIFFICULTY_ID -> {
                builder.setTitle(R.string.difficulty_choose)
                val levels: Array<CharSequence> = arrayOf(
                    getString(R.string.difficulty_easy),
                    getString(R.string.difficulty_harder),
                    getString(R.string.difficulty_expert)
                )
                val selected = mGame.getDifficultyLevel().ordinal
                builder.setSingleChoiceItems(levels, selected) { dialog, item ->
                    dialog.dismiss()
                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[item])
                    Toast.makeText(applicationContext, levels[item], Toast.LENGTH_SHORT).show()
                }
                return builder.create()
            }
            DIALOG_QUIT_ID -> {
                builder.setMessage(R.string.quit_question)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ -> finish() }
                    .setNegativeButton(R.string.no, null)
                return builder.create()
            }
            DIALOG_ABOUT_ID -> {
                val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val layout = inflater.inflate(R.layout.about_dialog, null)
                builder.setView(layout)
                builder.setPositiveButton(R.string.about_ok, null)
                return builder.create()
            }
        }

        return super.onCreateDialog(id)
    }

    companion object {
        private const val DIALOG_DIFFICULTY_ID = 0
        private const val DIALOG_QUIT_ID = 1
        private const val DIALOG_ABOUT_ID = 2
    }
}
```

- [ ] **Step 2: Delete the superseded single-item menu**

```bash
rm "/d/Escritorio/DADM-2026-2/Retos/Reto4/app/src/main/res/menu/main_menu.xml"
```

- [ ] **Step 3: Build the full app**

Run: `cd "/d/Escritorio/DADM-2026-2/Retos/Reto4" && ./gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Re-run the unit tests (make sure nothing broke the model)**

Run: `./gradlew.bat testDebugUnitTest`
Expected: PASS — all 18 `TicTacToeGameTest` tests still green.

- [ ] **Step 5: Manual verification checklist (requires Android Studio / an emulator or device — not runnable from this environment)**

Open the project in Android Studio, run on an emulator, and confirm:
- [ ] The overflow/options menu shows 4 items with icons: New Game, Difficulty, Quit, About.
- [ ] Tapping "Difficulty" opens a dialog titled "Choose a difficulty level:" with Easy/Harder/Expert radio buttons, and **Expert is pre-selected** the first time (matches the default).
- [ ] Selecting a different level closes the dialog, shows a Toast with the chosen level's name, and a subsequent game visibly plays weaker (Easy) or stronger (Expert) — e.g. on Easy, deliberately let the computer have an obvious winning move available and confirm it doesn't always take it.
- [ ] Reopening "Difficulty" afterward shows the **previously chosen** level pre-selected (not reset to Expert).
- [ ] Tapping "Quit" shows an "Are you sure you want to quit?" dialog with Yes/No; No dismisses and does nothing; Yes closes the app.
- [ ] Tapping "About" shows a dialog with the tic-tac-toe icon, "Tic-Tac-Toe", "By ACorduz", and the description text, with a single "OK" button that dismisses it.
- [ ] The app's launcher icon (on the home screen / app drawer) shows the new tic-tac-toe grid artwork, not the default Android robot.
- [ ] "New Game" still works exactly as in Reto3 (alternates who starts, updates the score row).

- [ ] **Step 6: Commit**

```bash
git -C "/d/Escritorio/DADM-2026-2" add \
  Retos/Reto4/app/src/main/java/com/example/reto4/MainActivity.kt \
  Retos/Reto4/app/src/main/res/menu/main_menu.xml
git -C "/d/Escritorio/DADM-2026-2" commit -m "Reto4: wire up options menu and dialogs (difficulty, quit, about) in MainActivity"
```
