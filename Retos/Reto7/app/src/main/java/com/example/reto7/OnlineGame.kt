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
