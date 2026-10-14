package com.example.reto5

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mGame: TicTacToeGame
    private lateinit var mBoardView: BoardView
    private lateinit var mInfoTextView: TextView
    private lateinit var mHumanScoreView: TextView
    private lateinit var mTiesScoreView: TextView
    private lateinit var mComputerScoreView: TextView

    // Sound effects; only alive between onResume() and onPause().
    private var mHumanMediaPlayer: MediaPlayer? = null
    private var mComputerMediaPlayer: MediaPlayer? = null

    // Delays the computer's move so its turn message and sound are noticeable.
    private val mHandler = Handler(Looper.getMainLooper())

    private var mGameOver = false
    private var mHumanGoesFirst = true
    // True while waiting for the delayed computer move; blocks human touches.
    private var mComputerTurn = false

    private var mHumanScore = 0
    private var mTiesScore = 0
    private var mComputerScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mInfoTextView = findViewById(R.id.information)
        mHumanScoreView = findViewById(R.id.human_score)
        mTiesScoreView = findViewById(R.id.ties_score)
        mComputerScoreView = findViewById(R.id.computer_score)

        mGame = TicTacToeGame()
        mBoardView = findViewById(R.id.board)
        mBoardView.setGame(mGame)

        // Listen for touches on the board
        mBoardView.setOnTouchListener(mTouchListener)

        startNewGame()
    }

    override fun onResume() {
        super.onResume()
        mHumanMediaPlayer = MediaPlayer.create(applicationContext, R.raw.human_move)
        mComputerMediaPlayer = MediaPlayer.create(applicationContext, R.raw.computer_move)
    }

    override fun onPause() {
        super.onPause()
        mHumanMediaPlayer?.release()
        mComputerMediaPlayer?.release()
        mHumanMediaPlayer = null
        mComputerMediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }

    // Sets up (or resets) the board for a new game. Alternates who goes
    // first each time it's called.
    private fun startNewGame() {
        // Cancel a pending computer move from the previous game
        mHandler.removeCallbacksAndMessages(null)
        mComputerTurn = false

        mGame.clearBoard()
        mBoardView.invalidate() // Redraw the board
        mGameOver = false

        if (mHumanGoesFirst) {
            mInfoTextView.setText(R.string.first_human)
        } else {
            makeComputerMove(computerWentFirst = true)
        }
        mHumanGoesFirst = !mHumanGoesFirst
    }

    // Listen for touches on the board
    private val mTouchListener = View.OnTouchListener { v, event ->
        v.performClick()

        // Determine which cell was touched (clamped so touching the very
        // edge of the view can't produce an out-of-range column/row)
        val col = (event.x.toInt() / mBoardView.getBoardCellWidth()).coerceIn(0, 2)
        val row = (event.y.toInt() / mBoardView.getBoardCellHeight()).coerceIn(0, 2)
        val pos = row * 3 + col

        if (!mGameOver && !mComputerTurn && setMove(TicTacToeGame.HUMAN_PLAYER, pos)) {
            val winner = mGame.checkForWinner()
            if (winner == 0) {
                // If no winner yet, let the computer make a move
                makeComputerMove(computerWentFirst = false)
            } else {
                showResult(winner)
            }
        }

        // So we aren't notified of continued events when finger is moved
        false
    }

    // Shows "Android's turn" and makes the computer's move one second later
    // without blocking the UI thread.
    private fun makeComputerMove(computerWentFirst: Boolean) {
        mComputerTurn = true
        mInfoTextView.setText(R.string.turn_computer)
        mHandler.postDelayed({
            setMove(TicTacToeGame.COMPUTER_PLAYER, mGame.getComputerMove())
            mComputerTurn = false
            if (computerWentFirst) {
                mInfoTextView.setText(R.string.first_computer)
            } else {
                showResult(mGame.checkForWinner())
            }
        }, COMPUTER_DELAY_MS)
    }

    // Updates the info text for the given checkForWinner() status and, if
    // the game is over, the scores.
    private fun showResult(winner: Int) {
        when (winner) {
            0 -> mInfoTextView.setText(R.string.turn_human)
            1 -> {
                mInfoTextView.setText(R.string.result_tie)
                mTiesScore++
                endGame()
            }
            2 -> {
                mInfoTextView.setText(R.string.result_human_wins)
                mHumanScore++
                endGame()
            }
            else -> {
                mInfoTextView.setText(R.string.result_computer_wins)
                mComputerScore++
                endGame()
            }
        }
    }

    private fun endGame() {
        mGameOver = true
        updateScoreDisplay()
    }

    private fun updateScoreDisplay() {
        mHumanScoreView.text = getString(R.string.score_human, mHumanScore)
        mTiesScoreView.text = getString(R.string.score_ties, mTiesScore)
        mComputerScoreView.text = getString(R.string.score_computer, mComputerScore)
    }

    // Makes the move in the model and, if it was legal, redraws the board
    // and plays that player's sound effect.
    private fun setMove(player: Char, location: Int): Boolean {
        if (mGame.setMove(player, location)) {
            mBoardView.invalidate() // Redraw the board
            if (player == TicTacToeGame.HUMAN_PLAYER) {
                mHumanMediaPlayer?.start() // Play the sound effect
            } else {
                mComputerMediaPlayer?.start()
            }
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_game -> {
                startNewGame()
                return true
            }
            R.id.ai_difficulty -> {
                showDialog(DIALOG_DIFFICULTY_ID)
                return true
            }
            R.id.quit -> {
                showDialog(DIALOG_QUIT_ID)
                return true
            }
            R.id.about -> {
                showDialog(DIALOG_ABOUT_ID)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(id: Int): Dialog? {
        val builder = AlertDialog.Builder(this)

        when (id) {
            DIALOG_DIFFICULTY_ID -> {
                builder.setTitle(R.string.difficulty_choose)
                val levels: Array<CharSequence> = arrayOf(
                    getString(R.string.difficulty_easy),
                    getString(R.string.difficulty_harder),
                    getString(R.string.difficulty_expert)
                )
                val selected = mGame.getDifficultyLevel().ordinal
                builder.setSingleChoiceItems(levels, selected) { dialog, item ->
                    dialog.dismiss()
                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[item])
                    Toast.makeText(applicationContext, levels[item], Toast.LENGTH_SHORT).show()
                }
                return builder.create()
            }
            DIALOG_QUIT_ID -> {
                builder.setMessage(R.string.quit_question)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ -> finish() }
                    .setNegativeButton(R.string.no, null)
                return builder.create()
            }
            DIALOG_ABOUT_ID -> {
                val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val layout = inflater.inflate(R.layout.about_dialog, null)
                builder.setView(layout)
                builder.setPositiveButton(R.string.about_ok, null)
                return builder.create()
            }
        }

        return super.onCreateDialog(id)
    }

    companion object {
        private const val DIALOG_DIFFICULTY_ID = 0
        private const val DIALOG_QUIT_ID = 1
        private const val DIALOG_ABOUT_ID = 2
        private const val COMPUTER_DELAY_MS = 1000L
    }
}
