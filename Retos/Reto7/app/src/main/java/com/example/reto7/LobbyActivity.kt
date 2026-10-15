package com.example.reto7

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import com.google.firebase.auth.FirebaseAuth

/**
 * The app's first screen: lists the online games waiting for an
 * opponent, and lets the player join one, create one, or play against
 * Android instead. Signs in anonymously so the database knows who's who.
 */
class LobbyActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mRepository: GameRepository
    private lateinit var mPrefs: SharedPreferences

    private lateinit var mPlayerNameView: TextView
    private lateinit var mStatusView: TextView
    private lateinit var mCreateButton: Button
    private lateinit var mRetryButton: Button
    private lateinit var mAdapter: ArrayAdapter<String>

    // The games shown in the list, as (gameId, game) pairs
    private var mGames: List<Pair<String, OnlineGame>> = emptyList()
    private var mGamesSubscription: GameRepository.Subscription? = null
    // True while creating or joining a game; ignores extra taps
    private var mBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)
        applySystemBarPadding(findViewById(R.id.root))

        mPlayerNameView = findViewById(R.id.player_name)
        mStatusView = findViewById(R.id.lobby_status)
        mCreateButton = findViewById(R.id.create_game_button)
        mRetryButton = findViewById(R.id.retry_button)

        mAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        val gamesList = findViewById<ListView>(R.id.games_list)
        gamesList.adapter = mAdapter
        gamesList.emptyView = findViewById(R.id.empty_games)
        gamesList.setOnItemClickListener { _, _, position, _ -> joinGame(mGames[position].first) }

        mCreateButton.setOnClickListener { createGame() }
        mRetryButton.setOnClickListener { signIn() }
        findViewById<Button>(R.id.play_android_button).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        findViewById<ImageButton>(R.id.menu_button).setOnClickListener { anchor ->
            showMenuPopup(anchor, R.menu.lobby_menu) { itemId ->
                when (itemId) {
                    R.id.change_name -> { showNameDialog(); true }
                    R.id.about -> { showAboutDialog(this); true }
                    else -> false
                }
            }
        }

        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()
        mRepository = GameRepository()

        displayPlayerName()
        if (playerName().isEmpty()) showNameDialog()
        signIn()
    }

    override fun onStart() {
        super.onStart()
        if (mAuth.currentUser != null) startWatchingGames()
    }

    override fun onStop() {
        super.onStop()
        mGamesSubscription?.cancel()
        mGamesSubscription = null
    }

    // Signs in anonymously (Firebase remembers the user between launches)
    private fun signIn() {
        if (mAuth.currentUser != null) {
            onSignedIn()
            return
        }
        mStatusView.setText(R.string.lobby_connecting)
        mRetryButton.visibility = View.GONE
        mAuth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                onSignedIn()
            } else {
                mStatusView.setText(R.string.lobby_connect_failed)
                mRetryButton.visibility = View.VISIBLE
            }
        }
    }

    private fun onSignedIn() {
        mStatusView.setText(R.string.lobby_available_games)
        mRetryButton.visibility = View.GONE
        mCreateButton.isEnabled = true
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startWatchingGames()
    }

    private fun startWatchingGames() {
        if (mGamesSubscription != null) return
        mGamesSubscription = mRepository.observeWaitingGames { games ->
            // Our own waiting games aren't joinable
            val uid = mAuth.currentUser?.uid
            mGames = games.filter { it.second.hostUid != uid }
            mAdapter.clear()
            mAdapter.addAll(mGames.map { getString(R.string.lobby_game_item, it.second.hostName) })
        }
    }

    private fun createGame() {
        val uid = mAuth.currentUser?.uid ?: return
        if (mBusy || !ensurePlayerName()) return
        mBusy = true
        mRepository.createGame(uid, playerName()) { gameId ->
            mBusy = false
            if (gameId != null) {
                openGame(gameId, OnlineGame.MARK_X)
            } else {
                Toast.makeText(this, R.string.lobby_create_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun joinGame(gameId: String) {
        val uid = mAuth.currentUser?.uid ?: return
        if (mBusy || !ensurePlayerName()) return
        mBusy = true
        mRepository.joinGame(gameId, uid, playerName()) { joined ->
            mBusy = false
            if (joined) {
                openGame(gameId, OnlineGame.MARK_O)
            } else {
                Toast.makeText(this, R.string.lobby_game_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGame(gameId: String, myMark: Char) {
        val intent = Intent(this, OnlineGameActivity::class.java)
            .putExtra(OnlineGameActivity.EXTRA_GAME_ID, gameId)
            .putExtra(OnlineGameActivity.EXTRA_MY_MARK, myMark)
        startActivity(intent)
    }

    private fun playerName(): String = mPrefs.getString(KEY_PLAYER_NAME, "") ?: ""

    // Asks for a name first if the player hasn't chosen one yet
    private fun ensurePlayerName(): Boolean {
        if (playerName().isNotEmpty()) return true
        showNameDialog()
        return false
    }

    private fun displayPlayerName() {
        val name = playerName()
        mPlayerNameView.text = if (name.isEmpty()) "" else getString(R.string.lobby_playing_as, name)
    }

    private fun showNameDialog() {
        val layout = layoutInflater.inflate(R.layout.dialog_name, null)
        val input = layout.findViewById<EditText>(R.id.name_input)
        input.setText(playerName())

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.name_title)
            .setView(layout)
            .setPositiveButton(R.string.about_ok, null)
            .setCancelable(playerName().isNotEmpty())
            .create()
        // Replace the OK listener so an empty name keeps the dialog open
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    input.error = getString(R.string.name_empty)
                } else {
                    mPrefs.edit { putString(KEY_PLAYER_NAME, name) }
                    displayPlayerName()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    companion object {
        private const val PREFS_NAME = "ttt_prefs"
        private const val KEY_PLAYER_NAME = "playerName"
    }
}
