package com.dehong.duelofSuits.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.zIndex
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.CardSelectionState
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Player
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.theme.ActionGreen
import com.dehong.duelofSuits.ui.theme.CounterBackground
import com.dehong.duelofSuits.ui.theme.TextOnDark

private val CARD_OVERLAP = (-14).dp

private val SUIT_ORDER = mapOf(
    Suit.DIAMONDS to 0,
    Suit.CLUBS to 1,
    Suit.HEARTS to 2,
    Suit.SPADES to 3
)

private fun sortedHand(hand: List<Card>): List<Card> = hand.sortedWith(
    compareBy(
        { card ->
            when (card) {
                is Card.SuitedCard -> SUIT_ORDER[card.suit] ?: 4
                is Card.Joker -> 4
            }
        },
        { card ->
            when (card) {
                is Card.SuitedCard -> card.rank.ordinal
                is Card.Joker -> card.id
            }
        }
    )
)

@Composable
fun PlayerHand(
    player: Player,
    state: GameState,
    registry: PositionRegistry,
    onCardTapped: (Card) -> Unit,
    getSelectionState: (Card) -> CardSelectionState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HandCountBadge(
            player = player,
            state = state,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        val hand = sortedHand(player.hand)

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CARD_OVERLAP),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                hand.forEachIndexed { idx, card ->
                    val selState = getSelectionState(card)
                    val yOffset = if (selState == CardSelectionState.SELECTED) (-8).dp else 0.dp
                    CardView(
                        card = card,
                        selectionState = selState,
                        modifier = Modifier
                            .zIndex(idx.toFloat())
                            .offset { IntOffset(0, yOffset.roundToPx()) }
                            .onGloballyPositioned { coords ->
                                registry.register(PositionKey.HandCard(player.id, idx), coords)
                            }
                            .clickable(enabled = selState != CardSelectionState.DISABLED) {
                                onCardTapped(card)
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun HandCountBadge(
    player: Player,
    state: GameState,
    modifier: Modifier = Modifier
) {
    val isDefender = state.defenderIndex == player.id
    val isAttacker = state.attackerIndex == player.id

    val roleLabel = when {
        isAttacker -> "ATTACKER"
        isDefender -> "DEFENDER"
        else -> "SUPPORT"
    }
    val roleColor by animateColorAsState(
        targetValue = when {
            isAttacker -> Color(0xFFFF8F00)
            isDefender -> Color(0xFFB71C1C)
            else -> ActionGreen
        },
        animationSpec = tween(300),
        label = "roleColor"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(roleColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = roleLabel,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
        Box(
            modifier = Modifier
                .background(CounterBackground, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${player.name}: ${player.hand.size} cards",
                color = TextOnDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
