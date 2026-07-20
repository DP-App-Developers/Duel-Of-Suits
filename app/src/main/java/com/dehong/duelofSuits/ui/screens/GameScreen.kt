package com.dehong.duelofSuits.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.ui.animation.AnimationEvent
import com.dehong.duelofSuits.ui.animation.FlyingCard
import com.dehong.duelofSuits.ui.animation.LocalFlyingCards
import com.dehong.duelofSuits.ui.animation.LocalTableResizing
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.components.AiSideArea
import com.dehong.duelofSuits.ui.components.AiTopArea
import com.dehong.duelofSuits.ui.components.PassBubble
import com.dehong.duelofSuits.ui.components.RolePill
import com.dehong.duelofSuits.ui.components.CARD_HEIGHT
import com.dehong.duelofSuits.ui.components.CARD_WIDTH
import com.dehong.duelofSuits.ui.components.LocalCardHeight
import com.dehong.duelofSuits.ui.components.LocalCardWidth
import com.dehong.duelofSuits.ui.components.CardView
import com.dehong.duelofSuits.ui.components.DrawPile
import com.dehong.duelofSuits.ui.components.GameInfoOverlay
import com.dehong.duelofSuits.ui.components.GameTable
import com.dehong.duelofSuits.ui.components.PlayerHand
import com.dehong.duelofSuits.ui.theme.DangerRed
import com.dehong.duelofSuits.ui.theme.Gold
import com.dehong.duelofSuits.ui.theme.TableGreen
import com.dehong.duelofSuits.ui.theme.TableGreenLight
import com.dehong.duelofSuits.ui.theme.TextOnDark
import com.dehong.duelofSuits.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val LocalPositionRegistry = staticCompositionLocalOf { PositionRegistry() }


