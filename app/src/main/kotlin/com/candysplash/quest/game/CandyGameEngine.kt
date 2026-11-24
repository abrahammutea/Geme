package com.candysplash.quest.game

import kotlin.math.abs
import kotlin.random.Random

enum class CandyType(
  val displayName: String,
  val primaryHex: Long,
  val secondaryHex: Long,
  val accentHex: Long,
) {
  STRAWBERRY("Berry", 0xFFFF3B6B, 0xFFFF85A2, 0xFFFFD1DC),
  LEMON("Sunny", 0xFFFFC700, 0xFFFFE761, 0xFFFFF8C9),
  BLUEBERRY("Splash", 0xFF1CB0F6, 0xFF72D6FF, 0xFFD6F4FF),
  APPLE("Gummy", 0xFF58CC02, 0xFF8EE000, 0xFFD7FFB8),
  GRAPE("Magic", 0xFFA560E8, 0xFFCFA2FF, 0xFFF0E0FF),
  TANGERINE("Honey", 0xFFFF8600, 0xFFFFB443, 0xFFFFE5B8),
}

enum class SpecialPower {
  NONE,
  STRIPED_HORIZONTAL,
  STRIPED_VERTICAL,
  BOMB_3X3,
  RAINBOW_STAR,
}

enum class ObstacleType {
  NONE,
  WAFFLE_CRATE, // Solid blocker: cleared by adjacent matches or explosions
}

data class CandyTile(
  val id: Long,
  val type: CandyType,
  val special: SpecialPower = SpecialPower.NONE,
  val isNewSpawn: Boolean = false,
)

data class BoardCell(
  val row: Int,
  val col: Int,
  val candy: CandyTile? = null,
  val iceLayers: Int = 0, // 0, 1, or 2 layers of ice over the candy
  val hasJelly: Boolean = false, // Golden jelly underneath the candy
  val obstacle: ObstacleType = ObstacleType.NONE, // Waffle crate blocker
  val obstacleHp: Int = 0,
)

data class GridPos(val row: Int, val col: Int)

data class ParticleBurst(
  val id: Long,
  val row: Int,
  val col: Int,
  val colorHex: Long,
  val textPopup: String? = null,
)

data class LevelConfig(
  val levelNumber: Int,
  val title: String,
  val subtitle: String,
  val rows: Int = 7,
  val cols: Int = 8,
  val maxMoves: Int,
  val targetScore: Int,
  val targetJelly: Int = 0,
  val targetIce: Int = 0,
  val targetCrates: Int = 0,
  val candyTypesCount: Int = 5,
)

val GAME_LEVELS = listOf(
  LevelConfig(
    levelNumber = 1,
    title = "Berry Meadow",
    subtitle = "Pop the golden jelly tiles!",
    rows = 6,
    cols = 7,
    maxMoves = 20,
    targetScore = 1000,
    targetJelly = 8,
    candyTypesCount = 5,
  ),
  LevelConfig(
    levelNumber = 2,
    title = "Frosty Ice Peak",
    subtitle = "Crack the crunchy ice blocks!",
    rows = 6,
    cols = 7,
    maxMoves = 22,
    targetScore = 1600,
    targetIce = 8,
    candyTypesCount = 5,
  ),
  LevelConfig(
    levelNumber = 3,
    title = "Waffle Woods",
    subtitle = "Match next to waffle crates to smash them!",
    rows = 6,
    cols = 8,
    maxMoves = 24,
    targetScore = 2200,
    targetCrates = 6,
    targetJelly = 6,
    candyTypesCount = 5,
  ),
  LevelConfig(
    levelNumber = 4,
    title = "Rainbow Carnival",
    subtitle = "Clear ice, waffles, and jelly together!",
    rows = 7,
    cols = 8,
    maxMoves = 25,
    targetScore = 3000,
    targetJelly = 8,
    targetIce = 6,
    targetCrates = 4,
    candyTypesCount = 6,
  ),
  LevelConfig(
    levelNumber = 5,
    title = "Candy Castle Party",
    subtitle = "Grand Finale! Unleash mega combos!",
    rows = 7,
    cols = 8,
    maxMoves = 28,
    targetScore = 4500,
    targetJelly = 10,
    targetIce = 8,
    targetCrates = 6,
    candyTypesCount = 6,
  ),
)

enum class BoosterTool {
  NONE,
  LOLLIPOP_HAMMER,
  COLOR_BOMB_DROP,
}

