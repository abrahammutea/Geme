package com.candysplash.quest.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.candysplash.quest.game.BoardCell
import com.candysplash.quest.game.CandyTile
import com.candysplash.quest.game.CandyType
import com.candysplash.quest.game.GridPos
import com.candysplash.quest.game.ObstacleType
import com.candysplash.quest.game.ParticleBurst
import com.candysplash.quest.game.SpecialPower
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CandyBoardCanvas(
  board: List<List<BoardCell>>,
  selectedCell: GridPos?,
  hintMove: Pair<GridPos, GridPos>?,
  activeBursts: List<ParticleBurst>,
  onCellTapped: (GridPos) -> Unit,
  onCellSwiped: (GridPos, GridPos) -> Unit,
  modifier: Modifier = Modifier,
) {
  val rows = board.size.coerceAtLeast(1)
  val cols = (board.firstOrNull()?.size ?: 1).coerceAtLeast(1)

  val infiniteTransition = rememberInfiniteTransition(label = "candyPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.07f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulseScale",
  )

  val sparkleAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4000),
      repeatMode = RepeatMode.Restart,
    ),
    label = "sparkleAngle",
  )

  BoxWithConstraints(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    val density = LocalDensity.current
    val maxW = with(density) { maxWidth.toPx() }
    val maxH = with(density) { maxHeight.toPx() }

    val cellSizePx = min(maxW / cols, maxH / rows)
    val boardWidthDp = with(density) { (cellSizePx * cols).toDp() }
    val boardHeightDp = with(density) { (cellSizePx * rows).toDp() }

    var dragStartCell by remember { mutableStateOf<GridPos?>(null) }
    var dragAccumulated by remember { mutableStateOf(Offset.Zero) }
    var swipeTriggered by remember { mutableStateOf(false) }

    Canvas(
      modifier = Modifier
        .size(boardWidthDp, boardHeightDp)
        .pointerInput(rows, cols, cellSizePx) {
          detectTapGestures { offset ->
            val c = (offset.x / cellSizePx).toInt().coerceIn(0, cols - 1)
            val r = (offset.y / cellSizePx).toInt().coerceIn(0, rows - 1)
            onCellTapped(GridPos(r, c))
          }
        }
        .pointerInput(rows, cols, cellSizePx) {
          detectDragGestures(
            onDragStart = { offset ->
              val c = (offset.x / cellSizePx).toInt().coerceIn(0, cols - 1)
              val r = (offset.y / cellSizePx).toInt().coerceIn(0, rows - 1)
              dragStartCell = GridPos(r, c)
              dragAccumulated = Offset.Zero
              swipeTriggered = false
            },
            onDrag = { change, dragAmount ->
              change.consume()
              val start = dragStartCell ?: return@detectDragGestures
              if (swipeTriggered) return@detectDragGestures
              dragAccumulated += dragAmount
              val threshold = cellSizePx * 0.33f
              if (abs(dragAccumulated.x) > threshold || abs(dragAccumulated.y) > threshold) {
                val target = if (abs(dragAccumulated.x) > abs(dragAccumulated.y)) {
                  val dc = if (dragAccumulated.x > 0) 1 else -1
                  GridPos(start.row, (start.col + dc).coerceIn(0, cols - 1))
                } else {
                  val dr = if (dragAccumulated.y > 0) 1 else -1
                  GridPos((start.row + dr).coerceIn(0, rows - 1), start.col)
                }
                if (target != start) {
                  swipeTriggered = true
                  onCellSwiped(start, target)
                }
              }
            },
            onDragEnd = {
              dragStartCell = null
              swipeTriggered = false
            },
          )
        }
    ) {
      // Draw outer candy board frame
      drawRoundRect(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF4C1D95).copy(alpha = 0.85f),
            Color(0xFF2E1065).copy(alpha = 0.92f),
          )
        ),
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = CornerRadius(cellSizePx * 0.35f, cellSizePx * 0.35f),
      )

      drawRoundRect(
        color = Color(0xFFF472B6),
        topLeft = Offset(2f, 2f),
        size = Size(size.width - 4f, size.height - 4f),
        cornerRadius = CornerRadius(cellSizePx * 0.35f, cellSizePx * 0.35f),
        style = Stroke(width = 5f),
      )

      for (r in 0 until rows) {
        for (c in 0 until cols) {
          val cell = board.getOrNull(r)?.getOrNull(c) ?: continue
          val topLeft = Offset(c * cellSizePx, r * cellSizePx)
          val center = Offset(topLeft.x + cellSizePx * 0.5f, topLeft.y + cellSizePx * 0.5f)
          val pad = cellSizePx * 0.06f

          // Checkerboard soft pastel cell tile
          val isEven = (r + c) % 2 == 0
          val tileBg = if (isEven) Color(0xFF6D28D9).copy(alpha = 0.55f) else Color(0xFF5B21B6).copy(alpha = 0.55f)
          drawRoundRect(
            color = tileBg,
            topLeft = Offset(topLeft.x + pad, topLeft.y + pad),
            size = Size(cellSizePx - pad * 2, cellSizePx - pad * 2),
            cornerRadius = CornerRadius(cellSizePx * 0.22f, cellSizePx * 0.22f),
          )

          // 1. Golden Jelly target background
          if (cell.hasJelly) {
            drawRoundRect(
              brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF066), Color(0xFFFFAE00)),
                center = center,
                radius = cellSizePx * 0.48f,
              ),
              topLeft = Offset(topLeft.x + pad * 1.4f, topLeft.y + pad * 1.4f),
              size = Size(cellSizePx - pad * 2.8f, cellSizePx - pad * 2.8f),
              cornerRadius = CornerRadius(cellSizePx * 0.24f, cellSizePx * 0.24f),
            )
            drawRoundRect(
              color = Color(0xFFFFF9C4),
              topLeft = Offset(topLeft.x + pad * 1.5f, topLeft.y + pad * 1.5f),
              size = Size(cellSizePx - pad * 3f, cellSizePx - pad * 3f),
              cornerRadius = CornerRadius(cellSizePx * 0.24f, cellSizePx * 0.24f),
              style = Stroke(width = 3.5f),
            )
          }

          // Selection or Hint highlight ring
          val isSelected = selectedCell?.row == r && selectedCell.col == c
          val isHinted = (hintMove?.first?.row == r && hintMove.first.col == c) ||
            (hintMove?.second?.row == r && hintMove.second.col == c)

          if (isSelected || isHinted) {
            val highlightColor = if (isSelected) Color(0xFF00F5D4) else Color(0xFFFFEA00)
            drawRoundRect(
              color = highlightColor,
              topLeft = Offset(topLeft.x + pad * 0.8f, topLeft.y + pad * 0.8f),
              size = Size(cellSizePx - pad * 1.6f, cellSizePx - pad * 1.6f),
              cornerRadius = CornerRadius(cellSizePx * 0.25f, cellSizePx * 0.25f),
              style = Stroke(width = 6f * pulseScale),
            )
          }

          // 2. Waffle Crate Obstacle OR Candy Character
          if (cell.obstacle == ObstacleType.WAFFLE_CRATE) {
            drawWaffleCrate(topLeft, cellSizePx)
          } else if (cell.candy != null) {
            val scale = if (isSelected || isHinted) pulseScale else 1f
            drawCuteCandy(
              candy = cell.candy,
              center = center,
              radius = cellSizePx * 0.37f * scale,
              sparkleAngle = sparkleAngle,
            )
          }

          // 3. Frosty Ice Overlay (drawn on top of candy!)
          if (cell.iceLayers > 0) {
            drawFrostyIceOverlay(topLeft, cellSizePx)
          }
        }
      }

      // 4. Confetti Particle Bursts on matched cells
      for (burst in activeBursts) {
        val cx = (burst.col + 0.5f) * cellSizePx
        val cy = (burst.row + 0.5f) * cellSizePx
        val burstColor = Color(burst.colorHex)
        for (i in 0 until 8) {
          val angle = (i * 45f + sparkleAngle * 2f) * (PI / 180.0)
          val dist = cellSizePx * 0.36f * pulseScale
          val px = cx + (cos(angle) * dist).toFloat()
          val py = cy + (sin(angle) * dist).toFloat()
          drawCircle(
            color = burstColor,
            radius = cellSizePx * 0.08f,
            center = Offset(px, py),
          )
          drawCircle(
            color = Color.White,
            radius = cellSizePx * 0.04f,
            center = Offset(px, py),
          )
        }
      }
    }
  }
}

