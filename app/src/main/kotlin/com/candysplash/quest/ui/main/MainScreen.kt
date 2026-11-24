package com.candysplash.quest.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.candysplash.quest.game.BoosterTool
import com.candysplash.quest.game.CandyGameUiState
import com.candysplash.quest.game.CandyGameViewModel
import com.candysplash.quest.game.GAME_LEVELS
import com.candysplash.quest.game.GridPos
import com.candysplash.quest.game.LevelPlayStatus
import com.candysplash.quest.ui.theme.CandySplashQuestTheme

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit = {},
  modifier: Modifier = Modifier,
  gameViewModel: CandyGameViewModel = viewModel { CandyGameViewModel() },
) {
  val state by gameViewModel.uiState.collectAsStateWithLifecycle()
  CandySplashGameContent(
    state = state,
    onSelectLevel = gameViewModel::startLevel,
    onCellTapped = gameViewModel::onCellTapped,
    onCellSwiped = gameViewModel::onCellSwiped,
    onSelectBooster = gameViewModel::selectBooster,
    onShuffle = gameViewModel::triggerShuffleBooster,
    onHint = gameViewModel::showHint,
    onToggleMute = gameViewModel::toggleMute,
    onAddBonusMoves = gameViewModel::addBonusMoves,
    modifier = modifier,
  )
}

@Composable
fun CandySplashGameContent(
  state: CandyGameUiState,
  onSelectLevel: (Int) -> Unit,
  onCellTapped: (GridPos) -> Unit,
  onCellSwiped: (GridPos, GridPos) -> Unit,
  onSelectBooster: (BoosterTool) -> Unit,
  onShuffle: () -> Unit,
  onHint: () -> Unit,
  onToggleMute: () -> Unit,
  onAddBonusMoves: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bgGradient = Brush.verticalGradient(
    colors = listOf(
      Color(0xFF3B0764),
      Color(0xFF6B21A8),
      Color(0xFF9D174D),
    )
  )

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(bgGradient)
      .safeDrawingPadding()
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    val isLandscape = maxWidth > maxHeight

    if (isLandscape) {
      Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left Panel: Level Map, Score, Moves, Obstacle Goals
        LandscapeLeftStatsPanel(
          state = state,
          onSelectLevel = onSelectLevel,
          onToggleMute = onToggleMute,
          modifier = Modifier
            .width(215.dp)
            .fillMaxHeight(),
        )

        // Center Panel: Cheer Banner + 2D Interactive Candy Board
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          CheerBannerBar(cheerText = state.cheerBanner)
          CandyBoardCanvas(
            board = state.board,
            selectedCell = state.selectedCell,
            hintMove = state.hintMove,
            activeBursts = state.activeBursts,
            onCellTapped = onCellTapped,
            onCellSwiped = onCellSwiped,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
          )
        }

        // Right Panel: Kid-Friendly Power Boosters & Actions
        LandscapeRightBoosterPanel(
          state = state,
          onSelectBooster = onSelectBooster,
          onShuffle = onShuffle,
          onHint = onHint,
          onRestartLevel = { onSelectLevel(state.levelIndex) },
          modifier = Modifier
            .width(155.dp)
            .fillMaxHeight(),
        )
      }
    } else {
      // Portrait fallback layout
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        PortraitTopHud(
          state = state,
          onSelectLevel = onSelectLevel,
          onToggleMute = onToggleMute,
        )
        CheerBannerBar(cheerText = state.cheerBanner)
        CandyBoardCanvas(
          board = state.board,
          selectedCell = state.selectedCell,
          hintMove = state.hintMove,
          activeBursts = state.activeBursts,
          onCellTapped = onCellTapped,
          onCellSwiped = onCellSwiped,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        )
        PortraitBottomBoosters(
          state = state,
          onSelectBooster = onSelectBooster,
          onShuffle = onShuffle,
          onHint = onHint,
          onRestartLevel = { onSelectLevel(state.levelIndex) },
        )
      }
    }

    // Victory or Out-of-Moves Modal Overlay
    AnimatedVisibility(
      visible = state.playStatus != LevelPlayStatus.PLAYING,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      LevelResultOverlay(
        state = state,
        onReplay = { onSelectLevel(state.levelIndex) },
        onNextLevel = {
          val nextIdx = (state.levelIndex + 1) % GAME_LEVELS.size
          onSelectLevel(nextIdx)
        },
        onAddBonusMoves = onAddBonusMoves,
      )
    }
  }
}