data class MatchStepResult(
  val board: List<List<BoardCell>>,
  val pointsEarned: Int,
  val jellyCleared: Int,
  val iceCleared: Int,
  val cratesCleared: Int,
  val bursts: List<ParticleBurst>,
  val cheerWord: String?,
  val hadMatches: Boolean,
)

class CandyGameEngine(private val random: Random = Random.Default) {
  private var nextTileId = 1L
  private var nextBurstId = 1L

  fun generateTileId(): Long = nextTileId++

  fun createInitialBoard(config: LevelConfig): List<List<BoardCell>> {
    val availableCandies = CandyType.entries.take(config.candyTypesCount)
    val grid = Array(config.rows) { r ->
      Array(config.cols) { c ->
        BoardCell(row = r, col = c)
      }
    }

    // Place level obstacles (Waffle Crates, Ice, Jelly) symmetrically for a pleasant look
    val cratePositions = mutableSetOf<GridPos>()
    if (config.targetCrates > 0) {
      val candidates = mutableListOf<GridPos>()
      for (r in 1 until config.rows - 1) {
        for (c in 1 until config.cols - 1) {
          if ((r + c) % 2 == 0) candidates.add(GridPos(r, c))
        }
      }
      candidates.shuffle(random)
      cratePositions.addAll(candidates.take(config.targetCrates))
    }

    val icePositions = mutableSetOf<GridPos>()
    if (config.targetIce > 0) {
      val candidates = mutableListOf<GridPos>()
      for (r in 0 until config.rows) {
        for (c in 0 until config.cols) {
          val pos = GridPos(r, c)
          if (pos !in cratePositions) candidates.add(pos)
        }
      }
      candidates.shuffle(random)
      icePositions.addAll(candidates.take(config.targetIce))
    }

    val jellyPositions = mutableSetOf<GridPos>()
    if (config.targetJelly > 0) {
      val candidates = mutableListOf<GridPos>()
      for (r in 1 until config.rows - 1) {
        for (c in 1 until config.cols - 1) {
          val pos = GridPos(r, c)
          if (pos !in cratePositions) candidates.add(pos)
        }
      }
      if (candidates.size < config.targetJelly) {
        for (r in 0 until config.rows) {
          for (c in 0 until config.cols) {
            val pos = GridPos(r, c)
            if (pos !in cratePositions && pos !in candidates) candidates.add(pos)
          }
        }
      }
      candidates.shuffle(random)
      jellyPositions.addAll(candidates.take(config.targetJelly))
    }

    for (r in 0 until config.rows) {
      for (c in 0 until config.cols) {
        val pos = GridPos(r, c)
        if (pos in cratePositions) {
          grid[r][c] = BoardCell(
            row = r,
            col = c,
            candy = null,
            obstacle = ObstacleType.WAFFLE_CRATE,
            obstacleHp = 1,
          )
        } else {
          // Pick a candy type that doesn't immediately form a 3-in-a-row
          val forbidden = mutableSetOf<CandyType>()
          if (c >= 2) {
            val c1 = grid[r][c - 1].candy?.type
            val c2 = grid[r][c - 2].candy?.type
            if (c1 != null && c1 == c2) forbidden.add(c1)
          }
          if (r >= 2) {
            val r1 = grid[r - 1][c].candy?.type
            val r2 = grid[r - 2][c].candy?.type
            if (r1 != null && r1 == r2) forbidden.add(r1)
          }
          val choices = availableCandies.filter { it !in forbidden }.ifEmpty { availableCandies }
          val chosen = choices[random.nextInt(choices.size)]
          grid[r][c] = BoardCell(
            row = r,
            col = c,
            candy = CandyTile(id = generateTileId(), type = chosen),
            iceLayers = if (pos in icePositions) 1 else 0,
            hasJelly = pos in jellyPositions,
          )
        }
      }
    }

    return grid.map { it.toList() }
  }

  fun areAdjacent(a: GridPos, b: GridPos): Boolean {
    return abs(a.row - b.row) + abs(a.col - b.col) == 1
  }

  fun canSwap(board: List<List<BoardCell>>, a: GridPos, b: GridPos): Boolean {
    if (!areAdjacent(a, b)) return false
    val cellA = board.getOrNull(a.row)?.getOrNull(a.col) ?: return false
    val cellB = board.getOrNull(b.row)?.getOrNull(b.col) ?: return false
    if (cellA.obstacle != ObstacleType.NONE || cellB.obstacle != ObstacleType.NONE) return false
    if (cellA.candy == null || cellB.candy == null) return false
    return true
  }

