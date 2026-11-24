package com.candysplash.quest.ui.main

import com.candysplash.quest.game.CandyGameEngine
import com.candysplash.quest.game.GAME_LEVELS
import com.candysplash.quest.game.ObstacleType
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import kotlin.random.Random

class MainScreenViewModelTest {
  @Test
  fun createInitialBoard_hasNoPreExistingMatches_andCorrectObstacleCounts() {
    val engine = CandyGameEngine(Random(42))
    val config = GAME_LEVELS[3] // Rainbow Carnival: jelly=8, ice=6, crates=4
    val board = engine.createInitialBoard(config)

    assertEquals(config.rows, board.size)
    assertEquals(config.cols, board.first().size)
    assertFalse("Initial board should not have immediate 3-in-a-row matches", engine.hasAnyMatch(board))

    val jellyCount = board.sumOf { row -> row.count { it.hasJelly } }
    val iceCount = board.sumOf { row -> row.count { it.iceLayers > 0 } }
    val crateCount = board.sumOf { row -> row.count { it.obstacle == ObstacleType.WAFFLE_CRATE } }

    assertEquals(config.targetJelly, jellyCount)
    assertEquals(config.targetIce, iceCount)
    assertEquals(config.targetCrates, crateCount)
  }

  @Test
  fun smashSingleCell_clearsObstacleAndEarnsPoints() {
    val engine = CandyGameEngine(Random(99))
    val config = GAME_LEVELS[0]
    val board = engine.createInitialBoard(config)
    val pos = com.candysplash.quest.game.GridPos(1, 1)

    val result = engine.smashSingleCell(board, pos)
    assertTrue(result.hadMatches)
    assertEquals(250, result.pointsEarned)
  }
}