private fun DrawScope.drawWaffleCrate(topLeft: Offset, cellSize: Float) {
  val pad = cellSize * 0.11f
  val crateLeft = topLeft.x + pad
  val crateTop = topLeft.y + pad
  val crateSize = cellSize - pad * 2

  // Chocolate/Waffle base
  drawRoundRect(
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFFE08E3C), Color(0xFFA85B15)),
      start = Offset(crateLeft, crateTop),
      end = Offset(crateLeft + crateSize, crateTop + crateSize),
    ),
    topLeft = Offset(crateLeft, crateTop),
    size = Size(crateSize, crateSize),
    cornerRadius = CornerRadius(cellSize * 0.18f, cellSize * 0.18f),
  )

  // Waffle grid lines
  val third = crateSize / 3f
  for (i in 1..2) {
    drawLine(
      color = Color(0xFF6E3809),
      start = Offset(crateLeft + third * i, crateTop + 6f),
      end = Offset(crateLeft + third * i, crateTop + crateSize - 6f),
      strokeWidth = 4f,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = Color(0xFF6E3809),
      start = Offset(crateLeft + 6f, crateTop + third * i),
      end = Offset(crateLeft + crateSize - 6f, crateTop + third * i),
      strokeWidth = 4f,
      cap = StrokeCap.Round,
    )
  }

  // Cream border
  drawRoundRect(
    color = Color(0xFFFFDEB4),
    topLeft = Offset(crateLeft, crateTop),
    size = Size(crateSize, crateSize),
    cornerRadius = CornerRadius(cellSize * 0.18f, cellSize * 0.18f),
    style = Stroke(width = 3.5f),
  )
}