@Composable
private fun LandscapeLeftStatsPanel(
  state: CandyGameUiState,
  onSelectLevel: (Int) -> Unit,
  onToggleMute: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(Color(0xFF2E1065).copy(alpha = 0.82f))
      .border(2.dp, Color(0xFFF472B6).copy(alpha = 0.7f), RoundedCornerShape(18.dp))
      .padding(10.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Title + Sound Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "🍬 CANDY QUEST",
        color = Color(0xFFFFF176),
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
      )
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(Color(0xFF7E22CE))
          .clickable { onToggleMute() }
          .padding(horizontal = 8.dp, vertical = 4.dp),
      ) {
        Text(
          text = if (state.isMuted) "🔇" else "🔊",
          fontSize = 13.sp,
        )
      }
    }

    // Quick Level Selector Pills (Levels 1..5)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      GAME_LEVELS.forEachIndexed { idx, lvl ->
        val isCurrent = idx == state.levelIndex
        val bg = if (isCurrent) Color(0xFFFF4081) else Color(0xFF581C87)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(
              width = if (isCurrent) 2.dp else 1.dp,
              color = if (isCurrent) Color(0xFFFFEA00) else Color(0xFFA855F7),
              shape = RoundedCornerShape(10.dp),
            )
            .clickable { onSelectLevel(idx) }
            .padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
          Text(
            text = "Lv.${lvl.levelNumber}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
          )
        }
      }
    }

    // Current Level Name
    Text(
      text = "World ${state.levelConfig.levelNumber}: ${state.levelConfig.title}",
      color = Color(0xFFFBCFE8),
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )

    // Moves & Score Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      StatBadge(
        label = "MOVES",
        value = "${state.movesLeft}",
        accentColor = if (state.movesLeft <= 5) Color(0xFFFF5252) else Color(0xFF00E676),
        modifier = Modifier.weight(1f),
      )
      StatBadge(
        label = "SCORE",
        value = "${state.score}",
        accentColor = Color(0xFFFFEA00),
        modifier = Modifier.weight(1.2f),
      )
    }

    // Star Progress Bar
    val progress = (state.score.toFloat() / state.levelConfig.targetScore.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Goal: ${state.levelConfig.targetScore}",
          color = Color(0xFFE9D5FF),
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
        )
        val starsText = "⭐".repeat(state.starsEarned) + "☆".repeat(3 - state.starsEarned)
        Text(text = starsText, fontSize = 12.sp, color = Color(0xFFFFEA00))
      }
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = Color(0xFFFFEA00),
        trackColor = Color(0xFF4C1D95),
      )
    }

    // Obstacle Targets Section
    Text(
      text = "LEVEL TARGETS",
      color = Color(0xFFF472B6),
      fontWeight = FontWeight.ExtraBold,
      fontSize = 11.sp,
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (state.levelConfig.targetJelly > 0) {
        ObstacleGoalChip(
          icon = "🟨",
          name = "Jelly",
          remaining = state.jellyRemaining,
          modifier = Modifier.weight(1f),
        )
      }
      if (state.levelConfig.targetIce > 0) {
        ObstacleGoalChip(
          icon = "🧊",
          name = "Ice",
          remaining = state.iceRemaining,
          modifier = Modifier.weight(1f),
        )
      }
      if (state.levelConfig.targetCrates > 0) {
        ObstacleGoalChip(
          icon = "🧇",
          name = "Waffle",
          remaining = state.cratesRemaining,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun LandscapeRightBoosterPanel(
  state: CandyGameUiState,
  onSelectBooster: (BoosterTool) -> Unit,
  onShuffle: () -> Unit,
  onHint: () -> Unit,
  onRestartLevel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(Color(0xFF2E1065).copy(alpha = 0.82f))
      .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.7f), RoundedCornerShape(18.dp))
      .padding(10.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "⚡ BOOSTERS",
      color = Color(0xFF7DD3FC),
      fontWeight = FontWeight.ExtraBold,
      fontSize = 13.sp,
    )

    BoosterButton(
      icon = "🔨",
      title = "Hammer",
      count = state.hammerCount,
      isSelected = state.activeBooster == BoosterTool.LOLLIPOP_HAMMER,
      color = Color(0xFFFF4081),
      onClick = { onSelectBooster(BoosterTool.LOLLIPOP_HAMMER) },
      modifier = Modifier.fillMaxWidth(),
    )

    BoosterButton(
      icon = "🌈",
      title = "Rainbow",
      count = state.rainbowBoosterCount,
      isSelected = state.activeBooster == BoosterTool.COLOR_BOMB_DROP,
      color = Color(0xFFA855F7),
      onClick = { onSelectBooster(BoosterTool.COLOR_BOMB_DROP) },
      modifier = Modifier.fillMaxWidth(),
    )

    BoosterButton(
      icon = "🌀",
      title = "Shuffle",
      count = state.shuffleCount,
      isSelected = false,
      color = Color(0xFF0EA5E9),
      onClick = onShuffle,
      modifier = Modifier.fillMaxWidth(),
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      SmallActionPill(
        text = "💡 Hint",
        bgColor = Color(0xFFEAB308),
        onClick = onHint,
        modifier = Modifier.weight(1f),
      )
      SmallActionPill(
        text = "🔄 Reset",
        bgColor = Color(0xFF64748B),
        onClick = onRestartLevel,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun CheerBannerBar(cheerText: String?) {
  Surface(
    color = Color(0xFF4C1D95).copy(alpha = 0.9f),
    shape = RoundedCornerShape(50),
    modifier = Modifier
      .border(1.5.dp, Color(0xFFFFEA00).copy(alpha = 0.8f), RoundedCornerShape(50))
      .padding(horizontal = 14.dp, vertical = 3.dp),
  ) {
    Text(
      text = cheerText ?: "Match 3 or more candies! 🍭",
      color = Color(0xFFFFF9C4),
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
  }
}

@Composable
private fun StatBadge(
  label: String,
  value: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF4C1D95))
      .border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
      .padding(vertical = 5.dp, horizontal = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = label,
      color = Color(0xFFD8B4FE),
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = value,
      color = accentColor,
      fontSize = 18.sp,
      fontWeight = FontWeight.ExtraBold,
    )
  }
}

@Composable
private fun ObstacleGoalChip(
  icon: String,
  name: String,
  remaining: Int,
  modifier: Modifier = Modifier,
) {
  val done = remaining <= 0
  val borderCol = if (done) Color(0xFF4ADE80) else Color(0xFFFBBF24)
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xFF3B0764))
      .border(1.5.dp, borderCol, RoundedCornerShape(10.dp))
      .padding(vertical = 4.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(text = icon, fontSize = 14.sp)
    Text(
      text = if (done) "✔" else "$remaining",
      color = if (done) Color(0xFF4ADE80) else Color.White,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 13.sp,
    )
    Text(
      text = name,
      color = Color(0xFFE9D5FF),
      fontSize = 9.sp,
      maxLines = 1,
    )
  }
}

