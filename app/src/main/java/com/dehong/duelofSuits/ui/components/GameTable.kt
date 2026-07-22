package com.dehong.duelofSuits.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.game.GameEngine
import com.dehong.duelofSuits.model.CardSelectionState
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.TableSlot
import com.dehong.duelofSuits.ui.animation.LocalFlyingCards
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.theme.HighlightCyan
import com.dehong.duelofSuits.ui.theme.HighlightCyanOverlay

// Each slot = attack card + defense-card offset stacked diagonally.
// slotWidth = cardWidth * 74/54  (card + 20px diagonal offset)
private const val SLOT_W_RATIO     = 74f / 54f
private const val DEFENSE_X_RATIO  = 20f / 54f
private const val DEFENSE_Y_RATIO  = 16f / 78f
private const val ASPECT_RATIO     = 78f / 54f   // cardHeight / cardWidth

private val COL_GAP = 18.dp
private val ROW_GAP = 14.dp

@Composable
fun GameTable(
    state: GameState,
    registry: PositionRegistry,
    onDefenseSlotTapped: (Int) -> Unit,
    // Reports (numCols, maxRowsInArea) so the ViewModel can compute total board capacity.
    onGridChanged: (numCols: Int, maxRows: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val globalCardWidth  = LocalCardWidth.current
    val drawPileReserved = globalCardWidth * 1.5f + 8.dp

    val slots = state.tableSlots
    // reservedSlotCount pre-sizes the board so rows expand before cards animate in.
    val effectiveSlotCount = maxOf(slots.size, state.reservedSlotCount)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                registry.register(PositionKey.TableArea, coords)
            },
        contentAlignment = Alignment.Center
    ) {
        val gridWidth  = maxWidth - drawPileReserved
        val gridHeight = if (maxHeight.value.isInfinite()) globalCardWidth * ASPECT_RATIO * 3f
                         else maxHeight

        // boardScale drives card size. At 1.0 cards equal hand size; shrinks by ×0.75 as needed.
        val scaledCardWidth  = globalCardWidth * state.boardScale
        val scaledSlotWidth  = scaledCardWidth * SLOT_W_RATIO
        // slotHeight includes the diagonal defense-card offset below the attack card.
        val scaledSlotHeight = scaledCardWidth * ASPECT_RATIO * (1f + DEFENSE_Y_RATIO)

        // How many columns fit horizontally and rows fit vertically at the current scale?
        val numCols = ((gridWidth  + COL_GAP) / (scaledSlotWidth  + COL_GAP))
            .toInt().coerceAtLeast(1)
        val maxRowsInArea = ((gridHeight + ROW_GAP) / (scaledSlotHeight + ROW_GAP))
            .toInt().coerceAtLeast(1)

        // Report to ViewModel whenever layout capacity changes.
        LaunchedEffect(numCols, maxRowsInArea) { onGridChanged(numCols, maxRowsInArea) }

        val numRows = if (effectiveSlotCount == 0) 1
                      else (effectiveSlotCount + numCols - 1) / numCols

        val targetCardWidth = scaledCardWidth.coerceAtLeast(8.dp)

        val animatedW by animateFloatAsState(
            targetValue   = targetCardWidth.value,
            animationSpec = tween(350, easing = FastOutSlowInEasing),
            label         = "boardCardWidth"
        )

        val boardCardWidth  = animatedW.dp
        val boardCardHeight = boardCardWidth  * ASPECT_RATIO
        val defenseX        = boardCardWidth  * DEFENSE_X_RATIO
        val defenseY        = boardCardHeight * DEFENSE_Y_RATIO

        // Override card-size locals so every card inside the board uses the animated size;
        // hand cards, flying cards, and the draw pile keep the global size.
        CompositionLocalProvider(
            LocalCardWidth  provides boardCardWidth,
            LocalCardHeight provides boardCardHeight
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = drawPileReserved),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ROW_GAP),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in 0 until numRows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(COL_GAP)) {
                            for (col in 0 until numCols) {
                                val i = row * numCols + col
                                if (i < slots.size) {
                                    TableSlotView(
                                        slot                = slots[i],
                                        slotIndex           = i,
                                        state               = state,
                                        registry            = registry,
                                        cardWidth           = boardCardWidth,
                                        cardHeight          = boardCardHeight,
                                        defenseX            = defenseX,
                                        defenseY            = defenseY,
                                        onDefenseSlotTapped = onDefenseSlotTapped
                                    )
                                } else {
                                    // Invisible placeholder: registers position immediately so
                                    // flying-card animations always have a valid target.
                                    Box(
                                        modifier = Modifier
                                            .requiredSize(boardCardWidth + defenseX, boardCardHeight + defenseY)
                                            .onGloballyPositioned { coords ->
                                                registry.register(PositionKey.AttackSlot(i), coords)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingDefenseTarget(
    modifier: Modifier,
    isHumanDefender: Boolean,
    slotIndex: Int,
    registry: PositionRegistry,
    onDefenseSlotTapped: (Int) -> Unit
) {
    val transition = rememberInfiniteTransition(label = "defenseSlot")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Box(
        modifier = modifier
            .background(HighlightCyanOverlay, RoundedCornerShape(6.dp))
            .border(2.dp, HighlightCyan.copy(alpha = pulseAlpha), RoundedCornerShape(6.dp))
            .then(
                if (isHumanDefender) Modifier.clickable { onDefenseSlotTapped(slotIndex) }
                else Modifier
            )
            .onGloballyPositioned { coords ->
                registry.register(PositionKey.DefenseSlot(slotIndex), coords)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "↓",
            color = HighlightCyan.copy(alpha = pulseAlpha),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TableSlotView(
    slot: TableSlot,
    slotIndex: Int,
    state: GameState,
    registry: PositionRegistry,
    cardWidth: Dp,
    cardHeight: Dp,
    defenseX: Dp,
    defenseY: Dp,
    onDefenseSlotTapped: (Int) -> Unit
) {
    val isHumanDefender  = state.isHumanDefender && state.phase == GamePhase.DEFENSE_PHASE
    val selectedHandCard = state.selectedHandCardForDefense
    val canDefendThisSlot = isHumanDefender && slot.defenseCard == null &&
            selectedHandCard != null &&
            GameEngine.canDefend(slot.attackCard, selectedHandCard, state.trumpSuit)
    val flyingCards = LocalFlyingCards.current

    CompositionLocalProvider(
        LocalCardWidth  provides cardWidth,
        LocalCardHeight provides cardHeight
    ) {
        Box(modifier = Modifier.requiredSize(cardWidth + defenseX, cardHeight + defenseY)) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .requiredSize(cardWidth, cardHeight)
                    .alpha(if (slot.attackCard in flyingCards) 0f else 1f)
                    .onGloballyPositioned { coords ->
                        registry.register(PositionKey.AttackSlot(slotIndex), coords)
                    }
            ) {
                CardView(card = slot.attackCard)
            }

            if (slot.defenseCard != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = defenseX, y = defenseY)
                        .requiredSize(cardWidth, cardHeight)
                        .alpha(if (slot.defenseCard in flyingCards) 0f else 1f)
                        .onGloballyPositioned { coords ->
                            registry.register(PositionKey.DefenseSlot(slotIndex), coords)
                        }
                ) {
                    CardView(card = slot.defenseCard, selectionState = CardSelectionState.COMMITTED)
                }
            } else {
                val slotModifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = defenseX, y = defenseY)
                    .requiredSize(cardWidth, cardHeight)
                if (canDefendThisSlot) {
                    PulsingDefenseTarget(
                        modifier = slotModifier,
                        isHumanDefender = isHumanDefender,
                        slotIndex = slotIndex,
                        registry = registry,
                        onDefenseSlotTapped = onDefenseSlotTapped
                    )
                } else {
                    Box(
                        modifier = slotModifier
                            .then(
                                if (isHumanDefender) Modifier.clickable { onDefenseSlotTapped(slotIndex) }
                                else Modifier
                            )
                            .onGloballyPositioned { coords ->
                                registry.register(PositionKey.DefenseSlot(slotIndex), coords)
                            }
                    )
                }
            }
        }
    }
}
