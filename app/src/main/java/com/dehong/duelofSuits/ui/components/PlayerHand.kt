package com.dehong.duelofSuits.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.roundToInt

private fun suitOrder(trump: Suit): Map<Suit, Int> = when (trump) {
    Suit.SPADES   -> mapOf(Suit.DIAMONDS to 0, Suit.CLUBS to 1, Suit.HEARTS to 2, Suit.SPADES to 3)
    Suit.HEARTS   -> mapOf(Suit.CLUBS to 0, Suit.DIAMONDS to 1, Suit.SPADES to 2, Suit.HEARTS to 3)
    Suit.DIAMONDS -> mapOf(Suit.CLUBS to 0, Suit.HEARTS to 1, Suit.SPADES to 2, Suit.DIAMONDS to 3)
    Suit.CLUBS    -> mapOf(Suit.DIAMONDS to 0, Suit.SPADES to 1, Suit.HEARTS to 2, Suit.CLUBS to 3)
}

private fun sortedHand(hand: List<Card>, trump: Suit): List<Card> = hand.sortedWith(
    compareBy(
        { card ->
            when (card) {
                is Card.SuitedCard -> suitOrder(trump)[card.suit] ?: 4
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
    val hand = sortedHand(player.hand, state.trumpSuit)
    val n = hand.size
    val density = LocalDensity.current
    val cardWidth  = LocalCardWidth.current
    val cardHeight = LocalCardHeight.current

    // Spread factor: 0 = all cards collapsed to center, 1 = full fan
    val spreadFactor = remember { Animatable(0f) }
    val prevHandSize = remember { mutableIntStateOf(hand.size) }

    LaunchedEffect(hand.size, state.animating) {
        val prev = prevHandSize.intValue
        prevHandSize.intValue = hand.size
        when {
            hand.size == 0 -> spreadFactor.snapTo(0f)
            hand.size > prev && state.animating -> {
                // Cards arriving mid-deal — stay collapsed so flying cards land seamlessly
                spreadFactor.snapTo(0f)
            }
            hand.size > prev && !state.animating -> {
                // Card added with no animation in flight — fan out immediately
                spreadFactor.snapTo(0f)
                spreadFactor.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            }
            !state.animating && spreadFactor.value < 0.01f && hand.size > 0 -> {
                // Deal just finished — fan out now
                spreadFactor.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val availableWidthPx = constraints.maxWidth.toFloat()
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val desiredStepPx = with(density) { 38.dp.toPx() }
        val step = if (n <= 1) 0f else minOf(desiredStepPx, (availableWidthPx - cardWidthPx) / (n - 1))
        val totalWidthPx = if (n <= 1) cardWidthPx else cardWidthPx + (n - 1) * step
        val startX = (availableWidthPx - totalWidthPx) / 2f
        // X position when all cards are collapsed to screen center
        val centerX = (availableWidthPx - cardWidthPx) / 2f
        val spread = spreadFactor.value

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .offset { IntOffset(0, (cardHeight * 0.2f).roundToPx()) }
                    .onGloballyPositioned { coords ->
                        registry.register(PositionKey.PlayerArea(player.id), coords)
                    }
            ) {
                hand.forEachIndexed { idx, card ->
                    // During deal/fan-out, keep cards fully visible regardless of game phase
                    val selState = if (spread < 1f) CardSelectionState.NORMAL else getSelectionState(card)
                    val finalX = startX + idx * step
                    // Interpolate from center to final fan position
                    val xPx = (centerX + (finalX - centerX) * spread).roundToInt()
                    val yPx = if (selState == CardSelectionState.SELECTED) {
                        with(density) { (-8).dp.roundToPx() }
                    } else 0

                    CardView(
                        card = card,
                        selectionState = selState,
                        modifier = Modifier
                            .offset { IntOffset(xPx, yPx) }
                            .zIndex(idx.toFloat())
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

        // Invisible anchor at the top-left of the center card when spreadFactor=0:
        // x offset = -cardWidth/2 shifts from boxCenter to card top-left (centerX = (boxWidth-cardWidth)/2)
        // y offset = -cardHeight*0.8f shifts from panel bottom to card top
        Box(modifier = Modifier
            .offset(x = -(cardWidth / 2), y = -(cardHeight * 0.8f))
            .onGloballyPositioned { registry.register(PositionKey.HumanHandCenter, it) }
        )
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
