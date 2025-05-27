package com.rajshobhit.tictactoe.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class GameViewModel : ViewModel() {

    enum class GameMode { PVP, PVE, ONLINE }

    private val _board = mutableStateListOf<String>().apply { addAll(List(9) { "" }) }
    val board: List<String> get() = _board

    var gameMode by mutableStateOf(GameMode.PVP)
    var isComputerTurn by mutableStateOf(false)
    var currentPlayer by mutableStateOf("X")
    var statusText by mutableStateOf("Player X's Turn")
    var gameOver by mutableStateOf(false)
    var xWins by mutableStateOf(0)
    var oWins by mutableStateOf(0)
    var history by mutableStateOf(listOf<String>())
    var winningCells by mutableStateOf(listOf<Int>())

    // Multiplayer state
    var onlineGameId by mutableStateOf("")
    var isHost by mutableStateOf(false)
    var playerId by mutableStateOf("")
    var connectionStatus by mutableStateOf("")

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    init {
        playerId = auth.currentUser?.uid ?: UUID.randomUUID().toString().take(8)
    }

    fun updateGameMode(mode: GameMode) {
        gameMode = mode
        resetGame()
    }

    fun createOnlineGame() {
        onlineGameId = database.child("games").push().key ?: UUID.randomUUID().toString().take(8)
        isHost = true
        connectionStatus = "Waiting for opponent..."

        val initialData = mapOf(
            "playerX" to playerId,
            "playerO" to "",
            "board" to List(9) { "" },
            "currentPlayer" to "X",
            "status" to "waiting"
        )

        database.child("games").child(onlineGameId).setValue(initialData)
        listenToOnlineGame()
    }

    fun joinOnlineGame(gameId: String) {
        onlineGameId = gameId
        isHost = false
        connectionStatus = "Connecting..."

        val gameRef = database.child("games").child(gameId)
        gameRef.child("playerO").setValue(playerId)
        gameRef.child("status").setValue("playing")

        listenToOnlineGame()
    }

    private fun listenToOnlineGame() {
        database.child("games").child(onlineGameId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val boardData = snapshot.child("board").children.mapNotNull {
                        it.getValue(String::class.java)
                    }

                    val current = snapshot.child("currentPlayer").getValue(String::class.java)
                    val status = snapshot.child("status").getValue(String::class.java)

                    if (boardData.size == 9) {
                        _board.clear()
                        _board.addAll(boardData)
                    }

                    currentPlayer = current ?: "X"
                    connectionStatus = when (status) {
                        "waiting" -> "Waiting for opponent..."
                        "playing" -> if (isMyTurn()) "Your turn" else "Opponent's turn"
                        else -> "Game ended"
                    }

                    gameOver = status == "ended"

                    if (!gameOver) {
                        checkOnlineWin()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    connectionStatus = "Connection failed: ${error.message}"
                }
            })
    }

    private fun isMyTurn(): Boolean {
        return (isHost && currentPlayer == "X") || (!isHost && currentPlayer == "O")
    }

    fun makeMove(index: Int) {
        if (_board[index].isNotEmpty() || gameOver) return

        when (gameMode) {
            GameMode.ONLINE -> {
                if (isMyTurn()) {
                    _board[index] = currentPlayer
                    updateOnlineMove()
                    checkOnlineWin()
                }
            }
            else -> {
                _board[index] = currentPlayer
                checkWin()?.let {
                    handleWin(it)
                } ?: run {
                    if (_board.all { it.isNotEmpty() }) handleTie()
                    else nextTurn()
                }
            }
        }
    }

    private fun updateOnlineMove() {
        val gameRef = database.child("games").child(onlineGameId)
        gameRef.child("board").setValue(_board)
        gameRef.child("currentPlayer").setValue(if (currentPlayer == "X") "O" else "X")
    }

    private fun checkOnlineWin() {
        checkWin()?.let {
            database.child("games").child(onlineGameId).child("status").setValue("ended")
            handleWin(it)
        } ?: run {
            if (_board.all { it.isNotEmpty() }) {
                database.child("games").child(onlineGameId).child("status").setValue("ended")
                handleTie()
            }
        }
    }

    private fun handleWin(pattern: List<Int>) {
        statusText = "Player $currentPlayer Wins!"
        gameOver = true
        winningCells = pattern
        if (currentPlayer == "X") xWins++ else oWins++
        history = history + "Player $currentPlayer won"
    }

    private fun handleTie() {
        statusText = "It's a Tie!"
        gameOver = true
        history = history + "Game Tied"
    }

    private fun nextTurn() {
        currentPlayer = if (currentPlayer == "X") "O" else "X"
        statusText = "Player $currentPlayer's Turn"

        if (gameMode == GameMode.PVE && currentPlayer == "O") {
            isComputerTurn = true
            viewModelScope.launch {
                delay(1000)
                computerMove()
                isComputerTurn = false
            }
        }
    }

    private fun computerMove() {
        val move = findBestMove()
        if (move != -1) makeMove(move)
    }

    private fun findBestMove(): Int {
        checkWinningMove("O")?.let { return it }
        checkWinningMove("X")?.let { return it }
        if (_board[4].isEmpty()) return 4
        return _board.indices.filter { _board[it].isEmpty() }.randomOrNull() ?: -1
    }

    private fun checkWinningMove(player: String): Int? {
        val patterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (pattern in patterns) {
            val values = pattern.map { _board[it] }
            if (values.count { it == player } == 2 && values.count { it == "" } == 1) {
                return pattern[values.indexOf("")] // get empty cell index
            }
        }
        return null
    }

    fun resetGame() {
        _board.replaceAll { "" }
        currentPlayer = "X"
        statusText = "Player X's Turn"
        gameOver = false
        winningCells = emptyList()
        isComputerTurn = false

        if (gameMode == GameMode.ONLINE) {
            database.child("games").child(onlineGameId).apply {
                child("board").setValue(List(9) { "" })
                child("currentPlayer").setValue("X")
                child("status").setValue("playing")
            }
        }

        if (gameMode == GameMode.PVE && currentPlayer == "O") {
            isComputerTurn = true
            viewModelScope.launch {
                delay(1000)
                computerMove()
                isComputerTurn = false
            }
        }
    }

    private fun checkWin(): List<Int>? {
        val patterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        return patterns.firstOrNull { pattern ->
            pattern.all { _board[it] == currentPlayer }
        }
    }
}
