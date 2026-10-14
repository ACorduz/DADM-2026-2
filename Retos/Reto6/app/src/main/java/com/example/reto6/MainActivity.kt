package com.example.reto6

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

    // Keeps the scores and difficulty level between app launches.
    private lateinit var mPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // There's no title bar, so keep the content clear of the status and
        // navigation bars (and any display cutout in landscape).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        mInfoTextView = findViewById(R.id.information)
        mHumanScoreView = findViewById(R.id.human_score)
        mTiesScoreView = findViewById(R.id.ties_score)
        mComputerScoreView = findViewById(R.id.computer_score)

        mGame = TicTacToeGame()
        mBoardView = findViewById(R.id.board)
        mBoardView.setGame(mGame)

        // Listen for touches on the board
        mBoardView.setOnTouchListener(mTouchListener)

        // Without a title bar, this button shows the options menu.
        findViewById<ImageButton>(R.id.menu_button).setOnClickListener { showOptionsPopup(it) }

        // Restore the scores and difficulty level saved by onStop()
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        mHumanScore = mPrefs.getInt(KEY_HUMAN_WINS, 0)
        mComputerScore = mPrefs.getInt(KEY_COMPUTER_WINS, 0)
        mTiesScore = mPrefs.getInt(KEY_TIES, 0)
        val levels = TicTacToeGame.DifficultyLevel.values()
        val savedLevel = mPrefs.getInt(KEY_DIFFICULTY, TicTacToeGame.DifficultyLevel.Expert.ordinal)
        mGame.setDifficultyLevel(levels.getOrElse(savedLevel) { TicTacToeGame.DifficultyLevel.Expert })

        if (savedInstanceState == null) {
            startNewGame()
        } else {
            // Restore the game's state (e.g. after an orientation change)
            mGame.setBoardState(savedInstanceState.getCharArray(KEY_BOARD))
            mGameOver = savedInstanceState.getBoolean(KEY_GAME_OVER)
            mInfoTextView.text = savedInstanceState.getCharSequence(KEY_INFO)
            mHumanGoesFirst = savedInstanceState.getBoolean(KEY_HUMAN_GOES_FIRST)

            // The old Activity's pending computer move was cancelled in its
            // onDestroy(), so if it was Android's turn, move again now. An
            // empty board means the computer was the one going first.
            if (savedInstanceState.getBoolean(KEY_COMPUTER_TURN)) {
                val boardEmpty = mGame.getBoardState().all { it == TicTacToeGame.OPEN_SPOT }
                makeComputerMove(computerWentFirst = boardEmpty)
            }
        }
        displayScores()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // The scores are saved in the preferences by onStop(), not here.
        outState.putCharArray(KEY_BOARD, mGame.getBoardState())
        outState.putBoolean(KEY_GAME_OVER, mGameOver)
        outState.putCharSequence(KEY_INFO, mInfoTextView.text)
        outState.putBoolean(KEY_HUMAN_GOES_FIRST, mHumanGoesFirst)
        // Whose turn it is, so the computer doesn't move on the human's turn
        outState.putBoolean(KEY_COMPUTER_TURN, mComputerTurn)
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

    override fun onStop() {
        super.onStop()
        // Save the current scores and difficulty level
        mPrefs.edit {
            putInt(KEY_HUMAN_WINS, mHumanScore)
            putInt(KEY_COMPUTER_WINS, mComputerScore)
            putInt(KEY_TIES, mTiesScore)
            // Enums can't be stored directly, so save the level's ordinal
            putInt(KEY_DIFFICULTY, mGame.getDifficultyLevel().ordinal)
        }
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
        displayScores()
    }

    private fun displayScores() {
        mHumanScoreView.text = getString(R.string.score_human, mHumanScore)
        mTiesScoreView.text = getString(R.string.score_ties, mTiesScore)
        mComputerScoreView.text = getString(R.string.score_computer, mComputerScore)
    }

    private fun resetScores() {
        mHumanScore = 0
        mComputerScore = 0
        mTiesScore = 0
        displayScores()
        Toast.makeText(applicationContext, R.string.scores_reset, Toast.LENGTH_SHORT).show()
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

    // Shows the options menu anchored to the given view; selections go
    // through onOptionsItemSelected() like a regular options menu.
    private fun showOptionsPopup(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.options_menu, popup.menu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            popup.setForceShowIcon(true)
        }
        popup.setOnMenuItemClickListener { onOptionsItemSelected(it) }
        popup.show()
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
            R.id.reset_scores -> {
                resetScores()
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
        private const val DIALOG_ABOUT_ID = 2
        private const val COMPUTER_DELAY_MS = 1000L

        // SharedPreferences file and keys
        private const val PREFS_NAME = "ttt_prefs"
        private const val KEY_HUMAN_WINS = "mHumanWins"
        private const val KEY_COMPUTER_WINS = "mComputerWins"
        private const val KEY_TIES = "mTies"
        private const val KEY_DIFFICULTY = "mDifficultyLevel"

        // Instance state keys
        private const val KEY_BOARD = "board"
        private const val KEY_GAME_OVER = "mGameOver"
        private const val KEY_INFO = "info"
        private const val KEY_HUMAN_GOES_FIRST = "mGoFirst"
        private const val KEY_COMPUTER_TURN = "mComputerTurn"
    }
}
