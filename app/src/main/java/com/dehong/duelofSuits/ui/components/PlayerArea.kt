package com.dehong.duelofSuits.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
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
    val transition = rememberInfiniteTransition(label = "turnArrow")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowAlpha"
    )
    Text(
        text = char,
        color = Color.White.copy(alpha = alpha),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
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
                            .offset(x = (cardOffset * 14).dp)
                            .offset { IntOffset(0, -(cardHeight * 0.2f).roundToPx()) }
                            .zIndex(idx.toFloat())
                            .graphicsLayer {
                                rotationZ = -(cardOffset * 4f)
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
                            .offset(y = (cardOffset * 14).dp)
                            .offset { IntOffset(-(cardHeight * 0.2f).roundToPx(), 0) }
                            .zIndex(idx.toFloat())
                            .graphicsLayer {
                                rotationZ = 90f + cardOffset * 4f
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                    )
                }
            }
        }

        AiBadge(
            count = player.hand.size,
            roleColor = roleColor,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        when {
            bubbleText != null -> PassBubble(text = bubbleText, modifier = Modifier.align(Alignment.BottomCenter))
            isActive -> TurnArrow("◀", modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
