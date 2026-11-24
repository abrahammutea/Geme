package com.candysplash.quest.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LevelPlayStatus {
  PLAYING,
  WON,
  OUT_OF_MOVES,
}

data class CandyGameUiState(
  val levelIndex: Int = 0,
  val levelConfig: LevelConfig = GAME_LEVELS.first(),
  val board: List<List<BoardCell>> = emptyList(),
  val selectedCell: GridPos? = null,
  val hintMove: Pair<GridPos, GridPos>? = null,
  val score: Int = 0,
  val highScore: Int = 0,
  val movesLeft: Int = 20,
  val jellyRemaining: Int = 0,
  val iceRemaining: Int = 0,
  val cratesRemaining: Int = 0,
  val comboChain: Int = 0,
  val cheerBanner: String? = "Tap or swipe candies to match 3! 🍬",
  val activeBursts: List<ParticleBurst> = emptyList(),
  val activeBooster: BoosterTool = BoosterTool.NONE,
  val hammerCount: Int = 3,
  val shuffleCount: Int = 3,
  val rainbowBoosterCount: Int = 2,
  val isAnimating: Boolean = false,
  val isMuted: Boolean = false,
  val playStatus: LevelPlayStatus = LevelPlayStatus.PLAYING,
  val unlockedLevelIndex: Int = 0,
  val starsPerLevel: Map<Int, Int> = emptyMap(),
) {
  val starsEarned: Int
    get() = when {
      score >= (levelConfig.targetScore * 1.6).toInt() -> 3
      score >= (levelConfig.targetScore * 1.15).toInt() -> 2
      score >= levelConfig.targetScore -> 1
      else -> 0
    }
}

class CandyGameViewModel : ViewModel() {
  private val engine = CandyGameEngine()
  private val sfx = CandySoundEffects()

  private val _uiState = MutableStateFlow(CandyGameUiState())
  val uiState: StateFlow<CandyGameUiState> = _uiState.asStateFlow()

  init {
    startLevel(0)
  }

  fun startLevel(index: Int) {
    val safeIndex = index.coerceIn(0, GAME_LEVELS.lastIndex)
    val config = GAME_LEVELS[safeIndex]
    var initialBoard = engine.createInitialBoard(config)
    if (engine.findBestHintMove(initialBoard) == null) {
      initialBoard = engine.shuffleBoard(initialBoard)
    }

    val (jellyCount, iceCount, crateCount) = countBoardObstacles(initialBoard)

    _uiState.update { prev ->
      prev.copy(
        levelIndex = safeIndex,
        levelConfig = config,
        board = initialBoard,
        selectedCell = null,
        hintMove = null,
        score = 0,
        movesLeft = config.maxMoves,
        jellyRemaining = jellyCount,
        iceRemaining = iceCount,
        cratesRemaining = crateCount,
        comboChain = 0,
        cheerBanner = "Level ${config.levelNumber}: ${config.subtitle}",
        activeBursts = emptyList(),
        activeBooster = BoosterTool.NONE,
        hammerCount = 3,
        shuffleCount = 3,
        rainbowBoosterCount = 2,
        isAnimating = false,
        playStatus = LevelPlayStatus.PLAYING,
      )
    }
  }

  fun toggleMute() {
    val newMuted = !_uiState.value.isMuted
    sfx.isMuted = newMuted
    _uiState.update { it.copy(isMuted = newMuted) }
  }

  fun selectBooster(tool: BoosterTool) {
    val state = _uiState.value
    if (state.isAnimating || state.playStatus != LevelPlayStatus.PLAYING) return

    if (state.activeBooster == tool) {
      _uiState.update {
        it.copy(
          activeBooster = BoosterTool.NONE,
          cheerBanner = "Booster cancelled. Keep matching! 🍬",
        )
      }
      return
    }

    when (tool) {
      BoosterTool.LOLLIPOP_HAMMER -> {
        if (state.hammerCount > 0) {
          _uiState.update {
            it.copy(
              activeBooster = BoosterTool.LOLLIPOP_HAMMER,
              selectedCell = null,
              cheerBanner = "Tap ANY candy or waffle crate to smash it! 🔨",
            )
          }
        } else {
          _uiState.update { it.copy(cheerBanner = "No Lollipop Hammers left this level!") }
        }
      }
      BoosterTool.COLOR_BOMB_DROP -> {
        if (state.rainbowBoosterCount > 0) {
          _uiState.update {
            it.copy(
              activeBooster = BoosterTool.COLOR_BOMB_DROP,
              selectedCell = null,
              cheerBanner = "Tap any candy to turn it into a Rainbow Star! 🌈",
            )
          }
        } else {
          _uiState.update { it.copy(cheerBanner = "No Rainbow Stars left this level!") }
        }
      }
      BoosterTool.NONE -> {
        _uiState.update { it.copy(activeBooster = BoosterTool.NONE) }
      }
    }
  }

