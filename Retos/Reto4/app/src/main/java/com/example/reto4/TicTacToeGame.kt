package com.example.reto4

import kotlin.random.Random

/**
 * Tic-Tac-Toe game model. Owns the board and the computer's AI
 * (win if possible, otherwise block, otherwise pick randomly).
 * All UI concerns live in MainActivity, which only talks to this
 * class through its public methods.
 */
class TicTacToeGame {

    private val mBoard = CharArray(BOARD_SIZE) { OPEN_SPOT }
    private val mRand = Random(System.currentTimeMillis())

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

    /**
     * Returns the best move for the computer to make. You must call
     * setMove() to actually make the computer move to that location.
     */
    fun getComputerMove(): Int {
        // First see if there's a move O can make to win right now.
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                val current = mBoard[i]
                mBoard[i] = COMPUTER_PLAYER
                val winner = checkForWinner()
                mBoard[i] = current
                if (winner == 3) return i
            }
        }
        // Otherwise, see if the human could win on their next move, and block them.
        for (i in 0 until BOARD_SIZE) {
            if (mBoard[i] == OPEN_SPOT) {
                val current = mBoard[i]
                mBoard[i] = HUMAN_PLAYER
                val winner = checkForWinner()
                mBoard[i] = current
                if (winner == 2) return i
            }
        }
        // Otherwise, pick a random open spot.
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
