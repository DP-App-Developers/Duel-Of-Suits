package com.dehong.duelofSuits.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Player
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.theme.ActionGreen
import com.dehong.duelofSuits.ui.theme.Gold

private val BADGE_SHAPE = RoundedCornerShape(20.dp)
private val BADGE_BG = Color(0xEE040C07)

@Composable
private fun AiBadge(
    count: Int,
    roleColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = BADGE_SHAPE)
            .background(BADGE_BG, BADGE_SHAPE)
            .border(0.5.dp, Gold.copy(alpha = 0.22f), BADGE_SHAPE)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(roleColor, CircleShape)
        )
        Text(
            text = "$count",
            color = Color(0xFFF0E6D0),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp
        )
    }
}

@Composable
internal fun PassBubble(text: String = "PASS", modifier: Modifier = Modifier) {
    val bubbleShape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .shadow(4.dp, bubbleShape)
            .background(Color(0xCC0D1B0F), bubbleShape)
            .border(0.5.dp, Color.White.copy(alpha = 0.18f), bubbleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
internal fun TurnArrow(char: String, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "cursor")

    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scale     = 0.88f + pulse * 0.22f   // breathes 0.88 → 1.10
    val glowAlpha = 0.18f + pulse * 0.52f
    val coreAlpha = 0.72f + pulse * 0.28f
    val bouncePx  = with(density) { (pulse * 4f).dp.toPx() }

    // Arrow path points UP by default; rotate to match direction
    val rotation = when (char) { "▼" -> 180f; "◀" -> 270f; "▶" -> 90f; else -> 0f }
    val txPx = when (char) { "◀" -> -bouncePx; "▶" -> bouncePx; else -> 0f }
    val tyPx = when (char) { "▲" -> -bouncePx; "▼" -> bouncePx; else -> 0f }

    val gold1 = Color(0xFFFFF8CC)   // near-white highlight at tip
    val gold2 = Color(0xFFC9A227)   // mid gold
    val gold3 = Color(0xFF7A5100)   // deep amber at base

    Canvas(
        modifier = modifier
            .size(30.dp, 36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                translationX = txPx
                translationY = tyPx
            }
    ) {
        val w = size.width
        val h = size.height

        // Game-style chevron: pointed tip + swept wings + notched stem
        val path = Path().apply {
            moveTo(w * 0.50f, 0f        )   // tip
            lineTo(w * 1.00f, h * 0.54f)   // right wing
            lineTo(w * 0.64f, h * 0.46f)   // right notch
            lineTo(w * 0.64f, h * 1.00f)   // right base
            lineTo(w * 0.36f, h * 1.00f)   // left base
            lineTo(w * 0.36f, h * 0.46f)   // left notch
            lineTo(w * 0.00f, h * 0.54f)   // left wing
            close()
        }

        // Soft glow halos radiating outward from the arrow
        for (ring in 4 downTo 1) {
            val ex = (ring * 5).dp.toPx()
            drawOval(
                color = gold2.copy(alpha = glowAlpha * ring * 0.055f),
                topLeft = Offset(-ex * 0.5f, -ex * 0.5f),
                size = Size(w + ex, h + ex)
            )
        }

        // Main fill — warm gold gradient from tip to base
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(gold1, gold2, gold3),
                startY = 0f,
                endY = h
            ),
            alpha = coreAlpha
        )

        // Crisp dark outline for definition against any background
        drawPath(
            path = path,
            color = gold3.copy(alpha = coreAlpha * 0.85f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Left-face specular highlight — catches light at the tip
        drawLine(
            color = Color.White.copy(alpha = coreAlpha * 0.55f),
            start = Offset(w * 0.50f, 2.5.dp.toPx()),
            end   = Offset(w * 0.19f, h * 0.50f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

@Composable
fun AiPlayerArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
    isActive: Boolean = false,
    bubbleText: String? = null,
    modifier: Modifier = Modifier
) {
    val isDefender = state.defenderIndex == player.id
    val isAttacker = state.attackerIndex == player.id

    val roleColor by animateColorAsState(
        targetValue = when {
            isAttacker -> Color(0xFFFF8F00)
            isDefender -> Color(0xFFB71C1C)
            else -> ActionGreen
        },
        animationSpec = tween(300),
        label = "aiRoleColor"
    )

    val spreadFactor = remember { Animatable(if (player.hand.size > 0) 1f else 0f) }
    val prevHandSize = remember { mutableIntStateOf(player.hand.size) }
    LaunchedEffect(player.hand.size) {
        val prev = prevHandSize.intValue
        prevHandSize.intValue = player.hand.size
        when {
            player.hand.size == 0 -> spreadFactor.snapTo(0f)
            player.hand.size > prev -> {
                spreadFactor.snapTo(0f)
                spreadFactor.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            }
        }
    }
    val spread = spreadFactor.value

    val cardWidth  = LocalCardWidth.current
    val cardHeight = LocalCardHeight.current
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val faceDownCards = minOf(player.hand.size, 8)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight * 0.8f),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(cardWidth, cardHeight)
                    .offset { IntOffset(0, -(cardHeight * 0.2f).roundToPx()) }
                    .onGloballyPositioned { coords ->
                        registry.register(PositionKey.PlayerArea(player.id), coords)
                    }
            )
            if (faceDownCards > 0) {
                val centerIdx = (faceDownCards - 1) / 2f
                repeat(faceDownCards) { idx ->
                    val cardOffset = idx - centerIdx
                    CardView(
                        card = Card.SuitedCard(Suit.SPADES, Rank.ACE),
                        faceDown = true,
                        modifier = Modifier
                            .offset(x = (cardOffset * 14 * spread).dp)
                            .offset { IntOffset(0, -(cardHeight * 0.2f).roundToPx()) }
                            .zIndex(idx.toFloat())
                            .graphicsLayer {
                                rotationZ = -(cardOffset * 4f * spread)
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                    )
                }
            }

            AiBadge(
                count = player.hand.size,
                roleColor = roleColor,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
            )
        }
        when {
            bubbleText != null -> PassBubble(text = bubbleText)
            isActive -> TurnArrow("▲")
        }
    }
}

// Vertical AI area for the left-side player in 4-player mode.
@Composable
fun AiSideArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
    isActive: Boolean = false,
    bubbleText: String? = null,
    modifier: Modifier = Modifier
) {
    val isDefender = state.defenderIndex == player.id
    val isAttacker = state.attackerIndex == player.id

    val roleColor by animateColorAsState(
        targetValue = when {
            isAttacker -> Color(0xFFFF8F00)
            isDefender -> Color(0xFFB71C1C)
            else -> ActionGreen
        },
        animationSpec = tween(300),
        label = "aiSideRoleColor"
    )

    val spreadFactor = remember { Animatable(if (player.hand.size > 0) 1f else 0f) }
    val prevHandSize = remember { mutableIntStateOf(player.hand.size) }
    LaunchedEffect(player.hand.size) {
        val prev = prevHandSize.intValue
        prevHandSize.intValue = player.hand.size
        when {
            player.hand.size == 0 -> spreadFactor.snapTo(0f)
            player.hand.size > prev -> {
                spreadFactor.snapTo(0f)
                spreadFactor.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            }
        }
    }
    val spread = spreadFactor.value

    val cardWidth  = LocalCardWidth.current
    val cardHeight = LocalCardHeight.current
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(cardHeight * 0.8f + 20.dp)
            .padding(end = 4.dp)
    ) {
        val faceDownCards = minOf(player.hand.size, 8)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(cardWidth, cardHeight)
                    .onGloballyPositioned { coords ->
                        registry.register(PositionKey.PlayerArea(player.id), coords)
                    }
            )
            if (faceDownCards > 0) {
                val centerIdx = (faceDownCards - 1) / 2f
                repeat(faceDownCards) { idx ->
                    val cardOffset = idx - centerIdx
                    CardView(
                        card = Card.SuitedCard(Suit.SPADES, Rank.ACE),
                        faceDown = true,
                        modifier = Modifier
                            .offset(y = (cardOffset * 14 * spread).dp)
                            .offset { IntOffset(-(cardHeight * 0.2f).roundToPx(), 0) }
                            .zIndex(idx.toFloat())
                            .graphicsLayer {
                                rotationZ = 90f + cardOffset * 4f * spread
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                    )
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AiBadge(count = player.hand.size, roleColor = roleColor)
            when {
                bubbleText != null -> PassBubble(text = bubbleText)
                isActive -> TurnArrow("◀")
            }
        }
    }
}
