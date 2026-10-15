# Reto7 Parte 2 (modo online) — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el lobby, la partida online entre dos dispositivos (con revancha y aviso de abandono) y mantener el modo contra Android.

**Architecture:** `OnlineGame` (estado) + `OnlineGameRules` (transiciones puras, probadas con JUnit) → `GameRepository` (única clase que usa Firebase; toda escritura de juego es una transacción que aplica una regla) → `OnlineGameActivity` y `LobbyActivity` (solo UI). `MainActivity` (Reto6) se mantiene para el modo contra Android.

**Tech Stack:** Kotlin, Android Views, Firebase Realtime Database 22.0.2 + Auth 24.2.0 (BoM 34.19.0), JUnit 4.

**Spec:** `Retos/Reto7/docs/superpowers/specs/2026-10-14-reto7-triqui-online-design.md` (secciones 4–10, Pruebas)

## Global Constraints

- Paquete `com.example.reto7`; todo el código en `Retos/Reto7/app/src/main/java/com/example/reto7/`, tests en `app/src/test/java/com/example/reto7/`.
- El host juega X (`TicTacToeGame.HUMAN_PLAYER`), el invitado O (`TicTacToeGame.COMPUTER_PLAYER`); `' '` = casilla libre.
- `winner` usa los códigos de `checkForWinner()`: 0 sigue, 1 empate, 2 ganó X, 3 ganó O.
- Estados: `"waiting"`, `"playing"`, `"abandoned"`; nodo `/games/{gameId}`.
- Textos de UI en inglés, en `values/strings.xml`.
- Estilo: prefijo `m` en propiedades privadas, comentarios breves como en Reto6, sin corrutinas (callbacks).
- No modificar `MainActivity.kt`, `BoardView.kt` ni `TicTacToeGame.kt`.
- Comandos Gradle desde `Retos/Reto7` con `./gradlew` (Git Bash).
- Commits terminan con `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

### Task 1: `OnlineGame` (estado + conversión a/desde mapa)

**Files:**
- Create: `app/src/main/java/com/example/reto7/OnlineGame.kt`
- Test: `app/src/test/java/com/example/reto7/OnlineGameTest.kt`

**Interfaces:**
- Produces:
  - `data class OnlineGame(hostUid: String, hostName: String, guestUid: String? = null, guestName: String? = null, status: String = STATUS_WAITING, board: String = EMPTY_BOARD, turn: Char = MARK_X, firstPlayer: Char = MARK_X, winner: Int = 0, scoreX: Int = 0, scoreO: Int = 0, ties: Int = 0, rematchX: Boolean = false, rematchO: Boolean = false, createdAt: Long = 0L)`
  - `fun markOf(uid: String): Char?`, `fun opponentName(myMark: Char): String`, `fun isRoundOver(): Boolean`, `fun rematchRequested(mark: Char): Boolean`, `fun toMap(): Map<String, Any?>`
  - `companion`: `MARK_X`, `MARK_O`, `STATUS_WAITING`, `STATUS_PLAYING`, `STATUS_ABANDONED`, `EMPTY_BOARD`, `fun fromMap(map: Map<*, *>?): OnlineGame?`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.reto7

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineGameTest {

    private val fullGame = OnlineGame(
        hostUid = "host", hostName = "Ana",
        guestUid = "guest", guestName = "Beto",
        status = OnlineGame.STATUS_PLAYING,
        board = "XO X  O  ",
        turn = OnlineGame.MARK_X, firstPlayer = OnlineGame.MARK_O,
        winner = 0, scoreX = 2, scoreO = 1, ties = 3,
        rematchX = true, rematchO = false, createdAt = 1234L
    )

    @Test
    fun toMap_thenFromMap_returnsTheSameGame() {
        assertEquals(fullGame, OnlineGame.fromMap(fullGame.toMap()))
    }

    @Test
    fun fromMap_acceptsLongNumbersAsFirebaseReturnsThem() {
        val map = mapOf(
            "hostUid" to "host", "hostName" to "Ana",
            "guestUid" to "guest", "guestName" to "Beto",
            "status" to "playing", "board" to "XO X  O  ",
            "turn" to "X", "firstPlayer" to "O", "winner" to 0L,
            "scores" to mapOf("x" to 2L, "o" to 1L, "ties" to 3L),
            "rematch" to mapOf("x" to true, "o" to false),
            "createdAt" to 1234L
        )
        assertEquals(fullGame, OnlineGame.fromMap(map))
    }

    @Test
    fun fromMap_usesDefaultsForMissingOptionalFields() {
        val map = mapOf(
            "hostUid" to "host", "hostName" to "Ana",
            "status" to "waiting", "board" to OnlineGame.EMPTY_BOARD
        )
        val game = OnlineGame.fromMap(map)!!
        assertNull(game.guestUid)
        assertNull(game.guestName)
        assertEquals(OnlineGame.MARK_X, game.turn)
        assertEquals(0, game.winner)
        assertEquals(0, game.scoreX)
        assertFalse(game.rematchO)
    }

    @Test
    fun fromMap_returnsNullForMissingOrInvalidData() {
        assertNull(OnlineGame.fromMap(null))
        val valid = fullGame.toMap()
        assertNull(OnlineGame.fromMap(valid - "hostUid"))
        assertNull(OnlineGame.fromMap(valid - "status"))
        assertNull(OnlineGame.fromMap(valid + ("board" to "XO")))
    }

    @Test
    fun emptyBoard_hasNineOpenSpots() {
        assertEquals(TicTacToeGame.BOARD_SIZE, OnlineGame.EMPTY_BOARD.length)
        assertTrue(OnlineGame.EMPTY_BOARD.all { it == TicTacToeGame.OPEN_SPOT })
    }

    @Test
    fun markOf_returnsXForHostOForGuestAndNullForStrangers() {
        assertEquals(OnlineGame.MARK_X, fullGame.markOf("host"))
        assertEquals(OnlineGame.MARK_O, fullGame.markOf("guest"))
        assertNull(fullGame.markOf("someone"))
    }

    @Test
    fun opponentName_returnsTheOtherPlayersName() {
        assertEquals("Beto", fullGame.opponentName(OnlineGame.MARK_X))
        assertEquals("Ana", fullGame.opponentName(OnlineGame.MARK_O))
        assertEquals("", fullGame.copy(guestName = null).opponentName(OnlineGame.MARK_X))
    }

    @Test
    fun rematchRequested_readsTheFlagOfTheGivenMark() {
        assertTrue(fullGame.rematchRequested(OnlineGame.MARK_X))
        assertFalse(fullGame.rematchRequested(OnlineGame.MARK_O))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.example.reto7.OnlineGameTest"`
