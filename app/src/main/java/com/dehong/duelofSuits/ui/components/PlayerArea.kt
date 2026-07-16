package com.dehong.duelofSuits.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Player
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.theme.ActionGreen
import com.dehong.duelofSuits.ui.theme.CounterBackground
import com.dehong.duelofSuits.ui.theme.TextOnDark

@Composable
fun AiPlayerArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
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

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        val faceDownCards = minOf(player.hand.size, 8)

        // Card fan at the very top — top 20% hangs off the screen edge.
        // Cards rotate around their bottom-center so the fan arcs naturally.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CARD_HEIGHT * 0.8f)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Registration anchor always present regardless of hand size
            Box(
                modifier = Modifier
                    .size(CARD_WIDTH, CARD_HEIGHT)
                    .offset { IntOffset(0, -(CARD_HEIGHT * 0.2f).roundToPx()) }
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
                            .offset { IntOffset(0, -(CARD_HEIGHT * 0.2f).roundToPx()) }
                            .zIndex(idx.toFloat())
                            .graphicsLayer {
                                // All cards share the same top-center pivot (the hidden grip).
                                // No x-spread: every card's top is at the same point, so the
                                // bottoms fan outward naturally like a hand held from above.
                                rotationZ = cardOffset * 8f
                                transformOrigin = TransformOrigin(0.5f, 0.0f)
                            }
                            .size(CARD_WIDTH, CARD_HEIGHT)
                    )
                }
            }
        }

        // Labels below the card fan
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .background(roleColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = player.name,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .background(CounterBackground, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${player.hand.size}",
                color = TextOnDark,
                fontSize = 10.sp
            )
        }

        if (state.phase != GamePhase.GAME_OVER) {
            val roleLabel = when {
                isAttacker -> "ATK"
                isDefender -> "DEF"
                else -> ""
            }
            if (roleLabel.isNotEmpty()) {
                Text(
                    text = roleLabel,
                    color = roleColor.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// Vertical AI area for the left-side player in 4-player mode.
@Composable
fun AiSideArea(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
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

    // Column width = 80% of card's visual landscape width (CARD_HEIGHT) + label space
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(CARD_HEIGHT * 0.8f + 20.dp)
            .padding(end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .background(roleColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = player.name,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .background(CounterBackground, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${player.hand.size}",
                color = TextOnDark,
                fontSize = 10.sp
            )
        }

        val faceDownCards = minOf(player.hand.size, 8)
        // Cards are rotated 90° (landscape) and fanned vertically.
        // translationX inside graphicsLayer shifts each card left so 20% of its
        // landscape visual width (CARD_HEIGHT) hangs off the left screen edge.
        Box(
            modifier = Modifier.padding(top = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            // Registration anchor always present
            Box(
                modifier = Modifier
                    .size(CARD_WIDTH, CARD_HEIGHT)
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
                            // push 20% of the landscape visual width (CARD_HEIGHT) off left edge
                            .offset { IntOffset(-(CARD_HEIGHT * 0.2f).roundToPx(), 0) }
                            .zIndex(idx.toFloat())
                            .graphicsLayer {
                                // 90° makes each card landscape; extra angle fans them vertically
                                rotationZ = 90f + cardOffset * 4f
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                            .size(CARD_WIDTH, CARD_HEIGHT)
                    )
                }
            }
        }

        if (state.phase != GamePhase.GAME_OVER) {
            val roleLabel = when {
                isAttacker -> "ATK"
                isDefender -> "DEF"
                else -> ""
            }
            if (roleLabel.isNotEmpty()) {
                Text(
                    text = roleLabel,
                    color = roleColor.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