  fun swapCandies(board: List<List<BoardCell>>, a: GridPos, b: GridPos): List<List<BoardCell>> {
    val mutable = board.map { it.toMutableList() }.toMutableList()
    val cellA = mutable[a.row][a.col]
    val cellB = mutable[b.row][b.col]
    mutable[a.row][a.col] = cellA.copy(candy = cellB.candy)
    mutable[b.row][b.col] = cellB.copy(candy = cellA.candy)
    return mutable.map { it.toList() }
  }

  fun isRainbowSwap(board: List<List<BoardCell>>, a: GridPos, b: GridPos): Boolean {
    val candyA = board[a.row][a.col].candy ?: return false
    val candyB = board[b.row][b.col].candy ?: return false
    return candyA.special == SpecialPower.RAINBOW_STAR ||
      candyB.special == SpecialPower.RAINBOW_STAR ||
      (candyA.special != SpecialPower.NONE && candyB.special != SpecialPower.NONE)
  }

  fun hasAnyMatch(board: List<List<BoardCell>>): Boolean {
    return findMatchGroups(board).isNotEmpty()
  }

  data class MatchGroup(
    val positions: Set<GridPos>,
    val candyType: CandyType,
    val isHorizontal: Boolean,
    val isVertical: Boolean,
  )

  fun findMatchGroups(board: List<List<BoardCell>>): List<MatchGroup> {
    val rows = board.size
    val cols = board.firstOrNull()?.size ?: 0
    val horizontalRuns = mutableListOf<Pair<CandyType, Set<GridPos>>>()
    val verticalRuns = mutableListOf<Pair<CandyType, Set<GridPos>>>()

    // Horizontal runs
    for (r in 0 until rows) {
      var c = 0
      while (c < cols) {
        val type = board[r][c].candy?.type
        if (type == null) {
          c++
          continue
        }
        var end = c + 1
        while (end < cols && board[r][end].candy?.type == type) {
          end++
        }
        if (end - c >= 3) {
          val set = (c until end).map { GridPos(r, it) }.toSet()
          horizontalRuns.add(type to set)
        }
        c = end
      }
    }

    // Vertical runs
    for (c in 0 until cols) {
      var r = 0
      while (r < rows) {
        val type = board[r][c].candy?.type
        if (type == null) {
          r++
          continue
        }
        var end = r + 1
        while (end < rows && board[end][c].candy?.type == type) {
          end++
        }
        if (end - r >= 3) {
          val set = (r until end).map { GridPos(it, c) }.toSet()
          verticalRuns.add(type to set)
        }
        r = end
      }
    }

    // Merge overlapping horizontal and vertical runs of the same candy type (L / T shapes)
    val groups = mutableListOf<MatchGroup>()
    val usedVert = BooleanArray(verticalRuns.size)

    for ((hType, hSet) in horizontalRuns) {
      var mergedSet = hSet
      var hasVertOverlap = false
      for (vIdx in verticalRuns.indices) {
        if (!usedVert[vIdx] && verticalRuns[vIdx].first == hType) {
          if (mergedSet.intersect(verticalRuns[vIdx].second).isNotEmpty()) {
            mergedSet = mergedSet + verticalRuns[vIdx].second
            usedVert[vIdx] = true
            hasVertOverlap = true
          }
        }
      }
      groups.add(
        MatchGroup(
          positions = mergedSet,
          candyType = hType,
          isHorizontal = true,
          isVertical = hasVertOverlap,
        )
      )
    }

    for (vIdx in verticalRuns.indices) {
      if (!usedVert[vIdx]) {
        groups.add(
          MatchGroup(
            positions = verticalRuns[vIdx].second,
            candyType = verticalRuns[vIdx].first,
            isHorizontal = false,
            isVertical = true,
          )
        )
      }
    }

    return groups
  }

