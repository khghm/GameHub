package com.gamehub.games.tictactoe.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamehub.games.tictactoe.TicTacToeState
import com.gamehub.shared.graphics.DeviceTierDetector
import com.gamehub.shared.graphics.theme.DefaultGraphicsSpec
import com.gamehub.shared.graphics.theme.GraphicsSpec
import com.gamehub.shared.graphics.theme.ThemeManager
import com.gamehub.shared.graphics.animation.bounceClick
import com.gamehub.shared.graphics.animation.pulse
import com.gamehub.shared.graphics.animation.slideIn
import com.gamehub.shared.graphics.layout.GameGrid
import com.gamehub.shared.graphics.particles.ParticleEffect
import com.gamehub.shared.graphics.particles.ParticleSystem

object TicTacToeGraphicsSpec : GraphicsSpec() {
    override val primaryColor: Color = Color(0xFF1A237E)
    override val secondaryColor: Color = Color(0xFF0D47A1)
    override val accentColor: Color = Color(0xFFFFD700)
    override val backgroundColor: Color = Color(0xFF0D47A1)
    override val surfaceColor: Color = Color(0xFFF5F5F5)
    override val surfaceVariantColor: Color = Color(0xFFE0E0E0)
    override val textColor: Color = Color(0xFF1A237E)
    override val textSecondaryColor: Color = Color(0xFF666666)
    override val errorColor: Color = Color(0xFFB00020)
    override val shadowElevation: Dp = 16.dp
    override val cornerRadius: Dp = 16.dp
    override val borderWidth: Dp = 2.dp
    override val animationDurationMs: Int = 400
    override val particleEnabled: Boolean = DeviceTierDetector.shouldEnableHeavyEffects()
    override val effectEnabled: Boolean = DeviceTierDetector.shouldEnableHeavyEffects()
}

@Composable
fun TicTacToeScreen(
    state: TicTacToeState,
    onCellClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val particleSystem = remember { ParticleSystem() }
    var previousGrid by remember { mutableStateOf(state.grid.map { it.toList() }) }
    val winCellPositions = remember { mutableStateListOf<Pair<Int, Int>>() }
    val cellCenters = remember { mutableStateMapOf<Pair<Int, Int>, Offset>() }

    // Detect cell changes to trigger particles
    LaunchedEffect(state.grid) {
        val current = state.grid
        val prev = previousGrid

        for (i in 0..2) {
            for (j in 0..2) {
                if (current[i][j] != prev[i][j] && current[i][j] != null) {
                    cellCenters[Pair(i, j)]?.let { center ->
                        val isX = state.players.indexOf(current[i][j]) == 0
                        val colors = if (isX) {
                            listOf(Color(0xFFE53935), Color(0xFFFF5252), Color(0xFFFF8A80))
                        } else {
                            listOf(Color(0xFF1E88E5), Color(0xFF42A5F5), Color(0xFF82B1FF))
                        }
                        particleSystem.emitBurst(
                            centerX = center.x,
                            centerY = center.y,
                            count = 30,
                            colors = colors
                        )
                    }
                }
            }
        }

        previousGrid = current.map { it.toList() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(TicTacToeGraphicsSpec.primaryColor, TicTacToeGraphicsSpec.secondaryColor)
                    )
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .slideIn(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tic Tac Toe",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TicTacToeGraphicsSpec.primaryColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "نوبت: ${state.currentPlayer?.value?.let { if (it == "player1") "X" else "O" } ?: "?"}",
                        fontSize = 18.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.pulse(enabled = true)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Game Board
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .shadow(TicTacToeGraphicsSpec.shadowElevation, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = TicTacToeGraphicsSpec.surfaceColor)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    for (i in 0..2) {
                        Row {
                            for (j in 0..2) {
                                val cell = state.grid[i][j]
                                val isX = cell != null && state.players.indexOf(cell) == 0
                                val isO = cell != null && state.players.indexOf(cell) == 1
                                val isWinningCell = winCellPositions.contains(Pair(i, j))

                                val cellColor by animateColorAsState(
                                    targetValue = when {
                                        isWinningCell -> TicTacToeGraphicsSpec.accentColor
                                        isX -> Color(0xFFE53935)
                                        isO -> Color(0xFF1E88E5)
                                        else -> Color(0xFFFFFFFF)
                                    },
                                    animationSpec = spring<Color>(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "cellColor"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cellColor)
                                        .border(
                                            2.dp,
                                            if (isWinningCell) TicTacToeGraphicsSpec.accentColor else Color(0xFFBDBDBD),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .bounceClick { onCellClick(i, j) }
                                        .then(if (isWinningCell) Modifier.pulse() else Modifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Track cell center for particles
                                    BoxWithConstraints(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DisposableEffect(Unit) {
                                            cellCenters[Pair(i, j)] = Offset(
                                                x = constraints.maxWidth / 2f,
                                                y = constraints.maxHeight / 2f
                                            )
                                            onDispose {
                                                cellCenters.remove(Pair(i, j))
                                            }
                                        }

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isX,
                                            enter = scaleIn(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                                )
                                            ) + fadeIn(),
                                            exit = scaleOut() + fadeOut()
                                        ) {
                                            Text(
                                                text = "✕",
                                                fontSize = 48.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = isO,
                                            enter = scaleIn(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                                )
                                            ) + fadeIn(),
                                            exit = scaleOut() + fadeOut()
                                        ) {
                                            Text(
                                                text = "○",
                                                fontSize = 48.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFFE53935), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Player 1 (X)", color = Color.White, fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF1E88E5), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Player 2 (O)", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Particle Effect Layer
        ParticleEffect(
            modifier = Modifier.fillMaxSize(),
            particleSystem = particleSystem,
            isActive = TicTacToeGraphicsSpec.particleEnabled
        )
    }
}