@Composable
private fun BoosterButton(
  icon: String,
  title: String,
  count: Int,
  isSelected: Boolean,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (isSelected) Color(0xFFFFEA00) else Color.White.copy(alpha = 0.45f)
  val bg = if (isSelected) color else color.copy(alpha = 0.75f)

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(bg)
      .border(if (isSelected) 3.dp else 1.5.dp, borderColor, RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(text = icon, fontSize = 16.sp)
      Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
      )
    }
    Box(
      modifier = Modifier
        .clip(CircleShape)
        .background(Color.Black.copy(alpha = 0.35f))
        .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
      Text(
        text = "$count",
        color = Color(0xFFFFEA00),
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
      )
    }
  }
}

@Composable
private fun SmallActionPill(
  text: String,
  bgColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(bgColor)
      .clickable { onClick() }
      .padding(vertical = 7.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = 11.sp,
    )
  }
}

@Composable
private fun PortraitTopHud(
  state: CandyGameUiState,
  onSelectLevel: (Int) -> Unit,
  onToggleMute: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xFF2E1065).copy(alpha = 0.85f))
      .border(1.5.dp, Color(0xFFF472B6), RoundedCornerShape(16.dp))
      .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "🍬 Lv.${state.levelConfig.levelNumber}: ${state.levelConfig.title}",
        color = Color(0xFFFFF176),
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        GAME_LEVELS.forEachIndexed { idx, lvl ->
          val active = idx == state.levelIndex
          Box(
            modifier = Modifier
              .clip(CircleShape)
              .background(if (active) Color(0xFFFF4081) else Color(0xFF581C87))
              .clickable { onSelectLevel(idx) }
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(text = "${lvl.levelNumber}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF7E22CE))
            .clickable { onToggleMute() }
            .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
          Text(text = if (state.isMuted) "🔇" else "🔊", fontSize = 12.sp)
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StatBadge("MOVES", "${state.movesLeft}", Color(0xFF00E676), Modifier.weight(1f))
      StatBadge("SCORE", "${state.score}/${state.levelConfig.targetScore}", Color(0xFFFFEA00), Modifier.weight(1.5f))
      if (state.levelConfig.targetJelly > 0) {
        ObstacleGoalChip("🟨", "Jelly", state.jellyRemaining, Modifier.weight(0.8f))
      }
      if (state.levelConfig.targetIce > 0) {
        ObstacleGoalChip("🧊", "Ice", state.iceRemaining, Modifier.weight(0.8f))
      }
      if (state.levelConfig.targetCrates > 0) {
        ObstacleGoalChip("🧇", "Waffle", state.cratesRemaining, Modifier.weight(0.8f))
      }
    }
  }
}