  fun resolveSpecialSwapAndMatches(
    board: List<List<BoardCell>>,
    swapA: GridPos? = null,
    swapB: GridPos? = null,
    comboMultiplier: Int = 1,
  ): MatchStepResult {
    val rows = board.size
    val cols = board.firstOrNull()?.size ?: 0
    val mutable = board.map { it.toMutableList() }.toMutableList()

    val toClear = mutableSetOf<GridPos>()
    val specialsToSpawn = mutableMapOf<GridPos, CandyTile>()

    // Check if swapA and swapB involve a Rainbow Star or dual specials
    if (swapA != null && swapB != null) {
      val candyA = mutable[swapA.row][swapA.col].candy
      val candyB = mutable[swapB.row][swapB.col].candy
      if (candyA != null && candyB != null) {
        if (candyA.special == SpecialPower.RAINBOW_STAR || candyB.special == SpecialPower.RAINBOW_STAR) {
          val targetType = if (candyA.special == SpecialPower.RAINBOW_STAR) candyB.type else candyA.type
          toClear.add(swapA)
          toClear.add(swapB)
          for (r in 0 until rows) {
            for (c in 0 until cols) {
              if (mutable[r][c].candy?.type == targetType) {
                toClear.add(GridPos(r, c))
              }
            }
          }
        } else if (candyA.special != SpecialPower.NONE && candyB.special != SpecialPower.NONE) {
          toClear.add(swapA)
          toClear.add(swapB)
          // Cross blast + 3x3
          for (c in 0 until cols) {
            toClear.add(GridPos(swapA.row, c))
            toClear.add(GridPos(swapB.row, c))
          }
          for (r in 0 until rows) {
            toClear.add(GridPos(r, swapA.col))
            toClear.add(GridPos(r, swapB.col))
          }
        }
      }
    }

    val matchGroups = findMatchGroups(mutable)
    for (group in matchGroups) {
      toClear.addAll(group.positions)

      // Determine if this group spawns a Special Power-Up Candy!
      val size = group.positions.size
      val specialPower = when {
        size >= 5 && (!group.isHorizontal || !group.isVertical) -> SpecialPower.RAINBOW_STAR
        group.isHorizontal && group.isVertical -> SpecialPower.BOMB_3X3
        size == 4 && group.isHorizontal -> SpecialPower.STRIPED_VERTICAL
        size == 4 && group.isVertical -> SpecialPower.STRIPED_HORIZONTAL
        size >= 5 -> SpecialPower.BOMB_3X3
        else -> SpecialPower.NONE
      }

      if (specialPower != SpecialPower.NONE) {
        val anchor = when {
          swapB != null && swapB in group.positions -> swapB
          swapA != null && swapA in group.positions -> swapA
          else -> group.positions.first()
        }
        specialsToSpawn[anchor] = CandyTile(
          id = generateTileId(),
          type = group.candyType,
          special = specialPower,
        )
      }
    }

    if (toClear.isEmpty()) {
      return MatchStepResult(
        board = board,
        pointsEarned = 0,
        jellyCleared = 0,
        iceCleared = 0,
        cratesCleared = 0,
        bursts = emptyList(),
        cheerWord = null,
        hadMatches = false,
      )
    }

    // Expand chain reactions from existing special candies inside `toClear`
    val expandedClear = toClear.toMutableSet()
    val processedSpecials = mutableSetOf<GridPos>()
    var addedNew = true
    while (addedNew) {
      addedNew = false
      for (pos in expandedClear.toList()) {
        if (pos in processedSpecials) continue
        val candy = mutable[pos.row][pos.col].candy ?: continue
        if (candy.special != SpecialPower.NONE) {
          processedSpecials.add(pos)
          when (candy.special) {
            SpecialPower.STRIPED_HORIZONTAL -> {
              for (c in 0 until cols) {
                if (expandedClear.add(GridPos(pos.row, c))) addedNew = true
              }
            }
            SpecialPower.STRIPED_VERTICAL -> {
              for (r in 0 until rows) {
                if (expandedClear.add(GridPos(r, pos.col))) addedNew = true
              }
            }
            SpecialPower.BOMB_3X3 -> {
              for (dr in -1..1) {
                for (dc in -1..1) {
                  val nr = pos.row + dr
                  val nc = pos.col + dc
                  if (nr in 0 until rows && nc in 0 until cols) {
                    if (expandedClear.add(GridPos(nr, nc))) addedNew = true
                  }
                }
              }
            }
            SpecialPower.RAINBOW_STAR -> {
              val randomType = CandyType.entries[random.nextInt(CandyType.entries.size)]
              for (r in 0 until rows) {
                for (c in 0 until cols) {
                  if (mutable[r][c].candy?.type == randomType) {
                    if (expandedClear.add(GridPos(r, c))) addedNew = true
                  }
                }
              }
            }
            SpecialPower.NONE -> {}
          }
        }
      }
    }

    var jellyCleared = 0
    var iceCleared = 0
    var cratesCleared = 0
    val bursts = mutableListOf<ParticleBurst>()

    // Damage adjacent Waffle Crates next to any matched cell
    val adjacentCratesToSmash = mutableSetOf<GridPos>()
    for (pos in expandedClear) {
      val neighbors = listOf(
        GridPos(pos.row - 1, pos.col),
        GridPos(pos.row + 1, pos.col),
        GridPos(pos.row, pos.col - 1),
        GridPos(pos.row, pos.col + 1),
        pos,
      )
      for (n in neighbors) {
        if (n.row in 0 until rows && n.col in 0 until cols) {
          if (mutable[n.row][n.col].obstacle == ObstacleType.WAFFLE_CRATE) {
            adjacentCratesToSmash.add(n)
          }
        }
      }
    }

    for (cratePos in adjacentCratesToSmash) {
      val cell = mutable[cratePos.row][cratePos.col]
      cratesCleared++
      mutable[cratePos.row][cratePos.col] = cell.copy(
        obstacle = ObstacleType.NONE,
        obstacleHp = 0,
      )
      bursts.add(
        ParticleBurst(
          id = nextBurstId++,
          row = cratePos.row,
          col = cratePos.col,
          colorHex = 0xFFD9822B,
          textPopup = "CRUNCH!",
        )
      )
    }

    // Clear matched candies, crack ice, and pop jelly
    for (pos in expandedClear) {
      val cell = mutable[pos.row][pos.col]
      val candyColor = cell.candy?.type?.primaryHex ?: 0xFFFF66A3

      var newIce = cell.iceLayers
      if (newIce > 0) {
        newIce -= 1
        iceCleared++
      }

      var newJelly = cell.hasJelly
      if (newJelly && newIce == 0) {
        newJelly = false
        jellyCleared++
      }

      val spawnedSpecial = specialsToSpawn[pos]
      mutable[pos.row][pos.col] = cell.copy(
        candy = spawnedSpecial,
        iceLayers = newIce,
        hasJelly = newJelly,
      )

      bursts.add(
        ParticleBurst(
          id = nextBurstId++,
          row = pos.row,
          col = pos.col,
          colorHex = candyColor,
          textPopup = if (spawnedSpecial != null) "POWER-UP!" else null,
        )
      )
    }

    val basePoints = expandedClear.size * 60 +
      jellyCleared * 120 +
      iceCleared * 120 +
      cratesCleared * 150 +
      specialsToSpawn.size * 200
    val totalPoints = basePoints * comboMultiplier

    val cheerWord = when {
      specialsToSpawn.values.any { it.special == SpecialPower.RAINBOW_STAR } -> "RAINBOW MAGIC! 🌈"
      expandedClear.size >= 8 || comboMultiplier >= 3 -> "MEGA SPLASH! 🌟"
      expandedClear.size >= 5 || comboMultiplier == 2 -> "SUPER SWEET! 🍭"
      jellyCleared > 0 || iceCleared > 0 || cratesCleared > 0 -> "YUMMY CRUNCH! 🎉"
      else -> "SWEET! ✨"
    }

    return MatchStepResult(
      board = mutable.map { it.toList() },
      pointsEarned = totalPoints,
      jellyCleared = jellyCleared,
      iceCleared = iceCleared,
      cratesCleared = cratesCleared,
      bursts = bursts,
      cheerWord = cheerWord,
      hadMatches = true,
    )
  }