private fun DrawScope.drawFrostyIceOverlay(topLeft: Offset, cellSize: Float) {
  val pad = cellSize * 0.08f
  val iceLeft = topLeft.x + pad
  val iceTop = topLeft.y + pad
  val iceSize = cellSize - pad * 2

  drawRoundRect(
    brush = Brush.linearGradient(
      colors = listOf(
        Color(0xFFB3E5FC).copy(alpha = 0.58f),
        Color(0xFF4FC3F7).copy(alpha = 0.45f),
      ),
      start = Offset(iceLeft, iceTop),
      end = Offset(iceLeft + iceSize, iceTop + iceSize),
    ),
    topLeft = Offset(iceLeft, iceTop),
    size = Size(iceSize, iceSize),
    cornerRadius = CornerRadius(cellSize * 0.22f, cellSize * 0.22f),
  )

  // Frost border + crystal crack lines
  drawRoundRect(
    color = Color(0xFFE1F5FE),
    topLeft = Offset(iceLeft, iceTop),
    size = Size(iceSize, iceSize),
    cornerRadius = CornerRadius(cellSize * 0.22f, cellSize * 0.22f),
    style = Stroke(width = 4.5f),
  )

  // Diagonal ice shine
  drawLine(
    color = Color.White.copy(alpha = 0.85f),
    start = Offset(iceLeft + iceSize * 0.2f, iceTop + iceSize * 0.35f),
    end = Offset(iceLeft + iceSize * 0.45f, iceTop + iceSize * 0.18f),
    strokeWidth = 4f,
    cap = StrokeCap.Round,
  )
}

