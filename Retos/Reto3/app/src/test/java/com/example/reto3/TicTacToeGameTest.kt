package com.example.reto3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TicTacToeGameTest {

    private lateinit var game: TicTacToeGame

    @Before
    fun setUp() {
        game = TicTacToeGame()
    }

    @Test
    fun clearBoard_setsEveryCellToOpenSpot() {
        for (i in 0 until TicTacToeGame.BOARD_SIZE) {
            assertEquals(TicTacToeGame.OPEN_SPOT, game.boardCell(i))
        }
    }

    @Test
    fun setMove_placesPlayerAtOpenLocation() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 4)
        assertEquals(TicTacToeGame.HUMAN_PLAYER, game.boardCell(4))
    }

    @Test
    fun setMove_doesNotOverwriteOccupiedLocation() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 0)
        assertEquals(TicTacToeGame.HUMAN_PLAYER, game.boardCell(0))
    }

    @Test
    fun checkForWinner_returnsZero_whenGameInProgress() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 1)
        assertEquals(0, game.checkForWinner())
    }

    @Test
    fun checkForWinner_returnsTwo_whenHumanCompletesARow() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 1)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 2)
        assertEquals(2, game.checkForWinner())
    }

    @Test
    fun checkForWinner_returnsThree_whenComputerCompletesAColumn() {
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 0)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 3)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 6)
        assertEquals(3, game.checkForWinner())
    }

    @Test
    fun checkForWinner_returnsOne_whenBoardIsFullWithNoWinner() {
        // X | O | X
        // X | O | O
        // O | X | X
        val xMoves = intArrayOf(0, 2, 3, 7, 8)
        val oMoves = intArrayOf(1, 4, 5, 6)
        for (loc in xMoves) game.setMove(TicTacToeGame.HUMAN_PLAYER, loc)
        for (loc in oMoves) game.setMove(TicTacToeGame.COMPUTER_PLAYER, loc)
        assertEquals(1, game.checkForWinner())
    }

    @Test
    fun getComputerMove_takesTheWinWhenOneIsAvailable() {
        // O has two in a row (0,1); should take 2 to win.
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 0)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 1)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 3)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 4)
        assertEquals(2, game.getComputerMove())
    }

    @Test
    fun getComputerMove_blocksTheHumanWhenAboutToWin() {
        // X has two in a row (0,1) and would win at 2; O must block.
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 1)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 6)
        assertEquals(2, game.getComputerMove())
    }

    @Test
    fun getComputerMove_returnsAnOpenLocation_whenNoWinOrBlockAvailable() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        val move = game.getComputerMove()
        assertTrue(move in 0 until TicTacToeGame.BOARD_SIZE)
        assertEquals(TicTacToeGame.OPEN_SPOT, game.boardCell(move))
    }
}