  fun applyGravityAndRefill(
    board: List<List<BoardCell>>,
    candyTypesCount: Int,
  ): List<List<BoardCell>> {
    val rows = board.size
    val cols = board.firstOrNull()?.size ?: 0
    val availableCandies = CandyType.entries.take(candyTypesCount)
    val mutable = board.map { it.toMutableList() }.toMutableList()

    for (c in 0 until cols) {
      // Collect existing candies in this column from bottom to top, respecting waffle crates as barriers
      var writeRow = rows - 1
      for (readRow in rows - 1 downTo 0) {
        val cell = mutable[readRow][c]
        if (cell.obstacle != ObstacleType.NONE) {
          // Fill any empty cells between readRow+1..writeRow before jumping above the obstacle
          while (writeRow > readRow) {
            val targetCell = mutable[writeRow][c]
            if (targetCell.obstacle == ObstacleType.NONE && targetCell.candy == null) {
              val newCandy = CandyTile(
                id = generateTileId(),
                type = availableCandies[random.nextInt(availableCandies.size)],
                isNewSpawn = true,
              )
              mutable[writeRow][c] = targetCell.copy(candy = newCandy)
            }
            writeRow--
          }
          writeRow = readRow - 1
        } else if (cell.candy != null) {
          if (writeRow != readRow) {
            val targetCell = mutable[writeRow][c]
            mutable[writeRow][c] = targetCell.copy(candy = cell.candy.copy(isNewSpawn = false))
            mutable[readRow][c] = cell.copy(candy = null)
          } else {
            mutable[writeRow][c] = cell.copy(candy = cell.candy.copy(isNewSpawn = false))
          }
          writeRow--
        }
      }

      // Fill remaining empty slots at top of column with fresh candies
      while (writeRow >= 0) {
        val targetCell = mutable[writeRow][c]
        if (targetCell.obstacle == ObstacleType.NONE && targetCell.candy == null) {
          val newCandy = CandyTile(
            id = generateTileId(),
            type = availableCandies[random.nextInt(availableCandies.size)],
            isNewSpawn = true,
          )
          mutable[writeRow][c] = targetCell.copy(candy = newCandy)
        }
        writeRow--
      }
    }

    return mutable.map { it.toList() }
  }

