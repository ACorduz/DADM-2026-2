package com.example.reto7

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

/**
 * The only class that talks to the Firebase Realtime Database. Every
 * change to a game runs one of the OnlineGameRules inside a transaction,
 * so two devices can't both join a game or move at the same time.
 * Callbacks run on the main thread.
 */
class GameRepository {

    /** A registered database listener; cancel() stops it. */
    class Subscription(private val query: Query, private val listener: ValueEventListener) {
        fun cancel() = query.removeEventListener(listener)
    }

    private val mGames: DatabaseReference = FirebaseDatabase.getInstance().reference.child(GAMES)

    /** Calls onChange with every game waiting for an opponent, newest first. */
    fun observeWaitingGames(onChange: (List<Pair<String, OnlineGame>>) -> Unit): Subscription {
        val query = mGames.orderByChild(STATUS).equalTo(OnlineGame.STATUS_WAITING)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val games = snapshot.children.mapNotNull { child ->
                    val key = child.key
                    val game = OnlineGame.fromMap(child.value as? Map<*, *>)
                    if (key != null && game != null) key to game else null
                }
                onChange(games.sortedByDescending { it.second.createdAt })
            }

            override fun onCancelled(error: DatabaseError) = onChange(emptyList())
        }
        query.addValueEventListener(listener)
        return Subscription(query, listener)
    }

    /** Calls onChange with the game's state on every change (null once it's deleted). */
    fun observeGame(gameId: String, onChange: (OnlineGame?) -> Unit): Subscription {
        val ref = mGames.child(gameId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) =
                onChange(OnlineGame.fromMap(snapshot.value as? Map<*, *>))

            override fun onCancelled(error: DatabaseError) = onChange(null)
        }
        ref.addValueEventListener(listener)
        return Subscription(ref, listener)
    }

    /** Creates a game waiting for an opponent; onResult gets its id, or null on failure. */
    fun createGame(uid: String, name: String, onResult: (String?) -> Unit) {
        val ref = mGames.push()
        val data = OnlineGameRules.newGame(uid, name).toMap() + (CREATED_AT to ServerValue.TIMESTAMP)
        ref.setValue(data).addOnCompleteListener { task ->
            onResult(if (task.isSuccessful) ref.key else null)
        }
    }

    fun joinGame(gameId: String, uid: String, name: String, onResult: (Boolean) -> Unit) =
        update(gameId, { OnlineGameRules.join(it, uid, name) }) { onResult(it?.guestUid == uid) }

    fun makeMove(gameId: String, mark: Char, pos: Int, onResult: (Boolean) -> Unit) =
        update(gameId, { OnlineGameRules.applyMove(it, mark, pos) }) { onResult(it != null) }

    fun requestRematch(gameId: String, mark: Char) =
        update(gameId, { OnlineGameRules.requestRematch(it, mark) }) {}

    /** While waiting for an opponent, delete the game if this device disconnects. */
    fun armDisconnectForWaiting(gameId: String) {
        mGames.child(gameId).onDisconnect().removeValue()
    }

    /** During a match, mark the game abandoned if this device disconnects. */
    fun armDisconnectForPlaying(gameId: String) {
        val ref = mGames.child(gameId)
        ref.onDisconnect().cancel()
        ref.child(STATUS).onDisconnect().setValue(OnlineGame.STATUS_ABANDONED)
    }

    /**
     * Leaves the game on purpose: a game in progress is marked abandoned so
     * the opponent finds out; otherwise (waiting, or already abandoned) the
     * game is deleted.
     */
    fun leaveGame(gameId: String, status: String?) {
        val ref = mGames.child(gameId)
        ref.onDisconnect().cancel()
        if (status == OnlineGame.STATUS_PLAYING) {
            ref.child(STATUS).setValue(OnlineGame.STATUS_ABANDONED)
        } else {
            ref.removeValue()
        }
    }

    // Applies the rule to the game in a transaction; onResult gets the
    // committed state, or null if the rule rejected it or it failed.
    private fun update(
        gameId: String,
        rule: (OnlineGame) -> OnlineGame?,
        onResult: (OnlineGame?) -> Unit
    ) {
        mGames.child(gameId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                // Before the first server round trip the local value may be
                // missing; returning it unchanged makes Firebase retry with
                // the server's value.
                val game = OnlineGame.fromMap(currentData.value as? Map<*, *>)
                    ?: return Transaction.success(currentData)
                val updated = rule(game) ?: return Transaction.abort()
                currentData.value = updated.toMap()
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                val game = if (error == null && committed) {
                    OnlineGame.fromMap(snapshot?.value as? Map<*, *>)
                } else {
                    null
                }
                onResult(game)
            }
        })
    }

    companion object {
        private const val GAMES = "games"
        private const val STATUS = "status"
        private const val CREATED_AT = "createdAt"
    }
}
