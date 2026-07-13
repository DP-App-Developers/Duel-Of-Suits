package com.dehong.duelofSuits.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.ui.animation.AnimationEvent
import com.dehong.duelofSuits.ui.animation.FlyingCard
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.components.AiPlayerArea
import com.dehong.duelofSuits.ui.components.CardView
import com.dehong.duelofSuits.ui.components.CARD_HEIGHT
import com.dehong.duelofSuits.ui.components.CARD_WIDTH
import com.dehong.duelofSuits.ui.components.DrawDiscardPiles
import com.dehong.duelofSuits.ui.components.GameInfoOverlay
import com.dehong.duelofSuits.ui.components.GameTable
import com.dehong.duelofSuits.ui.components.PlayerHand
import com.dehong.duelofSuits.ui.components.TrumpIndicator
import com.dehong.duelofSuits.ui.theme.ActionGreen
import com.dehong.duelofSuits.ui.theme.DangerRed
import com.dehong.duelofSuits.ui.theme.TableGreen
import com.dehong.duelofSuits.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val LocalPositionRegistry = staticCompositionLocalOf { PositionRegistry() }

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.gameState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val registry = remember { PositionRegistry() }
    val flyingCards = remember { mutableStateListOf<FlyingCard>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.animationEvents.collect { event ->
            handleAnimationEvent(event, registry, flyingCards, scope)
        }
    }

    CompositionLocalProvider(LocalPositionRegistry provides registry) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TableGreen)
                .navigationBarsPadding()
        ) {
            when (state.phase) {
                GamePhase.GAME_OVER -> GameOverOverlay(state = state, onRestart = viewModel::restartGame)
                else -> GameLayout(
                    state = state,
                    registry = registry,
                    viewModel = viewModel
                )
            }

            FlyingCardLayer(flyingCards = flyingCards)

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .zIndex(20f)
            )
        }
    }
}

@Composable
private fun GameLayout(
    state: GameState,
    registry: PositionRegistry,
    viewModel: GameViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.52f),
            verticalAlignment = Alignment.Top
        ) {
            AiPlayerArea(
                player = state.players[1],
                state = state,
                registry = registry,
                modifier = Modifier
                    .weight(0.22f)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DrawDiscardPiles(
                        drawPileCount = state.drawPileCount,
                        discardTopCard = state.discardPile.lastOrNull(),
                        registry = registry
                    )
                    TrumpIndicator()
                }

                GameTable(
                    state = state,
                    registry = registry,
                    onDefenseSlotTapped = viewModel::onDefenseSlotTapped
                )
            }

            AiPlayerArea(
                player = state.players[2],
                state = state,
                registry = registry,
                modifier = Modifier
                    .weight(0.22f)
                    .fillMaxHeight()
            )
        }

        GameInfoOverlay(
            state = state,
            onPlaySelected = viewModel::onPlaySelectedPressed,
            onPass = viewModel::onPassPressed,
            onConfirmDefense = viewModel::onConfirmDefensePressed,
            onTakeCards = viewModel::onTakeCardsPressed,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        PlayerHand(
            player = state.players[0],
            state = state,
            registry = registry,
            onCardTapped = viewModel::onHumanCardTapped,
            getSelectionState = { card -> viewModel.getCardSelectionState(card, state) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.38f)
                .padding(bottom = 6.dp)
        )
    }
}

@Composable
private fun FlyingCardLayer(flyingCards: List<FlyingCard>) {
    flyingCards.forEach { flying ->
        val offset = flying.animatable.value
        CardView(
            card = flying.card,
            faceDown = flying.faceDown,
            modifier = Modifier
                .zIndex(10f)
                .offset {
                    IntOffset(
                        offset.x.roundToInt(),
                        offset.y.roundToInt()
                    )
                }
                .size(CARD_WIDTH, CARD_HEIGHT)
        )
    }
}