  fun triggerShuffleBooster() {
    val state = _uiState.value
    if (state.isAnimating || state.playStatus != LevelPlayStatus.PLAYING) return
    if (state.shuffleCount <= 0) {
      _uiState.update { it.copy(cheerBanner = "No Shuffles left!") }
      return
    }

    sfx.playPop(2)
    val shuffled = engine.shuffleBoard(state.board)
    _uiState.update {
      it.copy(
        board = shuffled,
        shuffleCount = (it.shuffleCount - 1).coerceAtLeast(0),
        selectedCell = null,
        hintMove = null,
        activeBooster = BoosterTool.NONE,
        cheerBanner = "Board Shuffled! Fresh candies incoming! 🌀",
      )
    }
  }

  fun showHint() {
    val state = _uiState.value
    if (state.isAnimating || state.playStatus != LevelPlayStatus.PLAYING) return
    val hint = engine.findBestHintMove(state.board)
    if (hint != null) {
      sfx.playPop(1)
      _uiState.update {
        it.copy(
          hintMove = hint,
          cheerBanner = "Try swapping the glowing candies! 💡",
        )
      }
    } else {
      val shuffled = engine.shuffleBoard(state.board)
      _uiState.update {
        it.copy(
          board = shuffled,
          cheerBanner = "No moves left — Auto-Shuffled for free! 🌀",
        )
      }
    }
  }

  fun addBonusMoves() {
    _uiState.update {
      it.copy(
        movesLeft = it.movesLeft + 5,
        playStatus = LevelPlayStatus.PLAYING,
        cheerBanner = "+5 Bonus Moves! You can do it! 🌟",
      )
    }
  }

  fun onCellTapped(pos: GridPos) {
    val state = _uiState.value
    if (state.isAnimating || state.playStatus != LevelPlayStatus.PLAYING) return

    // Check if an active booster is selected
    if (state.activeBooster == BoosterTool.LOLLIPOP_HAMMER) {
      applyHammerOnCell(pos)
      return
    }
    if (state.activeBooster == BoosterTool.COLOR_BOMB_DROP) {
      applyRainbowBoosterOnCell(pos)
      return
    }

    val cell = state.board.getOrNull(pos.row)?.getOrNull(pos.col) ?: return
    if (cell.obstacle != ObstacleType.NONE || cell.candy == null) {
      _uiState.update {
        it.copy(
          selectedCell = null,
          cheerBanner = "Match candies next to Waffle Crates to break them! 🧇",
        )
      }
      return
    }

    val currentSelected = state.selectedCell
    if (currentSelected == null) {
      sfx.playPop(1)
      _uiState.update { it.copy(selectedCell = pos, hintMove = null) }
    } else if (currentSelected == pos) {
      // If tapping a special candy twice, activate it directly!
      if (cell.candy.special != SpecialPower.NONE) {
        triggerDirectSpecialCandy(pos)
      } else {
        _uiState.update { it.copy(selectedCell = null) }
      }
    } else if (engine.areAdjacent(currentSelected, pos)) {
      attemptSwap(currentSelected, pos)
    } else {
      sfx.playPop(1)
      _uiState.update { it.copy(selectedCell = pos, hintMove = null) }
    }
  }

  fun onCellSwiped(from: GridPos, to: GridPos) {
    val state = _uiState.value
    if (state.isAnimating || state.playStatus != LevelPlayStatus.PLAYING) return
    if (!engine.areAdjacent(from, to)) return
    attemptSwap(from, to)
  }

