package com.example.reto7

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            assertEquals(TicTacToeGame.OPEN_SPOT, game.getBoardOccupant(i))
        }
    }

    @Test
    fun clearBoard_resetsOccupiedCellsBackToOpenSpot() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 4)
        game.clearBoard()
        for (i in 0 until TicTacToeGame.BOARD_SIZE) {
            assertEquals(TicTacToeGame.OPEN_SPOT, game.getBoardOccupant(i))
        }
    }

    @Test
    fun setMove_placesPlayerAtOpenLocation() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 4)
        assertEquals(TicTacToeGame.HUMAN_PLAYER, game.getBoardOccupant(4))
    }

    @Test
    fun setMove_doesNotOverwriteOccupiedLocation() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 0)
        assertEquals(TicTacToeGame.HUMAN_PLAYER, game.getBoardOccupant(0))
    }

    @Test
    fun setMove_returnsTrue_whenLocationIsOpen() {
        assertTrue(game.setMove(TicTacToeGame.HUMAN_PLAYER, 4))
    }

    @Test
    fun setMove_returnsFalse_whenLocationIsOccupied() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 4)
        assertFalse(game.setMove(TicTacToeGame.COMPUTER_PLAYER, 4))
    }

    @Test
    fun setMove_returnsFalse_whenLocationIsOutOfRange() {
        assertFalse(game.setMove(TicTacToeGame.HUMAN_PLAYER, -1))
        assertFalse(game.setMove(TicTacToeGame.HUMAN_PLAYER, TicTacToeGame.BOARD_SIZE))
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
    fun checkForWinner_returnsTwo_whenHumanCompletesTopLeftToBottomRightDiagonal() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 4)
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 8)
        assertEquals(2, game.checkForWinner())
    }

    @Test
    fun checkForWinner_returnsThree_whenComputerCompletesTopRightToBottomLeftDiagonal() {
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 2)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 4)
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
        assertEquals(TicTacToeGame.OPEN_SPOT, game.getBoardOccupant(move))
    }

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
    fun clearBoard_doesNotResetDifficultyLevel() {
        game.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy)
        game.clearBoard()
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

    @Test
    fun getBoardState_returnsCurrentBoard() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setMove(TicTacToeGame.COMPUTER_PLAYER, 4)
        val state = game.getBoardState()
        assertEquals(TicTacToeGame.BOARD_SIZE, state.size)
        assertEquals(TicTacToeGame.HUMAN_PLAYER, state[0])
        assertEquals(TicTacToeGame.COMPUTER_PLAYER, state[4])
        assertEquals(TicTacToeGame.OPEN_SPOT, state[8])
    }

    @Test
    fun getBoardState_returnsACopy() {
        val state = game.getBoardState()
        state[0] = TicTacToeGame.HUMAN_PLAYER
        assertEquals(TicTacToeGame.OPEN_SPOT, game.getBoardOccupant(0))
    }

    @Test
    fun setBoardState_restoresBoard() {
        val state = charArrayOf('X', 'O', ' ', ' ', 'X', ' ', ' ', ' ', 'O')
        game.setBoardState(state)
        assertArrayEquals(state, game.getBoardState())
    }

    @Test
    fun setBoardState_copiesTheGivenArray() {
        val state = CharArray(TicTacToeGame.BOARD_SIZE) { TicTacToeGame.OPEN_SPOT }
        game.setBoardState(state)
        state[0] = TicTacToeGame.HUMAN_PLAYER
        assertEquals(TicTacToeGame.OPEN_SPOT, game.getBoardOccupant(0))
    }

    @Test
    fun setBoardState_ignoresNullOrWrongSizedArrays() {
        game.setMove(TicTacToeGame.HUMAN_PLAYER, 0)
        game.setBoardState(null)
        game.setBoardState(CharArray(3) { TicTacToeGame.COMPUTER_PLAYER })
        assertEquals(TicTacToeGame.HUMAN_PLAYER, game.getBoardOccupant(0))
        assertEquals(TicTacToeGame.OPEN_SPOT, game.getBoardOccupant(1))
    }
}