private fun handleAnimationEvent(
    event: AnimationEvent,
    registry: PositionRegistry,
    flyingCards: MutableList<FlyingCard>,
    scope: kotlinx.coroutines.CoroutineScope
) {
    when (event) {
        is AnimationEvent.DealCard -> {
            scope.launch {
                delay(event.delayMs)
                val startOffset = registry.getOffset(PositionKey.DrawPile)
                val endOffset = registry.getOffset(PositionKey.PlayerArea(event.targetPlayerId))
                    .takeIf { it != Offset.Zero }
                    ?: startOffset

                animateCard(
                    id = "deal_${event.targetPlayerId}_${event.cardIndex}",
                    card = event.card,
                    faceDown = event.targetPlayerId != 0,
                    from = startOffset,
                    to = endOffset,
                    durationMs = 280,
                    flyingCards = flyingCards
                )
            }
        }

        is AnimationEvent.PlayCardToTable -> {
            scope.launch {
                val startOffset = registry.getOffset(PositionKey.HandCard(event.fromPlayerId, 0))
                    .takeIf { it != Offset.Zero }
                    ?: registry.getOffset(PositionKey.PlayerArea(event.fromPlayerId))
                val endOffset = registry.getOffset(PositionKey.AttackSlot(event.toSlotIndex))
                    .takeIf { it != Offset.Zero }
                    ?: registry.getOffset(PositionKey.DrawPile)

                animateCard(
                    id = "play_${event.card.hashCode()}",
                    card = event.card,
                    faceDown = false,
                    from = startOffset,
                    to = endOffset,
                    durationMs = 250,
                    flyingCards = flyingCards
                )
            }
        }

        is AnimationEvent.DefenseCard -> {
            scope.launch {
                val startOffset = registry.getOffset(PositionKey.HandCard(event.fromPlayerId, 0))
                    .takeIf { it != Offset.Zero }
                    ?: registry.getOffset(PositionKey.PlayerArea(event.fromPlayerId))
                val endOffset = registry.getOffset(PositionKey.DefenseSlot(event.toSlotIndex))
                    .takeIf { it != Offset.Zero }
                    ?: registry.getOffset(PositionKey.DiscardPile)

                animateCard(
                    id = "def_${event.card.hashCode()}",
                    card = event.card,
                    faceDown = false,
                    from = startOffset,
                    to = endOffset,
                    durationMs = 280,
                    flyingCards = flyingCards
                )
            }
        }

        is AnimationEvent.DrawCardFromPile -> {
            scope.launch {
                val start = registry.getOffset(PositionKey.DrawPile)
                val end = registry.getOffset(PositionKey.PlayerArea(event.playerId))
                    .takeIf { it != Offset.Zero } ?: start

                repeat(minOf(event.count, 3)) { i ->
                    delay(i * 100L)
                    scope.launch {
                        animateCard(
                            id = "draw_${event.playerId}_$i",
                            card = Card.SuitedCard(Suit.SPADES, Rank.ACE),
                            faceDown = true,
                            from = start,
                            to = end,
                            durationMs = 220,
                            flyingCards = flyingCards
                        )
                    }
                }
            }
        }

        is AnimationEvent.TableToDiscard -> {
            scope.launch {
                val end = registry.getOffset(PositionKey.DiscardPile)
                event.cards.forEachIndexed { i, card ->
                    val slotIdx = i / 2
                    val start = registry.getOffset(
                        if (i % 2 == 0) PositionKey.AttackSlot(slotIdx)
                        else PositionKey.DefenseSlot(slotIdx)
                    ).takeIf { it != Offset.Zero }
                        ?: registry.getOffset(PositionKey.DrawPile)
                    scope.launch {
                        delay(i * 40L)
                        animateCard(
                            id = "discard_$i",
                            card = card,
                            faceDown = false,
                            from = start,
                            to = end,
                            durationMs = 300,
                            flyingCards = flyingCards
                        )
                    }
                }
            }
        }

        is AnimationEvent.TableToPlayer -> {
            scope.launch {
                val end = registry.getOffset(PositionKey.PlayerArea(event.targetPlayerId))
                event.cards.forEachIndexed { i, card ->
                    val slotIdx = i / 2
                    val start = registry.getOffset(
                        if (i % 2 == 0) PositionKey.AttackSlot(slotIdx)
                        else PositionKey.DefenseSlot(slotIdx)
                    ).takeIf { it != Offset.Zero }
                        ?: registry.getOffset(PositionKey.DrawPile)
                    scope.launch {
                        delay(i * 50L)
                        animateCard(
                            id = "pickup_$i",
                            card = card,
                            faceDown = event.targetPlayerId != 0,
                            from = start,
                            to = end,
                            durationMs = 350,
                            flyingCards = flyingCards
                        )
                    }
                }
            }
        }
    }
}

private suspend fun animateCard(
    id: String,
    card: Card,
    faceDown: Boolean,
    from: Offset,
    to: Offset,
    durationMs: Int,
    flyingCards: MutableList<FlyingCard>
) {
    val animatable = Animatable(from, Offset.VectorConverter)
    val flying = FlyingCard(
        id = id,
        card = card,
        faceDown = faceDown,
        animatable = animatable
    )
    flyingCards.add(flying)
    animatable.animateTo(to, animationSpec = tween(durationMs, easing = FastOutSlowInEasing))
    flyingCards.remove(flying)
}

@Composable
private fun GameOverOverlay(state: GameState, onRestart: () -> Unit) {
    val winner = state.players.firstOrNull { it.id == state.winnerId }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = if (winner?.isHuman == true) "🎉 You Win!" else "${winner?.name ?: "Someone"} Wins!",
                color = if (winner?.isHuman == true) ActionGreen else DangerRed,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (winner?.isHuman == true) "Congratulations!" else "Better luck next time!",
                color = Color.White,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen)
            ) {
                Text("Play Again", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