  private fun applyHammerOnCell(pos: GridPos) {
    viewModelScope.launch {
      _uiState.update { it.copy(isAnimating = true, activeBooster = BoosterTool.NONE, hintMove = null) }
      sfx.playSpecialBlast()
      val smashRes = engine.smashSingleCell(_uiState.value.board, pos)
      val refilled = engine.applyGravityAndRefill(smashRes.board, _uiState.value.levelConfig.candyTypesCount)
      val (jelly, ice, crates) = countBoardObstacles(refilled)

      _uiState.update { prev ->
        val newScore = prev.score + smashRes.pointsEarned
        prev.copy(
          board = refilled,
          score = newScore,
          highScore = maxOf(prev.highScore, newScore),
          hammerCount = (prev.hammerCount - 1).coerceAtLeast(0),
          jellyRemaining = jelly,
          iceRemaining = ice,
          cratesRemaining = crates,
          activeBursts = smashRes.bursts,
          cheerBanner = smashRes.cheerWord ?: "SMASHED! 🔨",
        )
      }
      delay(260)
      runCascadeLoop(initialCombo = 1)
    }
  }

  private fun applyRainbowBoosterOnCell(pos: GridPos) {
    val state = _uiState.value
    val cell = state.board.getOrNull(pos.row)?.getOrNull(pos.col) ?: return
    if (cell.candy == null) return

    sfx.playSpecialBlast()
    val upgraded = engine.upgradeCellToRainbow(state.board, pos)
    _uiState.update {
      it.copy(
        board = upgraded,
        rainbowBoosterCount = (it.rainbowBoosterCount - 1).coerceAtLeast(0),
        activeBooster = BoosterTool.NONE,
        selectedCell = null,
        cheerBanner = "Rainbow Star ready! Swap it with any candy! 🌈",
      )
    }
  }

  private fun triggerDirectSpecialCandy(pos: GridPos) {
    // Tapping a Bomb or Striped candy twice triggers it immediately
    val neighbor = listOf(
      GridPos(pos.row, pos.col + 1),
      GridPos(pos.row, pos.col - 1),
      GridPos(pos.row + 1, pos.col),
      GridPos(pos.row - 1, pos.col),
    ).firstOrNull { n ->
      engine.canSwap(_uiState.value.board, pos, n)
    } ?: return
    attemptSwap(pos, neighbor, forceSpecialTrigger = true)
  }

  private fun attemptSwap(a: GridPos, b: GridPos, forceSpecialTrigger: Boolean = false) {
    val state = _uiState.value
    if (!engine.canSwap(state.board, a, b)) {
      sfx.playInvalidMove()
      _uiState.update { it.copy(selectedCell = null) }
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isAnimating = true,
          selectedCell = null,
          hintMove = null,
          activeBooster = BoosterTool.NONE,
        )
      }

      val isRainbowOrDualSpecial = engine.isRainbowSwap(state.board, a, b)
      val swappedBoard = engine.swapCandies(state.board, a, b)
      _uiState.update { it.copy(board = swappedBoard) }
      delay(160)

      val hasMatch = isRainbowOrDualSpecial || forceSpecialTrigger || engine.hasAnyMatch(swappedBoard)
      if (!hasMatch) {
        // Invalid move: swap back!
        sfx.playInvalidMove()
        val reverted = engine.swapCandies(swappedBoard, a, b)
        _uiState.update {
          it.copy(
            board = reverted,
            isAnimating = false,
            cheerBanner = "Oops! That swap doesn't make a match of 3! 🍬",
          )
        }
        return@launch
      }

      // Valid move: consume 1 move and resolve cascade
      _uiState.update { it.copy(movesLeft = (it.movesLeft - 1).coerceAtLeast(0)) }

      val firstStep = engine.resolveSpecialSwapAndMatches(
        board = swappedBoard,
        swapA = a,
        swapB = b,
        comboMultiplier = 1,
      )

      if (firstStep.hadMatches) {
        if (isRainbowOrDualSpecial) sfx.playSpecialBlast() else sfx.playPop(1)
        applyStepToUi(firstStep, combo = 1)
        delay(240)
        val afterGravity = engine.applyGravityAndRefill(
          firstStep.board,
          _uiState.value.levelConfig.candyTypesCount,
        )
        _uiState.update { it.copy(board = afterGravity, activeBursts = emptyList()) }
        delay(180)
      }

