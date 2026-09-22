# Reto3: Tic-Tac-Toe Android app — design

## Goal

Implement the "Tic-Tac-Toe App" challenge (Frank McCown, Harding University,
see `Reto3-Tic-tac-toe for Android.pdf`) as a native Android app: a 3x3 board
of buttons where a human (green X) plays against a simple computer AI
(red O), with UI and game logic kept in separate classes. Includes the
PDF's "Extra Challenge": alternate who goes first each game, and track
win/tie/loss counts.

## Context

- `Retos/Reto3/` is currently empty on disk, but git's index still has a
  staged (never committed) Android Studio "Empty Activity" scaffold
  (Compose-based, package `com.example.reto3`) that was deleted from the
  working tree — recoverable via `git checkout -- <path>`.
- Sibling project `Retos/Reto0/` is the pattern to match: plain Kotlin,
  Views (no Compose actually wired up despite leftover `ui/theme/*.kt`
  files), `app/build.gradle.kts` using AGP `9.3.2`, `compileSdk 37`,
  `minSdk 24`, namespace `com.example.reto0`.
- Language/UI decisions already made with the user: **Kotlin + classic
  Views/XML** (not Compose), to follow the PDF's structure closely.
- Naming: use `com.example.reto3` / `MainActivity` (matches this repo's
  convention) instead of the PDF's `co.edu.unal.tictactoe` /
  `AndroidTicTacToeActivity`.

## Scope

1. Recover reusable boilerplate from git's index (`app/build.gradle.kts`,
   `AndroidManifest.xml`, mipmap icons, `colors.xml`/`themes.xml`/
   `strings.xml`, `gradle/libs.versions.toml`, `.gitignore`, template
   tests), drop the Compose-specific files (`ui/theme/Color.kt`,
   `Theme.kt`, `Type.kt`, the `setContent{}` `MainActivity.kt`).
2. Create the missing root-level Gradle files (`settings.gradle.kts`,
   root `build.gradle.kts`, gradle wrapper) mirroring `Reto0`.
3. `TicTacToeGame.kt` — game model, ported from the PDF's
   `TicTacToeConsole.java`:
   - Constants: `HUMAN_PLAYER='X'`, `COMPUTER_PLAYER='O'`, `OPEN_SPOT=' '`,
     `BOARD_SIZE=9`.
   - Public API: `clearBoard()`, `setMove(player: Char, location: Int)`,
     `getComputerMove(): Int`, `checkForWinner(): Int` (0=none, 1=tie,
     2=human won, 3=computer won).
   - Everything else (win detection helpers, random-move fallback,
     block/win heuristics) stays private.
4. `activity_main.xml` — vertical `LinearLayout` containing a
   `TableLayout` of 3 `TableRow`s x 3 `Button`s (ids `one`..`nine`,
   100dp square, 70dp text), a status `TextView` (`information`), and
   — for the extra challenge — a `RelativeLayout` row of 3 `TextView`s
   (`human_score`, `ties_score`, `computer_score`) positioned next to
   each other beneath the status text.
5. `MainActivity.kt` — UI/controller:
   - Holds `mGame: TicTacToeGame`, `mBoardButtons: Array<Button>`,
     `mInfoTextView: TextView`, score `TextView`s, `mGameOver: Boolean`,
     and a `mHumanGoesFirst: Boolean` that flips each new game.
   - `startNewGame()`: clears model + board UI, re-attaches click
     listeners, sets `mGameOver = false`, and either lets the human move
     first or triggers an immediate computer move depending on
     `mHumanGoesFirst`, then flips that flag for next time.
   - Click handling blocks input once `mGameOver` is true or a button is
     already disabled.
   - `setMove()` updates model + button text/color (green X / red O) and
     disables the button.
   - On game end, updates `mGameOver`, increments and redraws the
     human/tie/computer counters.
   - Options menu with a single "New Game" item wired to
     `startNewGame()`.
6. `strings.xml` — externalize all status messages (`first_human`,
   `turn_human`, `turn_computer`, `result_tie`, `result_human_wins`,
   `result_computer_wins`) plus labels for the score row.

## Out of scope

- Persisting scores across app restarts (PDF doesn't ask for it).
- Automated instrumentation/UI tests — the template's placeholder tests
  are kept as-is; `TicTacToeGame` is written so its public methods could
  be unit-tested later, but writing those tests isn't part of this pass.
- Actually compiling/running the app — this dev environment has no
  Android SDK configured, so verification happens by the user opening
  the project in Android Studio and running it on an emulator/device.

## Risks / open questions

- None blocking; the PDF plus the two decisions already made (Kotlin,
  Views) fully determine the shape of the code.
