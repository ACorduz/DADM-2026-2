package com.example.reto4

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mGame: TicTacToeGame
    private lateinit var mBoardButtons: Array<Button>
    private lateinit var mInfoTextView: TextView
    private lateinit var mHumanScoreView: TextView
    private lateinit var mTiesScoreView: TextView
    private lateinit var mComputerScoreView: TextView

    private var mGameOver = false
    private var mHumanGoesFirst = true

    private var mHumanScore = 0
    private var mTiesScore = 0
    private var mComputerScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBoardButtons = arrayOf(
            findViewById(R.id.one), findViewById(R.id.two), findViewById(R.id.three),
            findViewById(R.id.four), findViewById(R.id.five), findViewById(R.id.six),
            findViewById(R.id.seven), findViewById(R.id.eight), findViewById(R.id.nine)
        )
        mInfoTextView = findViewById(R.id.information)
        mHumanScoreView = findViewById(R.id.human_score)
        mTiesScoreView = findViewById(R.id.ties_score)
        mComputerScoreView = findViewById(R.id.computer_score)

        mGame = TicTacToeGame()

        startNewGame()
    }

    // Sets up (or resets) the board for a new game. Alternates who goes
    // first each time it's called, per the "extra challenge".
    private fun startNewGame() {
        mGame.clearBoard()
        mGameOver = false

        for (i in mBoardButtons.indices) {
            mBoardButtons[i].text = ""
            mBoardButtons[i].isEnabled = true
            mBoardButtons[i].setTextColor(Color.BLACK)
            mBoardButtons[i].setOnClickListener { onBoardButtonClicked(i) }
        }

        if (mHumanGoesFirst) {
            mInfoTextView.setText(R.string.first_human)
        } else {
            val move = mGame.getComputerMove()
            setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            mInfoTextView.setText(R.string.first_computer)
        }
        mHumanGoesFirst = !mHumanGoesFirst
    }

    private fun onBoardButtonClicked(location: Int) {
        if (mGameOver || !mBoardButtons[location].isEnabled) {
            return
        }

        setMove(TicTacToeGame.HUMAN_PLAYER, location)

        var winner = mGame.checkForWinner()
        if (winner == 0) {
            mInfoTextView.setText(R.string.turn_computer)
            val move = mGame.getComputerMove()
            setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            winner = mGame.checkForWinner()
        }

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

    // Updates the model, disables the button, and colors X green / O red.
    private fun setMove(player: Char, location: Int) {
        mGame.setMove(player, location)
        val button = mBoardButtons[location]
        button.isEnabled = false
        button.text = player.toString()
        button.setTextColor(
            if (player == TicTacToeGame.HUMAN_PLAYER) Color.rgb(0, 200, 0)
            else Color.rgb(200, 0, 0)
        )
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
    }
}