  fun smashSingleCell(board: List<List<BoardCell>>, pos: GridPos): MatchStepResult {
    val cell = board.getOrNull(pos.row)?.getOrNull(pos.col)
      ?: return MatchStepResult(board, 0, 0, 0, 0, emptyList(), null, false)
    val mutable = board.map { it.toMutableList() }.toMutableList()

    var jelly = 0
    var ice = 0
    var crates = 0

    if (cell.obstacle == ObstacleType.WAFFLE_CRATE) {
      crates = 1
      mutable[pos.row][pos.col] = cell.copy(obstacle = ObstacleType.NONE, obstacleHp = 0)
    } else {
      if (cell.iceLayers > 0) ice = 1
      if (cell.hasJelly) jelly = 1
      mutable[pos.row][pos.col] = cell.copy(
        candy = null,
        iceLayers = (cell.iceLayers - 1).coerceAtLeast(0),
        hasJelly = false,
      )
    }

    val burst = ParticleBurst(
      id = nextBurstId++,
      row = pos.row,
      col = pos.col,
      colorHex = 0xFFFF4081,
      textPopup = "BONK! 🍭",
    )

    return MatchStepResult(
      board = mutable.map { it.toList() },
      pointsEarned = 250,
      jellyCleared = jelly,
      iceCleared = ice,
      cratesCleared = crates,
      bursts = listOf(burst),
      cheerWord = "LOLLIPOP SMASH! 🔨",
      hadMatches = true,
    )
  }

  fun upgradeCellToRainbow(board: List<List<BoardCell>>, pos: GridPos): List<List<BoardCell>> {
    val cell = board.getOrNull(pos.row)?.getOrNull(pos.col) ?: return board
    val candy = cell.candy ?: return board
    val mutable = board.map { it.toMutableList() }.toMutableList()
    mutable[pos.row][pos.col] = cell.copy(
      candy = candy.copy(special = SpecialPower.RAINBOW_STAR)
    )
    return mutable.map { it.toList() }
  }

  fun shuffleBoard(board: List<List<BoardCell>>): List<List<BoardCell>> {
    val candies = mutableListOf<CandyTile>()
    for (row in board) {
      for (cell in row) {
        if (cell.obstacle == ObstacleType.NONE && cell.candy != null) {
          candies.add(cell.candy)
        }
      }
    }
    candies.shuffle(random)
    var index = 0
    return board.map { row ->
      row.map { cell ->
        if (cell.obstacle == ObstacleType.NONE && cell.candy != null) {
          cell.copy(candy = candies[index++])
        } else {
          cell
        }
      }
    }
  }

  fun findBestHintMove(board: List<List<BoardCell>>): Pair<GridPos, GridPos>? {
    val rows = board.size
    val cols = board.firstOrNull()?.size ?: 0
    val directions = listOf(0 to 1, 1 to 0)

    for (r in 0 until rows) {
      for (c in 0 until cols) {
        val posA = GridPos(r, c)
        for ((dr, dc) in directions) {
          val nr = r + dr
          val nc = c + dc
          if (nr in 0 until rows && nc in 0 until cols) {
            val posB = GridPos(nr, nc)
            if (canSwap(board, posA, posB)) {
              if (isRainbowSwap(board, posA, posB)) return posA to posB
              val swapped = swapCandies(board, posA, posB)
              if (hasAnyMatch(swapped)) {
                return posA to posB
              }
            }
          }
        }
      }
    }
    return null
  }
}
