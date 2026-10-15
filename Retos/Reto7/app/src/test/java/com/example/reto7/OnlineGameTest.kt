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
