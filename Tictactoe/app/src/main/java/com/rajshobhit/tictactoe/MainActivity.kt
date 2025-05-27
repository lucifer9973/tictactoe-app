package com.rajshobhit.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajshobhit.tictactoe.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val gameViewModel: GameViewModel = viewModel()
            TicTacToeApp(gameViewModel)
        }
    }
}

@Composable
fun TicTacToeApp(viewModel: GameViewModel) {
    var isDarkTheme by remember { mutableStateOf(true) }
    val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tic-Tac-Toe") },
                    actions = {
                        IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GameModeSelector(viewModel)

                if (viewModel.gameMode == GameViewModel.GameMode.ONLINE) {
                    OnlineGameScreen(viewModel)
                } else {
                    GameContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun GameModeSelector(viewModel: GameViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        GameModeButton("PvP", GameViewModel.GameMode.PVP, viewModel)
        GameModeButton("PvE", GameViewModel.GameMode.PVE, viewModel)
        GameModeButton("Online", GameViewModel.GameMode.ONLINE, viewModel)
    }
}

@Composable
fun GameModeButton(text: String, mode: GameViewModel.GameMode, viewModel: GameViewModel) {
    Button(
        onClick = { viewModel.updateGameMode(mode) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (viewModel.gameMode == mode)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(text)
    }
}

@Composable
fun GameContent(viewModel: GameViewModel) {
    AnimatedContent(
        targetState = viewModel.statusText,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()).togetherWith(
                slideOutVertically { -it } + fadeOut()
            )
        }
    ) { status ->
        Text(
            text = status,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }

    if (viewModel.isComputerTurn) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }

    BoardGrid(viewModel)
    NewGameButton(viewModel)
    GameStatistics(viewModel)
}

@Composable
fun OnlineGameScreen(viewModel: GameViewModel) {
    var codeInput by remember { mutableStateOf("") }

    Text(text = viewModel.connectionStatus, color = MaterialTheme.colorScheme.primary)

    if (viewModel.onlineGameId.isEmpty()) {
        Button(onClick = { viewModel.createOnlineGame() }, modifier = Modifier.fillMaxWidth()) {
            Text("Create Game")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = codeInput,
            onValueChange = { codeInput = it },
            label = { Text("Enter Game Code") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = { viewModel.joinOnlineGame(codeInput) }, modifier = Modifier.fillMaxWidth()) {
            Text("Join Game")
        }
    } else {
        Text("Game Code: ${viewModel.onlineGameId}")
        GameContent(viewModel)
    }
}

@Composable
fun BoardGrid(viewModel: GameViewModel) {
    Column(modifier = Modifier.padding(8.dp)) {
        repeat(3) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) { col ->
                    val index = row * 3 + col
                    BoardCell(
                        value = viewModel.board[index],
                        onClick = { viewModel.makeMove(index) },
                        isWinningCell = viewModel.winningCells.contains(index),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun BoardCell(
    value: String,
    onClick: () -> Unit,
    isWinningCell: Boolean,
    modifier: Modifier = Modifier
) {
    val cellColor = if (isWinningCell)
        MaterialTheme.colorScheme.tertiaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .padding(4.dp)
            .background(cellColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = when (value) {
                "X" -> MaterialTheme.colorScheme.primary
                "O" -> MaterialTheme.colorScheme.secondary
                else -> Color.Transparent
            }
        )
    }
}

@Composable
fun NewGameButton(viewModel: GameViewModel) {
    Button(
        onClick = { viewModel.resetGame() },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(0.7f),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text("New Game", fontSize = 16.sp)
    }
}

@Composable
fun GameStatistics(viewModel: GameViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Statistics", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("X Wins", viewModel.xWins)
                StatItem("O Wins", viewModel.oWins)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Game History", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                items(viewModel.history) { result ->
                    Text(
                        text = result,
                        modifier = Modifier
                            .padding(4.dp)
                            .animateItemPlacement()
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value.toString(), style = MaterialTheme.typography.displaySmall)
    }
}