@Composable
fun GameScreen(
    playerCount: Int = 3,
    onNavigateHome: () -> Unit = {},
    viewModel: GameViewModel
) {
    val state by viewModel.gameState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val registry = remember { PositionRegistry() }
    val flyingCards = remember { mutableStateListOf<FlyingCard>() }
    val playerBubbles = remember { mutableStateMapOf<Int, String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearError()
        }
    }

    // When the deal phase ends, the real hand is now populated.
    // Clear any deal cards that were kept at the landing spot so the fan-out takes over.
    LaunchedEffect(state.phase) {
        if (state.phase != GamePhase.DEALING) {
            flyingCards.removeAll { it.id.startsWith("deal_") }
        }
    }

    val cardWidth  = (LocalConfiguration.current.screenWidthDp / 12f).dp
    val cardHeight = cardWidth * (CARD_HEIGHT.value / CARD_WIDTH.value)
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        viewModel.animationEvents.collect { event ->
            handleAnimationEvent(event, registry, flyingCards, scope, density, cardWidth, playerCount,
                viewModel.boardNumCols.value)
            if (event is AnimationEvent.PlayerPassed) {
                playerBubbles[event.playerIdx] = "PASS"
                scope.launch {
                    delay(1200L)
                    playerBubbles.remove(event.playerIdx)
                }
            }
            if (event is AnimationEvent.PlayerTookCards) {
                playerBubbles[event.playerIdx] = "TAKE"
                scope.launch {
                    delay(1500L)
                    playerBubbles.remove(event.playerIdx)
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalPositionRegistry provides registry,
        LocalFlyingCards provides flyingCards.map { it.card }.toSet(),
        LocalCardWidth provides cardWidth,
        LocalCardHeight provides cardHeight,
        LocalTableResizing provides false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(TableGreenLight, TableGreen)))
                .drawBehind {
                    val lineColor = Color.Black.copy(alpha = 0.05f)
                    val spacing = 5.dp.toPx()
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(lineColor, Offset(x, 0f), Offset(x + size.height, size.height), 0.7f)
                        x += spacing
                    }
                    x = 0f
                    while (x < size.width + size.height) {
                        drawLine(lineColor, Offset(x, 0f), Offset(x - size.height, size.height), 0.7f)
                        x += spacing
                    }
                }
        ) {
            when (state.phase) {
                GamePhase.GAME_OVER -> GameOverOverlay(
                    state = state,
                    onRestart = viewModel::restartGame,
                    onHome = onNavigateHome
                )
                else -> {
                    GameLayout(
                        state = state,
                        registry = registry,
                        viewModel = viewModel,
                        passedPlayers = playerBubbles
                    )

                }
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
    viewModel: GameViewModel,
    passedPlayers: Map<Int, String>
) {
    val cardHeight = LocalCardHeight.current
    val tableAlpha = remember { Animatable(1f) }
    LaunchedEffect(state.tableClearing) {
        if (state.tableClearing) {
            tableAlpha.snapTo(1f)
            tableAlpha.animateTo(0f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        } else {
            tableAlpha.snapTo(1f)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // TOP: fixed height fits portrait card + badge label below
        Row(
            modifier = Modifier.fillMaxWidth().height(cardHeight + 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            when (state.playerCount) {
                2 -> AiTopArea(
                    player = state.players[1],
                    state = state,
                    registry = registry,
                    bubbleText = passedPlayers[state.players[1].id],
                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                )
                3 -> {
                    AiTopArea(
                        player = state.players[1],
                        state = state,
                        registry = registry,
                        bubbleText = passedPlayers[state.players[1].id],
                        modifier = Modifier.weight(0.5f).fillMaxHeight()
                    )
                    AiTopArea(
                        player = state.players[2],
                        state = state,
                        registry = registry,
                        bubbleText = passedPlayers[state.players[2].id],
                        modifier = Modifier.weight(0.5f).fillMaxHeight()
                    )
                }
                else -> {
                    AiTopArea(
                        player = state.players[2],
                        state = state,
                        registry = registry,
                        bubbleText = passedPlayers[state.players[2].id],
                        modifier = Modifier.weight(0.5f).fillMaxHeight()
                    )
                    AiTopArea(
                        player = state.players[3],
                        state = state,
                        registry = registry,
                        bubbleText = passedPlayers[state.players[3].id],
                        modifier = Modifier.weight(0.5f).fillMaxHeight()
                    )
                }
            }
        }

        // MIDDLE: [left player (4p only)] | board | draw pile — takes all remaining space
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (state.playerCount == 4) {
                AiSideArea(
                    player = state.players[1],
                    state = state,
                    registry = registry,
                    bubbleText = passedPlayers[state.players[1].id]
                )
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val tableModifier = if (state.tableClearing) Modifier.alpha(tableAlpha.value) else Modifier
                GameTable(
                    state = state,
                    registry = registry,
                    onDefenseSlotTapped = if (state.tableClearing) { _ -> } else viewModel::onDefenseSlotTapped,
                    onNumColsChanged = viewModel::updateBoardNumCols,
                    modifier = tableModifier
                )
            }

            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DrawPile(
                    count = state.drawPileCount,
                    registry = registry,
                    trumpCard = state.trumpCard
                )
            }
        }

        // BOTTOM: fixed height based on card size so it doesn't over-consume vertical space
        Box(modifier = Modifier.fillMaxWidth().height(cardHeight * 1.4f + 16.dp)) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom
            ) {
                Spacer(modifier = Modifier.weight(0.15f))
                PlayerHand(
                    player = state.players[0],
                    state = state,
                    registry = registry,
                    onCardTapped = viewModel::onHumanCardTapped,
                    getSelectionState = { card -> viewModel.getCardSelectionState(card, state) },
                    modifier = Modifier.weight(0.70f).fillMaxHeight()
                )
                GameInfoOverlay(
                    state = state,
                    onPlaySelected = viewModel::onPlaySelectedPressed,
                    onPass = viewModel::onPassPressed,
                    onTakeCards = viewModel::onTakeCardsPressed,
                    modifier = Modifier.weight(0.15f).fillMaxHeight()
                )
            }
            when {
                0 in passedPlayers -> PassBubble(text = passedPlayers[0]!!, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = cardHeight * 0.8f + 12.dp))
                state.attackerIndex == 0 -> RolePill("Attacker", Color(0xFFFF8F00), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = cardHeight * 0.8f + 12.dp))
                state.defenderIndex == 0 -> RolePill("Defender", Color(0xFFB71C1C), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = cardHeight * 0.8f + 12.dp))
            }
        }
    }
}

// ── Shared composables ───────────────────────────────────────────────────────

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
                .graphicsLayer { rotationZ = flying.rotation }
        )
    }
}

// GameTable always renders placeholder Boxes for every slot in the grid which register
// their positions on first composition. So target slots are nearly always in the registry
// before any card animation fires — this fallback is rarely reached.
private fun estimateAttackSlotOffset(
    slotIndex: Int,
    registry: PositionRegistry,
    density: androidx.compose.ui.unit.Density,
    cardWidth: androidx.compose.ui.unit.Dp,
    numCols: Int
): Offset {
    val defenseX   = cardWidth * (20f / 54f)
    val slotStepPx = with(density) { (cardWidth + defenseX + 12.dp).toPx() }

    val slot0 = registry.getOffset(PositionKey.AttackSlot(0))
    if (slot0 != Offset.Zero) {
        val col         = slotIndex % numCols
        val row         = slotIndex / numCols
        val cardHeight  = cardWidth * (78f / 54f)
        val defenseY    = cardWidth * (16f / 54f)
        val rowOffsetPx = with(density) { (cardHeight + defenseY + 8.dp).toPx() }
        return Offset(slot0.x + col * slotStepPx, slot0.y + row * rowOffsetPx)
    }

    return registry.getOffset(PositionKey.DrawPile)
}