private fun DrawScope.drawCuteCandy(
  candy: CandyTile,
  center: Offset,
  radius: Float,
  sparkleAngle: Float,
) {
  val type = candy.type
  val primary = Color(type.primaryHex)
  val secondary = Color(type.secondaryHex)
  val accent = Color(type.accentHex)

  // Special Rainbow Star Candy override!
  if (candy.special == SpecialPower.RAINBOW_STAR) {
    drawRainbowStarCandy(center, radius, sparkleAngle)
    return
  }

  // Drop shadow
  drawCircle(
    color = Color.Black.copy(alpha = 0.25f),
    radius = radius * 0.96f,
    center = Offset(center.x, center.y + radius * 0.12f),
  )

  val bodyBrush = Brush.radialGradient(
    colors = listOf(secondary, primary),
    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
    radius = radius * 1.25f,
  )

  when (type) {
    CandyType.STRAWBERRY -> {
      // Cute berry rounded heart/circle
      drawCircle(brush = bodyBrush, radius = radius, center = center)
      // Little green leaf crown on top
      drawCircle(
        color = Color(0xFF4ADE80),
        radius = radius * 0.24f,
        center = Offset(center.x - radius * 0.18f, center.y - radius * 0.88f),
      )
      drawCircle(
        color = Color(0xFF22C55E),
        radius = radius * 0.24f,
        center = Offset(center.x + radius * 0.18f, center.y - radius * 0.88f),
      )
    }
    CandyType.LEMON -> {
      // Sunny rounded star/pillow
      drawRoundRect(
        brush = bodyBrush,
        topLeft = Offset(center.x - radius, center.y - radius * 0.88f),
        size = Size(radius * 2f, radius * 1.76f),
        cornerRadius = CornerRadius(radius * 0.85f, radius * 0.85f),
      )
    }
    CandyType.BLUEBERRY -> {
      // Glossy aqua bubble drop
      drawCircle(brush = bodyBrush, radius = radius, center = center)
    }
    CandyType.APPLE -> {
      // Squishy rounded gummy square
      drawRoundRect(
        brush = bodyBrush,
        topLeft = Offset(center.x - radius * 0.92f, center.y - radius * 0.92f),
        size = Size(radius * 1.84f, radius * 1.84f),
        cornerRadius = CornerRadius(radius * 0.48f, radius * 0.48f),
      )
    }
    CandyType.GRAPE -> {
      // Magic diamond/gem cushion
      val path = Path().apply {
        moveTo(center.x, center.y - radius * 1.05f)
        lineTo(center.x + radius * 1.05f, center.y)
        lineTo(center.x, center.y + radius * 1.05f)
        lineTo(center.x - radius * 1.05f, center.y)
        close()
      }
      drawPath(path = path, brush = bodyBrush)
      drawPath(path = path, color = accent, style = Stroke(width = 3f))
    }
    CandyType.TANGERINE -> {
      // Cute Gummy Bear with little ears!
      drawCircle(
        color = primary,
        radius = radius * 0.36f,
        center = Offset(center.x - radius * 0.62f, center.y - radius * 0.62f),
      )
      drawCircle(
        color = primary,
        radius = radius * 0.36f,
        center = Offset(center.x + radius * 0.62f, center.y - radius * 0.62f),
      )
      drawCircle(brush = bodyBrush, radius = radius * 0.95f, center = center)
    }
  }

  // Glossy specular top-left shine
  drawCircle(
    color = Color.White.copy(alpha = 0.65f),
    radius = radius * 0.25f,
    center = Offset(center.x - radius * 0.36f, center.y - radius * 0.36f),
  )
  drawCircle(
    color = Color.White.copy(alpha = 0.45f),
    radius = radius * 0.11f,
    center = Offset(center.x - radius * 0.14f, center.y - radius * 0.54f),
  )

  // Cute Kawaii Eyes & Happy Smile for kids!
  val eyeOffsetX = radius * 0.32f
  val eyeY = center.y - radius * 0.04f
  val eyeRadius = radius * 0.12f

  // Left & Right Eyes
  drawCircle(
    color = Color(0xFF1E1B4B),
    radius = eyeRadius,
    center = Offset(center.x - eyeOffsetX, eyeY),
  )
  drawCircle(
    color = Color(0xFF1E1B4B),
    radius = eyeRadius,
    center = Offset(center.x + eyeOffsetX, eyeY),
  )
  // Eye sparkles
  drawCircle(
    color = Color.White,
    radius = eyeRadius * 0.42f,
    center = Offset(center.x - eyeOffsetX - eyeRadius * 0.25f, eyeY - eyeRadius * 0.25f),
  )
  drawCircle(
    color = Color.White,
    radius = eyeRadius * 0.42f,
    center = Offset(center.x + eyeOffsetX - eyeRadius * 0.25f, eyeY - eyeRadius * 0.25f),
  )

  // Rosy cheeks
  drawCircle(
    color = Color(0xFFFF80AB).copy(alpha = 0.6f),
    radius = radius * 0.11f,
    center = Offset(center.x - radius * 0.48f, eyeY + radius * 0.18f),
  )
  drawCircle(
    color = Color(0xFFFF80AB).copy(alpha = 0.6f),
    radius = radius * 0.11f,
    center = Offset(center.x + radius * 0.48f, eyeY + radius * 0.18f),
  )

  // Cute smile line
  drawLine(
    color = Color(0xFF1E1B4B),
    start = Offset(center.x - radius * 0.14f, eyeY + radius * 0.22f),
    end = Offset(center.x, eyeY + radius * 0.30f),
    strokeWidth = 3f,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = Color(0xFF1E1B4B),
    start = Offset(center.x, eyeY + radius * 0.30f),
    end = Offset(center.x + radius * 0.14f, eyeY + radius * 0.22f),
    strokeWidth = 3f,
    cap = StrokeCap.Round,
  )

  // Draw Special Power-Up Overlays (Striped Rocket or 3x3 Candy Bomb)
  when (candy.special) {
    SpecialPower.STRIPED_HORIZONTAL -> {
      for (dy in listOf(-0.42f, 0f, 0.42f)) {
        drawLine(
          color = Color.White.copy(alpha = 0.9f),
          start = Offset(center.x - radius * 0.78f, center.y + radius * dy),
          end = Offset(center.x + radius * 0.78f, center.y + radius * dy),
          strokeWidth = 5f,
          cap = StrokeCap.Round,
        )
      }
      drawCircle(
        color = Color.White,
        radius = radius * 1.02f,
        center = center,
        style = Stroke(width = 3.5f),
      )
    }
    SpecialPower.STRIPED_VERTICAL -> {
      for (dx in listOf(-0.42f, 0f, 0.42f)) {
        drawLine(
          color = Color.White.copy(alpha = 0.9f),
          start = Offset(center.x + radius * dx, center.y - radius * 0.78f),
          end = Offset(center.x + radius * dx, center.y + radius * 0.78f),
          strokeWidth = 5f,
          cap = StrokeCap.Round,
        )
      }
      drawCircle(
        color = Color.White,
        radius = radius * 1.02f,
        center = center,
        style = Stroke(width = 3.5f),
      )
    }
    SpecialPower.BOMB_3X3 -> {
      // Glowing golden wrapper aura for 3x3 Bomb
      drawCircle(
        color = Color(0xFFFFEA00),
        radius = radius * 1.08f,
        center = center,
        style = Stroke(width = 6f),
      )
      drawCircle(
        color = Color.White,
        radius = radius * 0.92f,
        center = center,
        style = Stroke(width = 3f),
      )
    }
    else -> {}
  }
}

