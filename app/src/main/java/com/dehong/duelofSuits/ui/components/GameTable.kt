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
// slotWidth  = cardWidth  * (54 + 20) / 54 = cardWidth  * 74/54
// slotHeight = cardHeight * (78 + 16) / 78 = cardHeight * 94/78
// Inverting (via aspect ratio cardHeight = cardWidth * 78/54):
//   cardWidth from slot width  = slotWidth  * 54/74
//   cardWidth from slot height = slotHeight * 54/94
private const val CARD_W_FROM_SLOT_W = 54f / 74f
private const val CARD_W_FROM_SLOT_H = 54f / 94f
private const val DEFENSE_X_RATIO   = 20f / 54f
private const val DEFENSE_Y_RATIO   = 16f / 78f
private const val ASPECT_RATIO      = 78f / 54f   // cardHeight / cardWidth

// Minimum column count; actual count grows to fill available width at hand-card size.
internal const val MIN_COLS = 4
private val COL_GAP = 18.dp
private val ROW_GAP = 14.dp

@Composable
fun GameTable(
    state: GameState,
    registry: PositionRegistry,
    onDefenseSlotTapped: (Int) -> Unit,
    onNumColsChanged: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Global card width (= hand card size) drives both draw-pile reservation and the
    // board-card size cap so board cards are never larger than hand cards.
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
        // Guard against unconstrained height (should not occur given weight(0.40f) on CenterPanel).
        val gridHeight = if (maxHeight.value.isInfinite()) globalCardWidth * ASPECT_RATIO * 2.6f
                         else maxHeight

        // How many columns fit at the global (hand) card size? At least MIN_COLS.
        val globalSlotWidth = globalCardWidth * (74f / 54f)
        val numCols = ((gridWidth + COL_GAP) / (globalSlotWidth + COL_GAP))
            .toInt()
            .coerceAtLeast(MIN_COLS)

        // Report numCols whenever it changes so the ViewModel can sequence animations.
        LaunchedEffect(numCols) { onNumColsChanged(numCols) }

        val numRows = if (effectiveSlotCount == 0) 1
                      else (effectiveSlotCount + numCols - 1) / numCols

        // Slot dimensions that tile the available area across numCols × numRows.
        val slotWidth  = (gridWidth  - COL_GAP * (numCols - 1)) / numCols
        val slotHeight = (gridHeight - ROW_GAP  * (numRows  - 1)) / numRows

        // Tightest constraint wins; cap at global card size so board ≤ hand.
        val targetCardWidth = minOf(slotWidth * CARD_W_FROM_SLOT_W, slotHeight * CARD_W_FROM_SLOT_H)
            .coerceAtMost(globalCardWidth)
            .coerceAtLeast(8.dp)

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