Expected: FAIL — `Unresolved reference 'OnlineGame'`.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.example.reto7

/**
 * State of one online game, mirroring its /games/{gameId} node in the
 * Firebase Realtime Database. The host always plays X and the guest O;
 * the board is a 9-character string using TicTacToeGame's characters.
 */
data class OnlineGame(
    val hostUid: String,
    val hostName: String,
    val guestUid: String? = null,
    val guestName: String? = null,
    val status: String = STATUS_WAITING,
    val board: String = EMPTY_BOARD,
    val turn: Char = MARK_X,
    // Who started the current round; alternates on every rematch
    val firstPlayer: Char = MARK_X,
    // Same codes as TicTacToeGame.checkForWinner()
    val winner: Int = 0,
    val scoreX: Int = 0,
    val scoreO: Int = 0,
    val ties: Int = 0,
    val rematchX: Boolean = false,
    val rematchO: Boolean = false,
    val createdAt: Long = 0L
) {

    /** The mark played by the given user, or null if they aren't in this game. */
    fun markOf(uid: String): Char? = when (uid) {
        hostUid -> MARK_X
        guestUid -> MARK_O
        else -> null
    }

    /** The name of the player using the other mark ("" while nobody has joined). */
    fun opponentName(myMark: Char): String =
        (if (myMark == MARK_X) guestName else hostName) ?: ""

    fun isRoundOver(): Boolean = winner != 0

    fun rematchRequested(mark: Char): Boolean = if (mark == MARK_X) rematchX else rematchO

    /** The game as the map of values stored in the database. */
    fun toMap(): Map<String, Any?> = mapOf(
        "hostUid" to hostUid,
        "hostName" to hostName,
        "guestUid" to guestUid,
        "guestName" to guestName,
        "status" to status,
        "board" to board,
        "turn" to turn.toString(),
        "firstPlayer" to firstPlayer.toString(),
        "winner" to winner,
        "scores" to mapOf("x" to scoreX, "o" to scoreO, "ties" to ties),
        "rematch" to mapOf("x" to rematchX, "o" to rematchO),
        "createdAt" to createdAt
    )

    companion object {
        const val MARK_X = TicTacToeGame.HUMAN_PLAYER
        const val MARK_O = TicTacToeGame.COMPUTER_PLAYER

        const val STATUS_WAITING = "waiting"
        const val STATUS_PLAYING = "playing"
        const val STATUS_ABANDONED = "abandoned"

        val EMPTY_BOARD: String =
            TicTacToeGame.OPEN_SPOT.toString().repeat(TicTacToeGame.BOARD_SIZE)

        /**
         * Builds a game from the map read from the database (numbers come
         * back as Long). Returns null if a required field is missing or
         * the board is malformed.
         */
        fun fromMap(map: Map<*, *>?): OnlineGame? {
            if (map == null) return null
            val hostUid = map["hostUid"] as? String ?: return null
            val hostName = map["hostName"] as? String ?: return null
            val status = map["status"] as? String ?: return null
            val board = map["board"] as? String ?: return null
            if (board.length != TicTacToeGame.BOARD_SIZE) return null
            val scores = map["scores"] as? Map<*, *>
            val rematch = map["rematch"] as? Map<*, *>
            return OnlineGame(
                hostUid = hostUid,
                hostName = hostName,
                guestUid = map["guestUid"] as? String,
                guestName = map["guestName"] as? String,
                status = status,
                board = board,
                turn = markFrom(map["turn"]),
                firstPlayer = markFrom(map["firstPlayer"]),
                winner = intFrom(map["winner"]),
                scoreX = intFrom(scores?.get("x")),
                scoreO = intFrom(scores?.get("o")),
                ties = intFrom(scores?.get("ties")),
                rematchX = rematch?.get("x") as? Boolean ?: false,
                rematchO = rematch?.get("o") as? Boolean ?: false,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }

        private fun intFrom(value: Any?): Int = (value as? Number)?.toInt() ?: 0

        private fun markFrom(value: Any?): Char =
            if (value == MARK_O.toString()) MARK_O else MARK_X
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.example.reto7.OnlineGameTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add Retos/Reto7/app/src && git commit -m "Reto7: add OnlineGame state with database map conversion"
```

---

### Task 2: `OnlineGameRules` (transiciones puras)

**Files:**
- Create: `app/src/main/java/com/example/reto7/OnlineGameRules.kt`
- Test: `app/src/test/java/com/example/reto7/OnlineGameRulesTest.kt`

**Interfaces:**
- Consumes: `OnlineGame` (Task 1), `TicTacToeGame.setBoardState/setMove/checkForWinner/getBoardState`.
- Produces: `object OnlineGameRules` with `newGame(hostUid: String, hostName: String): OnlineGame`, `join(game: OnlineGame, uid: String, name: String): OnlineGame?`, `applyMove(game: OnlineGame, mark: Char, pos: Int): OnlineGame?`, `requestRematch(game: OnlineGame, mark: Char): OnlineGame?`, `other(mark: Char): Char`. `null` = operación inválida.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.reto7

import com.example.reto7.OnlineGame.Companion.MARK_O
import com.example.reto7.OnlineGame.Companion.MARK_X
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineGameRulesTest {

    private val waiting = OnlineGameRules.newGame("host", "Ana")
    private val playing = OnlineGameRules.join(waiting, "guest", "Beto")!!

    // Plays the given positions in order, each by whoever's turn it is
    private fun play(start: OnlineGame, vararg moves: Int): OnlineGame =
        moves.fold(start) { game, pos -> OnlineGameRules.applyMove(game, game.turn, pos)!! }

    @Test
    fun newGame_isWaitingWithAnEmptyBoardAndXToMove() {
        assertEquals(OnlineGame.STATUS_WAITING, waiting.status)
        assertEquals(OnlineGame.EMPTY_BOARD, waiting.board)
        assertEquals(MARK_X, waiting.turn)
        assertEquals(MARK_X, waiting.firstPlayer)
        assertEquals("host", waiting.hostUid)
        assertEquals("Ana", waiting.hostName)
        assertNull(waiting.guestUid)
    }

    @Test
    fun join_assignsTheGuestAndStartsPlaying() {
        assertEquals("guest", playing.guestUid)
        assertEquals("Beto", playing.guestName)
        assertEquals(OnlineGame.STATUS_PLAYING, playing.status)
    }

    @Test
    fun join_rejectsFullGamesNonWaitingGamesAndTheHost() {
        assertNull(OnlineGameRules.join(playing, "third", "Caro"))
        assertNull(OnlineGameRules.join(waiting.copy(status = OnlineGame.STATUS_ABANDONED), "guest", "Beto"))
        assertNull(OnlineGameRules.join(waiting, "host", "Ana"))
    }

    @Test
    fun applyMove_placesTheMarkAndPassesTheTurn() {
        val game = OnlineGameRules.applyMove(playing, MARK_X, 4)!!
        assertEquals('X', game.board[4])
        assertEquals(MARK_O, game.turn)
        assertEquals(0, game.winner)
    }

    @Test
    fun applyMove_rejectsInvalidMoves() {
        assertNull("out of turn", OnlineGameRules.applyMove(playing, MARK_O, 4))
        val afterX = play(playing, 4)
        assertNull("occupied", OnlineGameRules.applyMove(afterX, MARK_O, 4))
        assertNull("below range", OnlineGameRules.applyMove(playing, MARK_X, -1))
        assertNull("above range", OnlineGameRules.applyMove(playing, MARK_X, 9))
        assertNull("waiting", OnlineGameRules.applyMove(waiting, MARK_X, 0))
        val xWon = play(playing, 0, 3, 1, 4, 2)
        assertNull("round over", OnlineGameRules.applyMove(xWon, MARK_O, 8))
    }

    @Test
    fun applyMove_xWinning_setsWinnerAndCountsIt() {
        val game = play(playing, 0, 3, 1, 4, 2)
        assertEquals(2, game.winner)
        assertEquals(1, game.scoreX)
        assertEquals(0, game.scoreO)
        assertEquals(0, game.ties)
    }

    @Test
    fun applyMove_oWinning_setsWinnerAndCountsIt() {
        val game = play(playing, 0, 3, 1, 4, 8, 5)
        assertEquals(3, game.winner)
        assertEquals(0, game.scoreX)
        assertEquals(1, game.scoreO)
    }

    @Test
    fun applyMove_fullBoardWithoutLine_isATie() {
        val game = play(playing, 0, 1, 2, 4, 3, 5, 7, 6, 8)
        assertEquals(1, game.winner)
        assertEquals(1, game.ties)
        assertEquals(0, game.scoreX)
        assertEquals(0, game.scoreO)
    }

    @Test
    fun requestRematch_fromOnePlayer_onlyRecordsTheRequest() {
        val over = play(playing, 0, 3, 1, 4, 2)
        val game = OnlineGameRules.requestRematch(over, MARK_O)!!
        assertTrue(game.rematchO)
        assertFalse(game.rematchX)
        assertEquals(over.board, game.board)
        assertEquals(2, game.winner)
    }

    @Test
    fun requestRematch_fromBothPlayers_startsANewRoundWithTheOtherPlayerFirst() {
        val over = play(playing, 0, 3, 1, 4, 2)
        val oAsked = OnlineGameRules.requestRematch(over, MARK_O)!!
        val game = OnlineGameRules.requestRematch(oAsked, MARK_X)!!
        assertEquals(OnlineGame.EMPTY_BOARD, game.board)
        assertEquals(MARK_O, game.firstPlayer)
        assertEquals(MARK_O, game.turn)
        assertEquals(0, game.winner)
        assertEquals(1, game.scoreX)
        assertFalse(game.rematchX)
        assertFalse(game.rematchO)
    }

    @Test
    fun requestRematch_isRejectedWhileTheRoundIsInProgress() {
        assertNull(OnlineGameRules.requestRematch(play(playing, 4), MARK_X))
        assertNull(OnlineGameRules.requestRematch(waiting, MARK_X))
    }

    @Test
    fun other_swapsTheMarks() {
        assertEquals(MARK_O, OnlineGameRules.other(MARK_X))
        assertEquals(MARK_X, OnlineGameRules.other(MARK_O))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.example.reto7.OnlineGameRulesTest"`
Expected: FAIL — `Unresolved reference 'OnlineGameRules'`.

- [ ] **Step 3: Write the implementation**

```kotlin
package com.example.reto7

import com.example.reto7.OnlineGame.Companion.MARK_O
import com.example.reto7.OnlineGame.Companion.MARK_X

/**
 * The rules of an online game as pure functions: each one takes the
 * current state and returns the new one, or null if the operation isn't
 * allowed. GameRepository runs them inside database transactions.
 */
object OnlineGameRules {

    fun newGame(hostUid: String, hostName: String): OnlineGame = OnlineGame(hostUid, hostName)

    /** The guest takes the empty O seat of a waiting game. */
    fun join(game: OnlineGame, uid: String, name: String): OnlineGame? {
        if (game.status != OnlineGame.STATUS_WAITING || game.guestUid != null || uid == game.hostUid) {
            return null
        }
        return game.copy(guestUid = uid, guestName = name, status = OnlineGame.STATUS_PLAYING)
    }

    /** Places the mark if it's that player's turn and the spot is open. */
    fun applyMove(game: OnlineGame, mark: Char, pos: Int): OnlineGame? {
        if (game.status != OnlineGame.STATUS_PLAYING || game.isRoundOver() || game.turn != mark) {
            return null
        }
        val model = TicTacToeGame()
        model.setBoardState(game.board.toCharArray())
        if (!model.setMove(mark, pos)) return null

        val winner = model.checkForWinner()
        return game.copy(
            board = String(model.getBoardState()),
            turn = other(mark),
            winner = winner,
            ties = game.ties + if (winner == 1) 1 else 0,
            scoreX = game.scoreX + if (winner == 2) 1 else 0,
            scoreO = game.scoreO + if (winner == 3) 1 else 0
        )
    }

    /**
     * Records that the player wants a rematch. Once both do, the board is
     * cleared and the player who didn't start the last round goes first.
     */
    fun requestRematch(game: OnlineGame, mark: Char): OnlineGame? {
        if (game.status != OnlineGame.STATUS_PLAYING || !game.isRoundOver()) return null
        val requested = if (mark == MARK_X) game.copy(rematchX = true) else game.copy(rematchO = true)
        if (!(requested.rematchX && requested.rematchO)) return requested

        val first = other(game.firstPlayer)
        return requested.copy(
            board = OnlineGame.EMPTY_BOARD,
            firstPlayer = first,
            turn = first,
            winner = 0,
            rematchX = false,
            rematchO = false
        )
    }

    fun other(mark: Char): Char = if (mark == MARK_X) MARK_O else MARK_X
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest`
Expected: PASS (todos: `TicTacToeGameTest`, `OnlineGameTest`, `OnlineGameRulesTest`).

- [ ] **Step 5: Commit**

```bash
git add Retos/Reto7/app/src && git commit -m "Reto7: add OnlineGameRules for joining, moves and rematches"
```

---

### Task 3: `GameRepository` + utilidades de UI

**Files:**
- Create: `app/src/main/java/com/example/reto7/GameRepository.kt`
- Create: `app/src/main/java/com/example/reto7/UiHelpers.kt`

**Interfaces:**
- Consumes: `OnlineGame`, `OnlineGameRules` (Tasks 1–2).
- Produces:
  - `class GameRepository` con `class Subscription { fun cancel() }`, `observeWaitingGames(onChange: (List<Pair<String, OnlineGame>>) -> Unit): Subscription`, `observeGame(gameId: String, onChange: (OnlineGame?) -> Unit): Subscription`, `createGame(uid: String, name: String, onResult: (String?) -> Unit)`, `joinGame(gameId: String, uid: String, name: String, onResult: (Boolean) -> Unit)`, `makeMove(gameId: String, mark: Char, pos: Int, onResult: (Boolean) -> Unit)`, `requestRematch(gameId: String, mark: Char)`, `armDisconnectForWaiting(gameId: String)`, `armDisconnectForPlaying(gameId: String)`, `leaveGame(gameId: String, status: String?)`.
  - `UiHelpers.kt`: `applySystemBarPadding(root: View)`, `showMenuPopup(anchor: View, menuRes: Int, onItemClick: (Int) -> Boolean)`, `showAboutDialog(activity: Activity)`.

- [ ] **Step 1: Escribir `GameRepository.kt`**

```kotlin
package com.example.reto7

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

/**
 * The only class that talks to the Firebase Realtime Database. Every
 * change to a game runs one of the OnlineGameRules inside a transaction,
 * so two devices can't both join a game or move at the same time.
 * Callbacks run on the main thread.
 */
class GameRepository {

    /** A registered database listener; cancel() stops it. */
    class Subscription(private val query: Query, private val listener: ValueEventListener) {
        fun cancel() = query.removeEventListener(listener)
    }

    private val mGames: DatabaseReference = FirebaseDatabase.getInstance().reference.child(GAMES)

    /** Calls onChange with every game waiting for an opponent, newest first. */
    fun observeWaitingGames(onChange: (List<Pair<String, OnlineGame>>) -> Unit): Subscription {
        val query = mGames.orderByChild(STATUS).equalTo(OnlineGame.STATUS_WAITING)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val games = snapshot.children.mapNotNull { child ->
                    val key = child.key
                    val game = OnlineGame.fromMap(child.value as? Map<*, *>)
                    if (key != null && game != null) key to game else null
                }
                onChange(games.sortedByDescending { it.second.createdAt })
            }

            override fun onCancelled(error: DatabaseError) = onChange(emptyList())
        }
        query.addValueEventListener(listener)
        return Subscription(query, listener)
    }

    /** Calls onChange with the game's state on every change (null once it's deleted). */
    fun observeGame(gameId: String, onChange: (OnlineGame?) -> Unit): Subscription {
        val ref = mGames.child(gameId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) =
                onChange(OnlineGame.fromMap(snapshot.value as? Map<*, *>))

            override fun onCancelled(error: DatabaseError) = onChange(null)
        }
        ref.addValueEventListener(listener)
        return Subscription(ref, listener)
    }

    /** Creates a game waiting for an opponent; onResult gets its id, or null on failure. */
    fun createGame(uid: String, name: String, onResult: (String?) -> Unit) {
        val ref = mGames.push()
        val data = OnlineGameRules.newGame(uid, name).toMap() + (CREATED_AT to ServerValue.TIMESTAMP)
        ref.setValue(data).addOnCompleteListener { task ->
            onResult(if (task.isSuccessful) ref.key else null)
        }
    }

    fun joinGame(gameId: String, uid: String, name: String, onResult: (Boolean) -> Unit) =
        update(gameId, { OnlineGameRules.join(it, uid, name) }) { onResult(it?.guestUid == uid) }

    fun makeMove(gameId: String, mark: Char, pos: Int, onResult: (Boolean) -> Unit) =
        update(gameId, { OnlineGameRules.applyMove(it, mark, pos) }) { onResult(it != null) }

    fun requestRematch(gameId: String, mark: Char) =
        update(gameId, { OnlineGameRules.requestRematch(it, mark) }) {}

    /** While waiting for an opponent, delete the game if this device disconnects. */
    fun armDisconnectForWaiting(gameId: String) {
        mGames.child(gameId).onDisconnect().removeValue()
    }

    /** During a match, mark the game abandoned if this device disconnects. */
    fun armDisconnectForPlaying(gameId: String) {
        val ref = mGames.child(gameId)
        ref.onDisconnect().cancel()
        ref.child(STATUS).onDisconnect().setValue(OnlineGame.STATUS_ABANDONED)
    }

    /**
     * Leaves the game on purpose: a game in progress is marked abandoned so
     * the opponent finds out; otherwise (waiting, or already abandoned) the
     * game is deleted.
     */
    fun leaveGame(gameId: String, status: String?) {
        val ref = mGames.child(gameId)
        ref.onDisconnect().cancel()
        if (status == OnlineGame.STATUS_PLAYING) {
            ref.child(STATUS).setValue(OnlineGame.STATUS_ABANDONED)
        } else {
            ref.removeValue()
        }
    }

    // Applies the rule to the game in a transaction; onResult gets the
    // committed state, or null if the rule rejected it or it failed.
    private fun update(
        gameId: String,
        rule: (OnlineGame) -> OnlineGame?,
        onResult: (OnlineGame?) -> Unit
    ) {
        mGames.child(gameId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                // Before the first server round trip the local value may be
                // missing; returning it unchanged makes Firebase retry with
                // the server's value.
                val game = OnlineGame.fromMap(currentData.value as? Map<*, *>)
                    ?: return Transaction.success(currentData)
                val updated = rule(game) ?: return Transaction.abort()
                currentData.value = updated.toMap()
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                val game = if (error == null && committed) {
                    OnlineGame.fromMap(snapshot?.value as? Map<*, *>)
                } else {
                    null
                }
                onResult(game)
            }
        })
    }

    companion object {
        private const val GAMES = "games"
        private const val STATUS = "status"
        private const val CREATED_AT = "createdAt"
    }
}
```

- [ ] **Step 2: Escribir `UiHelpers.kt`**

```kotlin
package com.example.reto7

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.view.View
import android.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * There's no title bar, so keep the content clear of the status and
 * navigation bars (and any display cutout in landscape).
 */
fun applySystemBarPadding(root: View) {
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}

/** Shows the given menu anchored to the corner menu button. */
fun showMenuPopup(anchor: View, menuRes: Int, onItemClick: (Int) -> Boolean) {
    val popup = PopupMenu(anchor.context, anchor)
    popup.menuInflater.inflate(menuRes, popup.menu)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        popup.setForceShowIcon(true)
    }
    popup.setOnMenuItemClickListener { onItemClick(it.itemId) }
    popup.show()
}

/** Shows the About dialog (about_dialog.xml). */
fun showAboutDialog(activity: Activity) {
    AlertDialog.Builder(activity)
        .setView(activity.layoutInflater.inflate(R.layout.about_dialog, null))
        .setPositiveButton(R.string.about_ok, null)
        .show()
}
```

- [ ] **Step 3: Verificar que compila y los tests siguen pasando**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add Retos/Reto7/app/src && git commit -m "Reto7: add GameRepository over Firebase and shared UI helpers"
```

---

### Task 4: `OnlineGameActivity` (partida online)

**Files:**
- Create: `app/src/main/java/com/example/reto7/OnlineGameActivity.kt`
- Create: `app/src/main/res/layout/activity_online_game.xml`, `app/src/main/res/layout-land/activity_online_game.xml`, `app/src/main/res/menu/online_menu.xml`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `GameRepository`, `OnlineGame`, `OnlineGameRules.other`, `applySystemBarPadding`, `showMenuPopup`, `showAboutDialog`, `BoardView`, `TicTacToeGame`.
- Produces: `OnlineGameActivity.EXTRA_GAME_ID: String` (extra String) y `OnlineGameActivity.EXTRA_MY_MARK: String` (extra Char).

- [ ] **Step 1: Strings** — agregar antes de `</resources>` en `values/strings.xml`:

```xml
    <string name="online_waiting_opponent">Waiting for an opponent…</string>
    <string name="online_your_turn">Your turn (%1$s)</string>
    <string name="online_opponent_turn">%1$s\'s turn</string>
    <string name="online_opponent_won">%1$s won!</string>
    <string name="online_rematch">Rematch</string>
    <string name="online_rematch_waiting">Waiting for %1$s…</string>
    <string name="online_rematch_offered">%1$s wants a rematch!</string>
    <string name="online_opponent_left">Your opponent left the game.</string>
    <string name="online_opponent">Opponent</string>
    <string name="score_you">You: %1$d</string>
    <string name="score_opponent">%1$s: %2$d</string>
    <string name="menu_leave_game">Leave game</string>
```

- [ ] **Step 2: Menú `res/menu/online_menu.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/leave_game"
        android:title="@string/menu_leave_game" />
    <item
        android:id="@+id/about"
        android:title="@string/menu_about"
        android:icon="@drawable/ic_about" />
</menu>
```

- [ ] **Step 3: Layout vertical `res/layout/activity_online_game.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:orientation="vertical"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center_horizontal">

        <com.example.reto7.BoardView
            android:id="@+id/board"
            android:layout_width="300dp"
            android:layout_height="300dp"
            android:layout_marginTop="48dp" />

        <TextView
            android:id="@+id/information"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/online_waiting_opponent"
            android:gravity="center_horizontal"
            android:layout_marginTop="20dp"
            android:textSize="20sp" />

        <RelativeLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="20dp">

            <TextView
                android:id="@+id/ties_score"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_centerHorizontal="true"
                android:textSize="18sp"
                android:text="@string/score_ties_default" />

            <TextView
                android:id="@+id/my_score"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_toStartOf="@id/ties_score"
                android:layout_alignBaseline="@id/ties_score"
                android:layout_marginEnd="24dp"
                android:textSize="18sp" />

            <TextView
                android:id="@+id/opponent_score"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_toEndOf="@id/ties_score"
                android:layout_alignBaseline="@id/ties_score"
                android:layout_marginStart="24dp"
                android:textSize="18sp" />
        </RelativeLayout>

        <Button
            android:id="@+id/rematch_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="@string/online_rematch"
            android:visibility="gone" />

    </LinearLayout>

    <!-- The app has no title bar, so this button opens the options menu. -->
    <ImageButton
        android:id="@+id/menu_button"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="top|end"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/menu_more"
        android:src="@drawable/ic_more_vert" />

</FrameLayout>
```

- [ ] **Step 4: Layout horizontal `res/layout-land/activity_online_game.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:orientation="horizontal"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center_vertical">

        <!-- Smaller board aligned on the left side of the screen -->
        <com.example.reto7.BoardView
            android:id="@+id/board"
            android:layout_width="270dp"
            android:layout_height="270dp"
            android:layout_marginStart="5dp" />

        <!-- Information, scores and rematch button to the right of the board -->
        <LinearLayout
            android:orientation="vertical"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="24dp"
            android:layout_marginEnd="56dp">

            <TextView
                android:id="@+id/information"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="@string/online_waiting_opponent"
                android:gravity="center_horizontal"
                android:textSize="20sp" />

            <TextView
                android:id="@+id/my_score"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="24dp"
                android:textSize="18sp" />

            <TextView
                android:id="@+id/ties_score"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:textSize="18sp"
                android:text="@string/score_ties_default" />

            <TextView
                android:id="@+id/opponent_score"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:textSize="18sp" />

            <Button
                android:id="@+id/rematch_button"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:text="@string/online_rematch"
                android:visibility="gone" />
        </LinearLayout>

    </LinearLayout>

    <!-- The app has no title bar, so this button opens the options menu. -->
    <ImageButton
        android:id="@+id/menu_button"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="top|end"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/menu_more"
        android:src="@drawable/ic_more_vert" />

</FrameLayout>
```

- [ ] **Step 5: `OnlineGameActivity.kt`**

```kotlin
package com.example.reto7

import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

/**
 * A game against another player on their own device. All of the game's
 * state lives in the database: this screen draws every change it
 * receives and sends the player's moves through GameRepository.
 */
class OnlineGameActivity : AppCompatActivity() {

    // Only used so BoardView can draw the board received from the database
    private val mGame = TicTacToeGame()
    private lateinit var mRepository: GameRepository

    private lateinit var mBoardView: BoardView
    private lateinit var mInfoTextView: TextView
    private lateinit var mMyScoreView: TextView
    private lateinit var mTiesScoreView: TextView
    private lateinit var mOpponentScoreView: TextView
    private lateinit var mRematchButton: Button

    private lateinit var mGameId: String
    private var mMyMark = OnlineGame.MARK_X

    // Latest state received from the database
    private var mState: OnlineGame? = null
    private var mSubscription: GameRepository.Subscription? = null

    // True while our move's transaction is running; blocks extra touches
    private var mMovePending = false
    // The status the onDisconnect handler was last armed for
    private var mArmedStatus: String? = null
    // True once we've left the game, so its later changes are ignored
    private var mLeft = false
    private var mOpponentLeftDialog: AlertDialog? = null

    // Sound effects; only alive between onResume() and onPause().
    private var mMyMediaPlayer: MediaPlayer? = null
    private var mOpponentMediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_game)
        applySystemBarPadding(findViewById(R.id.root))

        val gameId = intent.getStringExtra(EXTRA_GAME_ID)
        if (gameId == null) {
            finish()
            return
        }
        mGameId = gameId
        mMyMark = intent.getCharExtra(EXTRA_MY_MARK, OnlineGame.MARK_X)

        mInfoTextView = findViewById(R.id.information)
        mMyScoreView = findViewById(R.id.my_score)
        mTiesScoreView = findViewById(R.id.ties_score)
        mOpponentScoreView = findViewById(R.id.opponent_score)
        mRematchButton = findViewById(R.id.rematch_button)
        mBoardView = findViewById(R.id.board)
        mBoardView.setGame(mGame)
        mBoardView.setOnTouchListener(mTouchListener)

        mRepository = GameRepository()
        mRematchButton.setOnClickListener { mRepository.requestRematch(mGameId, mMyMark) }
        findViewById<ImageButton>(R.id.menu_button).setOnClickListener { anchor ->
            showMenuPopup(anchor, R.menu.online_menu) { itemId ->
                when (itemId) {
                    R.id.leave_game -> { leaveGame(); true }
                    R.id.about -> { showAboutDialog(this); true }
                    else -> false
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = leaveGame()
        })

        // The state comes from the database, so this also restores the
        // screen after an orientation change.
        mSubscription = mRepository.observeGame(mGameId) { onGameChanged(it) }
    }

    override fun onResume() {
        super.onResume()
        mMyMediaPlayer = MediaPlayer.create(applicationContext, R.raw.human_move)
        mOpponentMediaPlayer = MediaPlayer.create(applicationContext, R.raw.computer_move)
    }

    override fun onPause() {
        super.onPause()
        mMyMediaPlayer?.release()
        mOpponentMediaPlayer?.release()
        mMyMediaPlayer = null
        mOpponentMediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mSubscription?.cancel()
        mOpponentLeftDialog?.dismiss()
        // Closing the screen leaves the game; an orientation change doesn't.
        if (isFinishing && !mLeft && ::mGameId.isInitialized) {
            mLeft = true
            mRepository.leaveGame(mGameId, mState?.status)
        }
    }

    private fun leaveGame() {
        if (!mLeft) {
            mLeft = true
            mRepository.leaveGame(mGameId, mState?.status)
        }
        finish()
    }

    private fun onGameChanged(game: OnlineGame?) {
        if (mLeft) return
        val previous = mState
        mState = game
        if (game == null || game.status == OnlineGame.STATUS_ABANDONED) {
            showOpponentLeft()
            return
        }

        armDisconnect(game.status)
        playMoveSound(previous, game)

        mGame.setBoardState(game.board.toCharArray())
        mBoardView.invalidate() // Redraw the board
        mInfoTextView.text = infoText(game)
        displayScores(game)
        mRematchButton.visibility =
            if (game.isRoundOver() && !game.rematchRequested(mMyMark)) View.VISIBLE else View.GONE
    }

    // Makes the database clean up after us if this device disconnects:
    // a waiting game is deleted, a game in progress is marked abandoned.
    private fun armDisconnect(status: String) {
        if (status == mArmedStatus) return
        mArmedStatus = status
        if (status == OnlineGame.STATUS_WAITING && mMyMark == OnlineGame.MARK_X) {
            mRepository.armDisconnectForWaiting(mGameId)
        } else if (status == OnlineGame.STATUS_PLAYING) {
            mRepository.armDisconnectForPlaying(mGameId)
        }
    }

    // Plays the sound of the mark that appeared since the previous state
    private fun playMoveSound(previous: OnlineGame?, game: OnlineGame) {
        if (previous == null) return
        for (i in 0 until TicTacToeGame.BOARD_SIZE) {
            if (previous.board[i] == TicTacToeGame.OPEN_SPOT && game.board[i] != TicTacToeGame.OPEN_SPOT) {
                if (game.board[i] == mMyMark) mMyMediaPlayer?.start() else mOpponentMediaPlayer?.start()
                return
            }
        }
    }

    private fun opponentName(game: OnlineGame): String =
        game.opponentName(mMyMark).ifEmpty { getString(R.string.online_opponent) }

    private fun infoText(game: OnlineGame): String {
        if (game.status == OnlineGame.STATUS_WAITING) {
            return getString(R.string.online_waiting_opponent)
        }
        val opponent = opponentName(game)
        if (!game.isRoundOver()) {
            return if (game.turn == mMyMark) {
                getString(R.string.online_your_turn, mMyMark.toString())
            } else {
                getString(R.string.online_opponent_turn, opponent)
            }
        }

        val myWin = if (mMyMark == OnlineGame.MARK_X) 2 else 3
        val result = when (game.winner) {
            1 -> getString(R.string.result_tie)
            myWin -> getString(R.string.result_human_wins)
            else -> getString(R.string.online_opponent_won, opponent)
        }
        val rematch = when {
            game.rematchRequested(mMyMark) -> getString(R.string.online_rematch_waiting, opponent)
            game.rematchRequested(OnlineGameRules.other(mMyMark)) ->
                getString(R.string.online_rematch_offered, opponent)
            else -> null
        }
        return if (rematch == null) result else "$result\n$rematch"
    }

    private fun displayScores(game: OnlineGame) {
        val mine = if (mMyMark == OnlineGame.MARK_X) game.scoreX else game.scoreO
        val theirs = if (mMyMark == OnlineGame.MARK_X) game.scoreO else game.scoreX
        mMyScoreView.text = getString(R.string.score_you, mine)
        mTiesScoreView.text = getString(R.string.score_ties, game.ties)
        mOpponentScoreView.text = getString(R.string.score_opponent, opponentName(game), theirs)
    }

    private fun showOpponentLeft() {
        if (mOpponentLeftDialog != null) return
        mOpponentLeftDialog = AlertDialog.Builder(this)
            .setMessage(R.string.online_opponent_left)
            .setCancelable(false)
            .setPositiveButton(R.string.about_ok) { _, _ -> leaveGame() }
            .show()
    }

    // Listen for touches on the board
    private val mTouchListener = View.OnTouchListener { v, event ->
        v.performClick()

        // Determine which cell was touched (clamped so touching the very
        // edge of the view can't produce an out-of-range column/row)
        val col = (event.x.toInt() / mBoardView.getBoardCellWidth()).coerceIn(0, 2)
        val row = (event.y.toInt() / mBoardView.getBoardCellHeight()).coerceIn(0, 2)
        val pos = row * 3 + col

        val game = mState
        if (game != null && !mMovePending && game.status == OnlineGame.STATUS_PLAYING &&
            !game.isRoundOver() && game.turn == mMyMark &&
            game.board[pos] == TicTacToeGame.OPEN_SPOT
        ) {
            mMovePending = true
            mRepository.makeMove(mGameId, mMyMark, pos) { mMovePending = false }
        }

        // So we aren't notified of continued events when finger is moved
        false
    }

    companion object {
        const val EXTRA_GAME_ID = "gameId"
        const val EXTRA_MY_MARK = "myMark"
    }
}
```

- [ ] **Step 6: Registrar la Activity** — en `AndroidManifest.xml`, dentro de `<application>` después de `.MainActivity`:

```xml
        <activity
            android:name=".OnlineGameActivity"
            android:exported="false" />
```

- [ ] **Step 7: Verificar**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add Retos/Reto7/app/src && git commit -m "Reto7: add OnlineGameActivity for playing against a remote opponent"
```

---

### Task 5: `LobbyActivity` (pantalla inicial)

**Files:**
- Create: `app/src/main/java/com/example/reto7/LobbyActivity.kt`
- Create: `app/src/main/res/layout/activity_lobby.xml`, `app/src/main/res/layout/dialog_name.xml`, `app/src/main/res/menu/lobby_menu.xml`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `GameRepository`, `OnlineGame`, `OnlineGameActivity.EXTRA_GAME_ID/EXTRA_MY_MARK`, `MainActivity`, `applySystemBarPadding`, `showMenuPopup`, `showAboutDialog`, `FirebaseAuth`.

- [ ] **Step 1: Strings** — agregar antes de `</resources>` y reemplazar `about_description`:

```xml
    <string name="lobby_title">Tic-Tac-Toe Online</string>
    <string name="lobby_playing_as">Playing as %1$s</string>
    <string name="lobby_create_game">Create game</string>
    <string name="lobby_play_android">Play vs Android</string>
    <string name="lobby_connecting">Connecting…</string>
    <string name="lobby_connect_failed">Couldn\'t connect. Check your internet connection.</string>
    <string name="lobby_retry">Retry</string>
    <string name="lobby_available_games">Available games</string>
    <string name="lobby_no_games">No games available. Create one!</string>
    <string name="lobby_game_item">%1$s\'s game</string>
    <string name="lobby_game_unavailable">That game is no longer available</string>
    <string name="lobby_create_failed">Couldn\'t create the game. Try again.</string>
    <string name="menu_change_name">Change name</string>
    <string name="name_title">What\'s your name?</string>
    <string name="name_hint">Your name</string>
    <string name="name_empty">Please enter a name</string>
```

```xml
    <string name="about_description">Play online against a friend, or against Android at one of three difficulty levels. Don\'t let the Android beat you!</string>
```

- [ ] **Step 2: Menú `res/menu/lobby_menu.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/change_name"
        android:title="@string/menu_change_name" />
    <item
        android:id="@+id/about"
        android:title="@string/menu_about"
        android:icon="@drawable/ic_about" />
</menu>
```

- [ ] **Step 3: `res/layout/dialog_name.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingStart="24dp"
    android:paddingTop="8dp"
    android:paddingEnd="24dp">

    <EditText
        android:id="@+id/name_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/name_hint"
        android:importantForAutofill="no"
        android:inputType="textPersonName|textCapWords"
        android:maxLength="20"
        android:maxLines="1" />

</FrameLayout>
```

- [ ] **Step 4: `res/layout/activity_lobby.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:orientation="vertical"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="16dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:gravity="center_horizontal"
            android:text="@string/lobby_title"
            android:textSize="24sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/player_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:gravity="center_horizontal"
            android:textSize="16sp" />

        <Button
            android:id="@+id/create_game_button"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:enabled="false"
            android:text="@string/lobby_create_game" />

        <Button
            android:id="@+id/play_android_button"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/lobby_play_android" />

        <TextView
            android:id="@+id/lobby_status"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="@string/lobby_connecting"
            android:textSize="18sp"
            android:textStyle="bold" />

        <Button
            android:id="@+id/retry_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/lobby_retry"
            android:visibility="gone" />

        <ListView
            android:id="@+id/games_list"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />

        <TextView
            android:id="@+id/empty_games"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_horizontal"
            android:padding="16dp"
            android:text="@string/lobby_no_games"
            android:textSize="16sp" />

    </LinearLayout>

    <!-- The app has no title bar, so this button opens the options menu. -->
    <ImageButton
        android:id="@+id/menu_button"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="top|end"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/menu_more"
        android:src="@drawable/ic_more_vert" />

</FrameLayout>
```

- [ ] **Step 5: `LobbyActivity.kt`**

```kotlin
package com.example.reto7

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import com.google.firebase.auth.FirebaseAuth

/**
 * The app's first screen: lists the online games waiting for an
 * opponent, and lets the player join one, create one, or play against
 * Android instead. Signs in anonymously so the database knows who's who.
 */
class LobbyActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mRepository: GameRepository
    private lateinit var mPrefs: SharedPreferences

    private lateinit var mPlayerNameView: TextView
    private lateinit var mStatusView: TextView
    private lateinit var mCreateButton: Button
    private lateinit var mRetryButton: Button
    private lateinit var mAdapter: ArrayAdapter<String>

    // The games shown in the list, as (gameId, game) pairs
    private var mGames: List<Pair<String, OnlineGame>> = emptyList()
    private var mGamesSubscription: GameRepository.Subscription? = null
    // True while creating or joining a game; ignores extra taps
    private var mBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)
        applySystemBarPadding(findViewById(R.id.root))

        mPlayerNameView = findViewById(R.id.player_name)
        mStatusView = findViewById(R.id.lobby_status)
        mCreateButton = findViewById(R.id.create_game_button)
        mRetryButton = findViewById(R.id.retry_button)

        mAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        val gamesList = findViewById<ListView>(R.id.games_list)
        gamesList.adapter = mAdapter
        gamesList.emptyView = findViewById(R.id.empty_games)
        gamesList.setOnItemClickListener { _, _, position, _ -> joinGame(mGames[position].first) }

        mCreateButton.setOnClickListener { createGame() }
        mRetryButton.setOnClickListener { signIn() }
        findViewById<Button>(R.id.play_android_button).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<ImageButton>(R.id.menu_button).setOnClickListener { anchor ->
            showMenuPopup(anchor, R.menu.lobby_menu) { itemId ->
                when (itemId) {
                    R.id.change_name -> { showNameDialog(); true }
                    R.id.about -> { showAboutDialog(this); true }
                    else -> false
                }
            }
        }

        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()
        mRepository = GameRepository()

        displayPlayerName()
        if (playerName().isEmpty()) showNameDialog()
        signIn()
    }

    override fun onStart() {
        super.onStart()
        if (mAuth.currentUser != null) startWatchingGames()
    }

    override fun onStop() {
        super.onStop()
        mGamesSubscription?.cancel()
        mGamesSubscription = null
    }

    // Signs in anonymously (Firebase remembers the user between launches)
    private fun signIn() {
        if (mAuth.currentUser != null) {
            onSignedIn()
            return
        }
        mStatusView.setText(R.string.lobby_connecting)
        mRetryButton.visibility = View.GONE
        mAuth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                onSignedIn()
            } else {
                mStatusView.setText(R.string.lobby_connect_failed)
                mRetryButton.visibility = View.VISIBLE
            }
        }
    }

    private fun onSignedIn() {
        mStatusView.setText(R.string.lobby_available_games)
        mRetryButton.visibility = View.GONE
        mCreateButton.isEnabled = true
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startWatchingGames()
    }

    private fun startWatchingGames() {
        if (mGamesSubscription != null) return
        mGamesSubscription = mRepository.observeWaitingGames { games ->
            // Our own waiting games aren't joinable
            val uid = mAuth.currentUser?.uid
            mGames = games.filter { it.second.hostUid != uid }
            mAdapter.clear()
            mAdapter.addAll(mGames.map { getString(R.string.lobby_game_item, it.second.hostName) })
        }
    }

    private fun createGame() {
        val uid = mAuth.currentUser?.uid ?: return
        if (mBusy || !ensurePlayerName()) return
        mBusy = true
        mRepository.createGame(uid, playerName()) { gameId ->
            mBusy = false
            if (gameId != null) {
                openGame(gameId, OnlineGame.MARK_X)
            } else {
                Toast.makeText(this, R.string.lobby_create_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun joinGame(gameId: String) {
        val uid = mAuth.currentUser?.uid ?: return
        if (mBusy || !ensurePlayerName()) return
        mBusy = true
        mRepository.joinGame(gameId, uid, playerName()) { joined ->
            mBusy = false
            if (joined) {
                openGame(gameId, OnlineGame.MARK_O)
            } else {
                Toast.makeText(this, R.string.lobby_game_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGame(gameId: String, myMark: Char) {
        val intent = Intent(this, OnlineGameActivity::class.java)
            .putExtra(OnlineGameActivity.EXTRA_GAME_ID, gameId)
            .putExtra(OnlineGameActivity.EXTRA_MY_MARK, myMark)
        startActivity(intent)
    }

    private fun playerName(): String = mPrefs.getString(KEY_PLAYER_NAME, "") ?: ""

    // Asks for a name first if the player hasn't chosen one yet
    private fun ensurePlayerName(): Boolean {
        if (playerName().isNotEmpty()) return true
        showNameDialog()
        return false
    }

    private fun displayPlayerName() {
        val name = playerName()
        mPlayerNameView.text = if (name.isEmpty()) "" else getString(R.string.lobby_playing_as, name)
    }

    private fun showNameDialog() {
        val layout = layoutInflater.inflate(R.layout.dialog_name, null)
        val input = layout.findViewById<EditText>(R.id.name_input)
        input.setText(playerName())

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.name_title)
            .setView(layout)
            .setPositiveButton(R.string.about_ok, null)
            .setCancelable(playerName().isNotEmpty())
            .create()
        // Replace the OK listener so an empty name keeps the dialog open
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    input.error = getString(R.string.name_empty)
                } else {
                    mPrefs.edit { putString(KEY_PLAYER_NAME, name) }
                    displayPlayerName()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    companion object {
        private const val PREFS_NAME = "ttt_prefs"
        private const val KEY_PLAYER_NAME = "playerName"
    }
}
```

- [ ] **Step 6: Manifest** — `LobbyActivity` pasa a ser el launcher y `MainActivity` deja de exportarse. Reemplazar el bloque de `.MainActivity` por:

```xml
        <activity
            android:name=".LobbyActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity
            android:name=".MainActivity"
            android:exported="false" />
```

- [ ] **Step 7: Verificar**

Run: `./gradlew assembleDebug testDebugUnitTest lintDebug`
Expected: `BUILD SUCCESSFUL`; sin errores de lint nuevos.

- [ ] **Step 8: Commit**

```bash
git add Retos/Reto7/app/src && git commit -m "Reto7: add LobbyActivity as the launcher with game list and name prompt"
```

---

### Task 6: Verificación manual con dos dispositivos

**Files:** ninguno (solo correcciones si algo falla).

- [ ] **Step 1:** Instalar en dos emuladores/dispositivos: `./gradlew installDebug` (con ambos conectados, o `adb -s <serial> install app/build/outputs/apk/debug/app-debug.apk`).
- [ ] **Step 2:** Recorrer la checklist manual del spec (sección "Pruebas → Manual", puntos 1–12). Cualquier fallo se corrige con su propio commit.
- [ ] **Step 3:** En la consola de Firebase (Realtime Database → Datos), confirmar que al terminar no quedan nodos huérfanos en `/games`.
