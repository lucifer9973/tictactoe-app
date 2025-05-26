package com.rajshobhit.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TictactoeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GameScreen()
                }
            }
        }
    }
}

@Composable
fun GameScreen() {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var gameStatus by remember { mutableStateOf("Your Turn (O)") }
    var userTurn by remember { mutableStateOf(true) }
    var computerMoveTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(500)
        board = board.toMutableList().apply { set(4, "X") }
        userTurn = true
    }

    // Handle computer moves
    LaunchedEffect(computerMoveTrigger) {
        if (computerMoveTrigger > 0 && !userTurn) {
            delay(1000)
            computerMove(board) { updatedBoard ->
                board = updatedBoard
                userTurn = true
                when {
                    checkWin(updatedBoard, "X") -> gameStatus = "Computer Wins! 💻"
                    checkTie(updatedBoard) -> gameStatus = "It's a Tie! 🤝"
                    else -> gameStatus = "Your Turn (O)"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = gameStatus,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        GridBoard(board) { index ->
            if (userTurn && board[index].isEmpty()) {
                // User move
                board = board.toMutableList().apply { set(index, "O") }
                userTurn = false

                when {
                    checkWin(board, "O") -> gameStatus = "You Won! 🎉"
                    checkTie(board) -> gameStatus = "It's a Tie! 🤝"
                    else -> {
                        gameStatus = "Computer's Turn..."
                        computerMoveTrigger++ // Trigger computer move
                    }
                }
            }
        }
    }
}

@Composable
fun GridBoard(board: List<String>, onClick: (Int) -> Unit) {
    Column {
        repeat(3) { row ->
            Row {
                repeat(3) { col ->
                    val index = row * 3 + col
                    Button(
                        onClick = { onClick(index) },
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp),
                        enabled = board[index].isEmpty()
                    ) {
                        Text(
                            text = board[index].ifEmpty { "" },
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}

fun computerMove(currentBoard: List<String>, callback: (List<String>) -> Unit) {
    val emptyCells = currentBoard.indices.filter { currentBoard[it].isEmpty() }
    if (emptyCells.isEmpty()) return

    val updatedBoard = currentBoard.toMutableList().apply {
        set(emptyCells.random(), "X")
    }
    callback(updatedBoard)
}

fun checkWin(board: List<String>, player: String): Boolean {
    val winPatterns = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Rows
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Columns
        listOf(0, 4, 8), listOf(2, 4, 6) // Diagonals
    )
    return winPatterns.any { pattern ->
        pattern.all { board[it] == player }
    }
}

fun checkTie(board: List<String>): Boolean = board.none { it.isEmpty() }

// Replace the TictactoeTheme composable with this
@Composable
fun TictactoeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF121212)
        ),
        content = content
    )
}