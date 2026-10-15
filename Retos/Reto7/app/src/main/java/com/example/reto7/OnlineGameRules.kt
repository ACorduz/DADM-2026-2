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
