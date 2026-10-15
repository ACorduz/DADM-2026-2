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