private fun handleAnimationEvent(
    event: AnimationEvent,
    registry: PositionRegistry,
    flyingCards: MutableList<FlyingCard>,
    scope: kotlinx.coroutines.CoroutineScope,
    density: androidx.compose.ui.unit.Density,
    cardWidth: androidx.compose.ui.unit.Dp,
    playerCount: Int,
    numCols: Int
) {
    when (event) {
        is AnimationEvent.DealCard -> {
            scope.launch {
                delay(event.delayMs)
                val startOffset = registry.getOffset(PositionKey.DrawPile)
                // Human cards fly to the bottom-center landing zone; AI cards to their area
                val endKey = if (event.targetPlayerId == 0)
                    PositionKey.HumanHandCenter
                else
                    PositionKey.PlayerArea(event.targetPlayerId)
                val endOffset = registry.getOffset(endKey)
                    .takeIf { it != Offset.Zero } ?: startOffset

                // In 4-player mode player 1 sits at the left edge — cards are horizontal
                val dealRotation = if (playerCount == 4 && event.targetPlayerId == 1) 90f else 0f
                animateCard(
                    id = "deal_${event.targetPlayerId}_${event.cardIndex}",
                    card = event.card,
                    faceDown = event.targetPlayerId != 0,
                    from = startOffset,
                    to = endOffset,
                    durationMs = 500,
                    flyingCards = flyingCards,
                    removeAfter = false,
                    rotation = dealRotation
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
                    ?: estimateAttackSlotOffset(event.toSlotIndex, registry, density, cardWidth, numCols)

                animateCard(
                    id = "play_${event.card.hashCode()}",
                    card = event.card,
                    faceDown = false,
                    from = startOffset,
                    to = endOffset,
                    durationMs = 450,
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
                    ?: registry.getOffset(PositionKey.DrawPile)

                animateCard(
                    id = "def_${event.card.hashCode()}",
                    card = event.card,
                    faceDown = false,
                    from = startOffset,
                    to = endOffset,
                    durationMs = 450,
                    flyingCards = flyingCards
                )
            }
        }

        is AnimationEvent.DrawCardFromPile -> {
            scope.launch {
                val start = registry.getOffset(PositionKey.DrawPile)
                val endKey = if (event.playerId == 0)
                    PositionKey.HumanHandCenter
                else
                    PositionKey.PlayerArea(event.playerId)
                val end = registry.getOffset(endKey).takeIf { it != Offset.Zero } ?: start

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

        is AnimationEvent.PlayerPassed -> { /* handled in LaunchedEffect collector */ }
        is AnimationEvent.PlayerTookCards -> { /* handled in LaunchedEffect collector */ }

        is AnimationEvent.TableToPlayer -> {
            scope.launch {
                val endKey = if (event.targetPlayerId == 0)
                    PositionKey.HumanHandCenter
                else
                    PositionKey.PlayerArea(event.targetPlayerId)
                val end = registry.getOffset(endKey)
                    .takeIf { it != Offset.Zero }
                    ?: registry.getOffset(PositionKey.PlayerArea(event.targetPlayerId))
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
    flyingCards: MutableList<FlyingCard>,
    removeAfter: Boolean = true,
    rotation: Float = 0f
) {
    val animatable = Animatable(from, Offset.VectorConverter)
    val flying = FlyingCard(
        id = id,
        card = card,
        faceDown = faceDown,
        animatable = animatable,
        rotation = rotation
    )
    flyingCards.add(flying)
    animatable.animateTo(to, animationSpec = tween(durationMs, easing = FastOutSlowInEasing))
    if (removeAfter) flyingCards.remove(flying)
}


@Composable
private fun GameOverOverlay(state: GameState, onRestart: () -> Unit, onHome: () -> Unit) {
    val winner = state.players.firstOrNull { it.id == state.winnerId }
    val humanWon = winner?.isHuman == true
    val winnerName = winner?.name ?: "Someone"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .border(1.5.dp, Gold, RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F2E1A), Color(0xFF050E08))
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 40.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (humanWon) "VICTORY" else "DEFEATED",
                color = if (humanWon) Gold else DangerRed.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Text(
                text = winnerName,
                color = if (humanWon) Gold else Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "wins the round",
                color = TextOnDark.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Play Again — gold accent button
                Box(
                    modifier = Modifier
                        .border(1.5.dp, Gold, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Gold.copy(alpha = 0.15f))
                        .clickable { onRestart() }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PLAY AGAIN",
                        color = Gold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                // Home — muted button
                Box(
                    modifier = Modifier
                        .border(1.dp, TextOnDark.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .clickable { onHome() }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "HOME",
                        color = TextOnDark.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