@Composable
private fun PortraitBottomBoosters(
  state: CandyGameUiState,
  onSelectBooster: (BoosterTool) -> Unit,
  onShuffle: () -> Unit,
  onHint: () -> Unit,
  onRestartLevel: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    BoosterButton(
      icon = "🔨",
      title = "Smash",
      count = state.hammerCount,
      isSelected = state.activeBooster == BoosterTool.LOLLIPOP_HAMMER,
      color = Color(0xFFFF4081),
      onClick = { onSelectBooster(BoosterTool.LOLLIPOP_HAMMER) },
      modifier = Modifier.weight(1f),
    )
    BoosterButton(
      icon = "🌈",
      title = "Star",
      count = state.rainbowBoosterCount,
      isSelected = state.activeBooster == BoosterTool.COLOR_BOMB_DROP,
      color = Color(0xFFA855F7),
      onClick = { onSelectBooster(BoosterTool.COLOR_BOMB_DROP) },
      modifier = Modifier.weight(1f),
    )
    BoosterButton(
      icon = "🌀",
      title = "Mix",
      count = state.shuffleCount,
      isSelected = false,
      color = Color(0xFF0EA5E9),
      onClick = onShuffle,
      modifier = Modifier.weight(1f),
    )
    SmallActionPill("💡 Hint", Color(0xFFEAB308), onHint, Modifier.weight(0.7f))
    SmallActionPill("🔄", Color(0xFF64748B), onRestartLevel, Modifier.weight(0.45f))
  }
}

@Composable
private fun LevelResultOverlay(
  state: CandyGameUiState,
  onReplay: () -> Unit,
  onNextLevel: () -> Unit,
  onAddBonusMoves: () -> Unit,
) {
  val isWin = state.playStatus == LevelPlayStatus.WON
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.65f)),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .width(340.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.verticalGradient(
            colors = if (isWin) {
              listOf(Color(0xFF6B21A8), Color(0xFF3B0764))
            } else {
              listOf(Color(0xFF475569), Color(0xFF1E293B))
            }
          )
        )
        .border(3.dp, Color(0xFFFFEA00), RoundedCornerShape(24.dp))
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = if (isWin) "🎉 SWEET VICTORY! 🎉" else "😮 OUT OF MOVES!",
        color = Color(0xFFFFEA00),
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
      )

      if (isWin) {
        val earned = maxOf(1, state.starsEarned)
        Text(
          text = "⭐".repeat(earned),
          fontSize = 32.sp,
        )
      }

      Text(
        text = "Final Score: ${state.score}",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = onReplay,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
        ) {
          Text("🔄 Play Again", fontWeight = FontWeight.Bold)
        }

        if (isWin) {
          Button(
            onClick = onNextLevel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
          ) {
            Text("Next Level ▶", fontWeight = FontWeight.Bold)
          }
        } else {
          Button(
            onClick = onAddBonusMoves,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
          ) {
            Text("+5 Moves 🍬", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Preview(
  name = "Candy Splash Landscape",
  showBackground = true,
  widthDp = 840,
  heightDp = 390,
)
@Composable
fun MainScreenPreview() {
  val engine = com.candysplash.quest.game.CandyGameEngine()
  val config = GAME_LEVELS[3] // Rainbow Carnival with Jelly, Ice & Waffle Crates!
  val sampleBoard = engine.createInitialBoard(config)
  val sampleState = CandyGameUiState(
    levelIndex = 3,
    levelConfig = config,
    board = sampleBoard,
    score = 1850,
    movesLeft = 18,
    jellyRemaining = 5,
    iceRemaining = 4,
    cratesRemaining = 3,
    cheerBanner = "SUPER SWEET! 🍭 (2x COMBO!)",
  )
  CandySplashQuestTheme {
    CandySplashGameContent(
      state = sampleState,
      onSelectLevel = {},
      onCellTapped = {},
      onCellSwiped = { _, _ -> },
      onSelectBooster = {},
      onShuffle = {},
      onHint = {},
      onToggleMute = {},
      onAddBonusMoves = {},
    )
  }
}
