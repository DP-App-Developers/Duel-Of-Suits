package com.dehong.duelofSuits.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
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
import com.dehong.duelofSuits.ui.theme.Gold

@Composable
private fun AiBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Gold.copy(alpha = 0.55f),
                spotColor = Gold.copy(alpha = 0.80f)
            )
            .size(34.dp)
            .drawWithContent {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerR = size.minDimension / 2f
                val ringW = 3.2f.dp.toPx()
                val innerR = outerR - ringW

                // Metallic gold ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFFFFE566), Color(0xFFB8860B),
                            Color(0xFFFFD700), Color(0xFFE8C000),
                            Color(0xFF9A6E00), Color(0xFFFFE566)
                        ),
                        center = Offset(cx, cy)
                    ),
                    radius = outerR
                )
                // Deep navy inner disc
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1C2E58), Color(0xFF060C1A)),
                        center = Offset(cx, cy),
                        radius = innerR
                    ),
                    radius = innerR
                )
                // Specular highlight — top-left shimmer
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(cx * 0.58f, cy * 0.52f),
                        radius = innerR * 0.62f
                    ),
                    radius = innerR
                )
                drawContent()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            color = Color(0xFFFFF4CC),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
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
internal fun RolePill(text: String, color: Color, modifier: Modifier = Modifier) {
    val pillShape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), pillShape)
            .border(0.8.dp, color.copy(alpha = 0.50f), pillShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun AiPlayerArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
    bubbleText: String? = null,
    modifier: Modifier = Modifier
) {
    val isDefender = state.defenderIndex == player.id
    val isAttacker = state.attackerIndex == player.id
    val roleLabel = when {
        isAttacker -> "Attacker"
        isDefender -> "Defender"
        else -> null
    }
    val roleLabelColor = if (isAttacker) Color(0xFFFF8F00) else Color(0xFFB71C1C)

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
                .height(cardHeight),
            contentAlignment = Alignment.TopCenter
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
                            .offset(x = (cardOffset * 14 * spread).dp)
                            .zIndex(idx.toFloat())
                            .graphicsLayer {
                                rotationZ = -(cardOffset * 6f * spread)
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                    )
                }
            }

            AiBadge(
                count = player.hand.size,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
            )
        }
        if (bubbleText != null) {
            PassBubble(text = bubbleText)
        } else if (roleLabel != null) {
            RolePill(text = roleLabel, color = roleLabelColor)
        }
    }
}

// Top AI area — portrait cards flipped 180° (player looking down), fanned left-right.
@Composable
fun AiTopArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
    bubbleText: String? = null,
    modifier: Modifier = Modifier
) {
    val isDefender = state.defenderIndex == player.id
    val isAttacker = state.attackerIndex == player.id
    val roleLabel = when {
        isAttacker -> "Attacker"
        isDefender -> "Defender"
        else -> null
    }
    val roleLabelColor = if (isAttacker) Color(0xFFFF8F00) else Color(0xFFB71C1C)

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
                .height(cardHeight),
            contentAlignment = Alignment.TopCenter
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
                            .offset(x = (cardOffset * 14 * spread).dp)
                            .zIndex(idx.toFloat())
                            .graphicsLayer {
                                rotationZ = 180f - cardOffset * 6f * spread
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                    )
                }
            }

            AiBadge(
                count = player.hand.size,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
            )
        }
        if (bubbleText != null) {
            PassBubble(text = bubbleText)
        } else if (roleLabel != null) {
            RolePill(text = roleLabel, color = roleLabelColor)
        }
    }
}

// Vertical AI area for the left-side player in 4-player mode.
@Composable
fun AiSideArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
    bubbleText: String? = null,
    modifier: Modifier = Modifier
) {
    val isDefender = state.defenderIndex == player.id
    val isAttacker = state.attackerIndex == player.id
    val roleLabel = when {
        isAttacker -> "Attacker"
        isDefender -> "Defender"
        else -> null
    }
    val roleLabelColor = if (isAttacker) Color(0xFFFF8F00) else Color(0xFFB71C1C)

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
                            .zIndex(idx.toFloat())
                            .graphicsLayer {
                                rotationZ = 90f + cardOffset * 6f * spread
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
            AiBadge(count = player.hand.size)
            if (bubbleText != null) {
                PassBubble(text = bubbleText)
            } else if (roleLabel != null) {
                RolePill(
                    text = roleLabel,
                    color = roleLabelColor,
                    modifier = Modifier.rotate(90f)
                )
            }
        }
    }
}
