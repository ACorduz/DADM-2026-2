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