      runCascadeLoop(initialCombo = 2)
    }
  }

  private suspend fun runCascadeLoop(initialCombo: Int) {
    var combo = initialCombo
    while (true) {
      val currentBoard = _uiState.value.board
      val step = engine.resolveSpecialSwapAndMatches(
        board = currentBoard,
        comboMultiplier = combo,
      )
      if (!step.hadMatches) break

      sfx.playPop(combo)
      applyStepToUi(step, combo)
      delay(240)

      val refilled = engine.applyGravityAndRefill(
        step.board,
        _uiState.value.levelConfig.candyTypesCount,
      )
      _uiState.update { it.copy(board = refilled, activeBursts = emptyList()) }
      delay(180)
      combo++
    }

    // Ensure at least one valid move exists; if not, auto-shuffle
    var finalBoard = _uiState.value.board
    if (engine.findBestHintMove(finalBoard) == null) {
      finalBoard = engine.shuffleBoard(finalBoard)
    }

    val (jellyLeft, iceLeft, cratesLeft) = countBoardObstacles(finalBoard)
    val currentState = _uiState.value
    val allGoalsCleared = jellyLeft == 0 && iceLeft == 0 && cratesLeft == 0 &&
      currentState.score >= currentState.levelConfig.targetScore

    val newStatus = when {
      allGoalsCleared -> LevelPlayStatus.WON
      currentState.movesLeft <= 0 -> LevelPlayStatus.OUT_OF_MOVES
      else -> LevelPlayStatus.PLAYING
    }

    if (newStatus == LevelPlayStatus.WON) {
      sfx.playLevelWin()
    }

    _uiState.update { prev ->
      val earnedStars = maxOf(1, prev.starsEarned)
      val updatedStars = if (newStatus == LevelPlayStatus.WON) {
        val oldStars = prev.starsPerLevel[prev.levelIndex] ?: 0
        prev.starsPerLevel + (prev.levelIndex to maxOf(oldStars, earnedStars))
      } else {
        prev.starsPerLevel
      }
      val newUnlocked = if (newStatus == LevelPlayStatus.WON) {
        maxOf(prev.unlockedLevelIndex, (prev.levelIndex + 1).coerceAtMost(GAME_LEVELS.lastIndex))
      } else {
        prev.unlockedLevelIndex
      }

      prev.copy(
        board = finalBoard,
        jellyRemaining = jellyLeft,
        iceRemaining = iceLeft,
        cratesRemaining = cratesLeft,
        activeBursts = emptyList(),
        isAnimating = false,
        playStatus = newStatus,
        unlockedLevelIndex = newUnlocked,
        starsPerLevel = updatedStars,
        cheerBanner = when (newStatus) {
          LevelPlayStatus.WON -> "LEVEL COMPLETE! Super Sweet Victory! 🏆🎉"
          LevelPlayStatus.OUT_OF_MOVES -> "Out of moves! Grab +5 Bonus Moves or Try Again! 💪"
          LevelPlayStatus.PLAYING -> prev.cheerBanner
        },
      )
    }
  }

  private fun applyStepToUi(step: MatchStepResult, combo: Int) {
    val (jellyLeft, iceLeft, cratesLeft) = countBoardObstacles(step.board)
    _uiState.update { prev ->
      val updatedScore = prev.score + step.pointsEarned
      prev.copy(
        board = step.board,
        score = updatedScore,
        highScore = maxOf(prev.highScore, updatedScore),
        jellyRemaining = jellyLeft,
        iceRemaining = iceLeft,
        cratesRemaining = cratesLeft,
        comboChain = combo,
        activeBursts = step.bursts,
        cheerBanner = if (combo > 1) "${step.cheerWord ?: "SWEET!"} (${combo}x COMBO!)" else step.cheerWord,
      )
    }
  }

  private fun countBoardObstacles(board: List<List<BoardCell>>): Triple<Int, Int, Int> {
    var jelly = 0
    var ice = 0
    var crates = 0
    for (row in board) {
      for (cell in row) {
        if (cell.hasJelly) jelly++
        if (cell.iceLayers > 0) ice++
        if (cell.obstacle == ObstacleType.WAFFLE_CRATE) crates++
      }
    }
    return Triple(jelly, ice, crates)
  }
}
