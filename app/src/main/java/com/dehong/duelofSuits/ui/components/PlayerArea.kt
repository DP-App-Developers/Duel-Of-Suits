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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val faceDownCards = minOf(player.hand.size, 5)

        // Card fan at the very top — top 20% hangs off the screen edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CARD_HEIGHT * 0.8f)
                .clipToBounds()
        ) {
            if (faceDownCards > 0) {
                Box(
                    modifier = Modifier
                        .size((CARD_WIDTH.value + (faceDownCards - 1) * 6).dp, CARD_HEIGHT)
                        .align(Alignment.TopCenter)
                        .offset { IntOffset(0, -(CARD_HEIGHT * 0.2f).roundToPx()) }
                        .onGloballyPositioned { coords ->
                            registry.register(PositionKey.PlayerArea(player.id), coords)
                        }
                ) {
                    repeat(faceDownCards) { idx ->
                        CardView(
                            card = Card.SuitedCard(Suit.SPADES, Rank.ACE),
                            faceDown = true,
                            modifier = Modifier
                                .offset { IntOffset(idx * 6, 0) }
                                .size(CARD_WIDTH, CARD_HEIGHT)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(CARD_WIDTH, CARD_HEIGHT)
                        .align(Alignment.TopCenter)
                        .offset { IntOffset(0, -(CARD_HEIGHT * 0.2f).roundToPx()) }
                        .onGloballyPositioned { coords ->
                            registry.register(PositionKey.PlayerArea(player.id), coords)
                        }
                )
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

    // Total column width: visible card portion (80%) + space for labels
    val visibleCardWidth = CARD_WIDTH * 0.8f
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(visibleCardWidth + 20.dp)
            .padding(end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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

        val faceDownCards = minOf(player.hand.size, 6)
        // Cards offset left so 20% hangs off the left screen edge (screen clips naturally)
        Box(modifier = Modifier.padding(top = 6.dp)) {
            if (faceDownCards > 0) {
                Box(
                    modifier = Modifier
                        .size(CARD_WIDTH, (CARD_HEIGHT.value + (faceDownCards - 1) * 8).dp)
                        .offset { IntOffset(-(CARD_WIDTH * 0.2f).roundToPx(), 0) }
                        .onGloballyPositioned { coords ->
                            registry.register(PositionKey.PlayerArea(player.id), coords)
                        }
                ) {
                    repeat(faceDownCards) { idx ->
                        CardView(
                            card = Card.SuitedCard(Suit.SPADES, Rank.ACE),
                            faceDown = true,
                            modifier = Modifier
                                .offset { IntOffset(0, idx * 8) }
                                .size(CARD_WIDTH, CARD_HEIGHT)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(CARD_WIDTH, CARD_HEIGHT)
                        .offset { IntOffset(-(CARD_WIDTH * 0.2f).roundToPx(), 0) }
                        .onGloballyPositioned { coords ->
                            registry.register(PositionKey.PlayerArea(player.id), coords)
                        }
                )
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