private fun DrawScope.drawRainbowStarCandy(
  center: Offset,
  radius: Float,
  sparkleAngle: Float,
) {
  val rainbowColors = listOf(
    Color(0xFFFF3B6B),
    Color(0xFFFF8600),
    Color(0xFFFFEA00),
    Color(0xFF58CC02),
    Color(0xFF1CB0F6),
    Color(0xFFA560E8),
    Color(0xFFFF3B6B),
  )

  drawCircle(
    brush = Brush.sweepGradient(colors = rainbowColors, center = center),
    radius = radius * 1.05f,
    center = center,
  )

  drawCircle(
    color = Color.White,
    radius = radius * 1.05f,
    center = center,
    style = Stroke(width = 4f),
  )

  // Rotating star rays
  for (i in 0 until 6) {
    val angleRad = ((i * 60f) + sparkleAngle) * (PI / 180.0)
    val endX = center.x + (cos(angleRad) * radius * 0.85f).toFloat()
    val endY = center.y + (sin(angleRad) * radius * 0.85f).toFloat()
    drawLine(
      color = Color.White,
      start = center,
      end = Offset(endX, endY),
      strokeWidth = 5f,
      cap = StrokeCap.Round,
    )
  }

  drawCircle(
    color = Color(0xFFFFF59D),
    radius = radius * 0.38f,
    center = center,
  )
}
