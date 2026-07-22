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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dehong.duelofSuits.R
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Player
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.theme.Gold

enum class TailDirection { UP, DOWN, LEFT }

private fun buildBalloonPath(
    w: Float, h: Float,
    r: Float, tw: Float, th: Float,
    dir: TailDirection
): Path = Path().apply {
    val midX = w / 2f
    val midY = h / 2f
    when (dir) {
        TailDirection.UP -> {
            val tLeft  = (midX - tw / 2f).coerceAtLeast(r + 2f)
            val tRight = (midX + tw / 2f).coerceAtMost(w - r - 2f)
            moveTo(midX, 0f)
            lineTo(tRight, th)
            lineTo(w - r, th)
            quadraticBezierTo(w, th, w, th + r)
            lineTo(w, h - r)
            quadraticBezierTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticBezierTo(0f, h, 0f, h - r)
            lineTo(0f, th + r)
            quadraticBezierTo(0f, th, r, th)
            lineTo(tLeft, th)
            close()
        }
        TailDirection.DOWN -> {
            val bodyH  = h - th
            val tLeft  = (midX - tw / 2f).coerceAtLeast(r + 2f)
            val tRight = (midX + tw / 2f).coerceAtMost(w - r - 2f)
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            quadraticBezierTo(w, 0f, w, r)
            lineTo(w, bodyH - r)
            quadraticBezierTo(w, bodyH, w - r, bodyH)
            lineTo(tRight, bodyH)
            lineTo(midX, h)
            lineTo(tLeft, bodyH)
            lineTo(r, bodyH)
            quadraticBezierTo(0f, bodyH, 0f, bodyH - r)
            lineTo(0f, r)
            quadraticBezierTo(0f, 0f, r, 0f)
            close()
        }
        TailDirection.LEFT -> {
            val tTop = (midY - tw / 2f).coerceAtLeast(r + 2f)
            val tBot = (midY + tw / 2f).coerceAtMost(h - r - 2f)
            moveTo(0f, midY)
            lineTo(th, tTop)
            lineTo(th, r)
            quadraticBezierTo(th, 0f, th + r, 0f)
            lineTo(w - r, 0f)
            quadraticBezierTo(w, 0f, w, r)
            lineTo(w, h - r)
            quadraticBezierTo(w, h, w - r, h)
            lineTo(th + r, h)
            quadraticBezierTo(th, h, th, h - r)
            lineTo(th, tBot)
            close()
        }
    }
}

@Composable
fun SpeechBubble(
    text: String,
    tailDirection: TailDirection = TailDirection.DOWN,
    modifier: Modifier = Modifier
) {
    val tailH       = 12.dp
    val tailW       = 15.dp
    val cornerR     = 10.dp
    val hPad        = 11.dp
    val vPad        = 7.dp
    val fillColor   = Color(0xFFFFFEF5)
    val strokeColor = Color(0xFF0D0D1A)
    val shadowColor = Color(0x44000000)

    val topPad    = if (tailDirection == TailDirection.UP)   tailH + vPad else vPad
    val bottomPad = if (tailDirection == TailDirection.DOWN) tailH + vPad else vPad
    val startPad  = if (tailDirection == TailDirection.LEFT) tailH + hPad else hPad

    Box(
        modifier = modifier
            .drawBehind {
                val tw = tailW.toPx()
                val th = tailH.toPx()
                val r  = cornerR.toPx()
                val sw = 2.dp.toPx()

                val mainPath = buildBalloonPath(size.width, size.height, r, tw, th, tailDirection)

                // Soft drop shadow
                withTransform({ translate(2f, 3f) }) {
                    drawPath(mainPath, shadowColor)
                }

                // Cream fill
                drawPath(mainPath, fillColor)

                // Comic ink border
                drawPath(mainPath, strokeColor, style = Stroke(width = sw, join = StrokeJoin.Round))
            }
            .padding(start = startPad, end = hPad, top = topPad, bottom = bottomPad),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = strokeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}

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
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1C2E58), Color(0xFF060C1A)),
                        center = Offset(cx, cy),
                        radius = innerR
                    ),
                    radius = innerR
                )
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
            letterSpacing = 1.2.sp,
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        )
    }
}

@Composable
fun AiPlayerArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
    showUI: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDefender = state.defenderIndex == player.id
    val isAttacker = state.attackerIndex == player.id
    val roleLabel = when {
        isAttacker -> stringResource(R.string.role_attacker)
        isDefender -> stringResource(R.string.role_defender)
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
            player.hand.size > prev && spreadFactor.value < 0.01f -> {
                spreadFactor.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            }
            player.hand.size > prev -> spreadFactor.snapTo(1f)
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(10f)
                    .onGloballyPositioned { registry.register(PositionKey.BubbleAnchor(player.id), it) }
            ) {
                if (showUI) AiBadge(count = player.hand.size)
            }
        }
        if (showUI && roleLabel != null) {
            Box(modifier = Modifier.onGloballyPositioned {
                registry.register(PositionKey.BubbleAnchor(player.id), it)
            }) {
                RolePill(text = roleLabel, color = roleLabelColor)
            }
        }
    }
}

@Composable
fun AiTopArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
    showUI: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDefender = state.defenderIndex == player.id
    val isAttacker = state.attackerIndex == player.id
    val roleLabel = when {
        isAttacker -> stringResource(R.string.role_attacker)
        isDefender -> stringResource(R.string.role_defender)
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
            player.hand.size > prev && spreadFactor.value < 0.01f -> {
                spreadFactor.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            }
            player.hand.size > prev -> spreadFactor.snapTo(1f)
        }
    }
    val spread = spreadFactor.value
    val cardWidth  = LocalCardWidth.current
    val cardHeight = LocalCardHeight.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp)
            .clipToBounds(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val faceDownCards = minOf(player.hand.size, 8)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .offset(y = -(cardHeight * 0.2f)),
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(10f)
                    .onGloballyPositioned { registry.register(PositionKey.BubbleAnchor(player.id), it) }
            ) {
                if (showUI) AiBadge(count = player.hand.size)
            }
        }
        if (showUI && roleLabel != null) {
            Box(modifier = Modifier.onGloballyPositioned {
                registry.register(PositionKey.BubbleAnchor(player.id), it)
            }) {
                RolePill(text = roleLabel, color = roleLabelColor)
            }
        }
    }
}

@Composable
fun AiSideArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
    showUI: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDefender = state.defenderIndex == player.id
    val isAttacker = state.attackerIndex == player.id
    val roleLabel = when {
        isAttacker -> stringResource(R.string.role_attacker)
        isDefender -> stringResource(R.string.role_defender)
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
            player.hand.size > prev && spreadFactor.value < 0.01f -> {
                spreadFactor.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            }
            player.hand.size > prev -> spreadFactor.snapTo(1f)
        }
    }
    val spread = spreadFactor.value

    val cardWidth  = LocalCardWidth.current
    val cardHeight = LocalCardHeight.current
    val faceDownCards = minOf(player.hand.size, 8)

    Row(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(cardHeight * 0.8f + 20.dp),
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
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(10f)
                    .onGloballyPositioned { registry.register(PositionKey.BubbleAnchor(player.id), it) }
            ) {
                if (showUI) AiBadge(count = player.hand.size)
            }
        }

        if (showUI && roleLabel != null) {
            RolePill(text = roleLabel, color = roleLabelColor, modifier = Modifier.rotate(90f))
        }
    }
}
