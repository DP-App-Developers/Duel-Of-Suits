package com.dehong.duelofSuits.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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

private val DEFENSE_X_RATIO = 20f / 54f
private val DEFENSE_Y_RATIO = 16f / 78f

// Every row always contains exactly this many slot-columns (real cards or invisible placeholders).
// Placeholders register their position immediately so flying-card animations always have a valid
// target before the card state is committed, and every row is the same width.
internal const val ROW_THRESHOLD = 4

@Composable
fun GameTable(
    state: GameState,
    registry: PositionRegistry,
    onDefenseSlotTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Read card dimensions exactly once. Every slot receives these same values so all
    // cards on the board are guaranteed identical in size, regardless of row or position.
    val cardWidth  = LocalCardWidth.current
    val cardHeight = LocalCardHeight.current
    val defenseX   = cardWidth  * DEFENSE_X_RATIO
    val defenseY   = cardHeight * DEFENSE_Y_RATIO
    val slots      = state.tableSlots
    val drawPileReserved = cardWidth * 1.5f + 8.dp

    val numRows = if (slots.isEmpty()) 1 else (slots.size + ROW_THRESHOLD - 1) / ROW_THRESHOLD

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                registry.register(PositionKey.TableArea, coords)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = drawPileReserved),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0 until numRows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (col in 0 until ROW_THRESHOLD) {
                            val i = row * ROW_THRESHOLD + col
                            if (i < slots.size) {
                                TableSlotView(
                                    slot                = slots[i],
                                    slotIndex           = i,
                                    state               = state,
                                    registry            = registry,
                                    cardWidth           = cardWidth,
                                    cardHeight          = cardHeight,
                                    defenseX            = defenseX,
                                    defenseY            = defenseY,
                                    onDefenseSlotTapped = onDefenseSlotTapped
                                )
                            } else {
                                // Invisible placeholder: same dimensions as a real slot so the
                                // row width is stable and the registered position is always valid.
                                Box(
                                    modifier = Modifier
                                        .requiredSize(cardWidth + defenseX, cardHeight + defenseY)
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
    val isHumanDefender = state.isHumanDefender && state.phase == GamePhase.DEFENSE_PHASE
    val selectedHandCard = state.selectedHandCardForDefense
    val canDefendThisSlot = isHumanDefender && slot.defenseCard == null &&
            selectedHandCard != null &&
            GameEngine.canDefend(slot.attackCard, selectedHandCard, state.trumpSuit)
    val flyingCards = LocalFlyingCards.current

    // Pin exact card dimensions for all CardView children in this slot so that attack
    // and defense cards are pixel-identical in size regardless of any parent constraint.
    CompositionLocalProvider(
        LocalCardWidth  provides cardWidth,
        LocalCardHeight provides cardHeight
    ) {
        // requiredSize ignores incoming parent constraints — slot dimensions are always exact.
        Box(modifier = Modifier.requiredSize(cardWidth + defenseX, cardHeight + defenseY)) {

            // Attack card — wrapped in a requiredSize box so the card cannot be squeezed.
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
                // Defense card — same hard-sized wrapper.
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
                val slotBorderColor = when {
                    canDefendThisSlot -> HighlightCyan
                    isHumanDefender   -> Color.White.copy(alpha = 0.25f)
                    else              -> Color.White.copy(alpha = 0.15f)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = defenseX, y = defenseY)
                        .requiredSize(cardWidth, cardHeight)
                        .border(
                            width = if (canDefendThisSlot) 2.dp else 1.dp,
                            color = slotBorderColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .then(
                            if (isHumanDefender) Modifier.clickable { onDefenseSlotTapped(slotIndex) }
                            else Modifier
                        )
                        .onGloballyPositioned { coords ->
                            registry.register(PositionKey.DefenseSlot(slotIndex), coords)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isHumanDefender) {
                        Text(
                            text = if (canDefendThisSlot) "▸" else "?",
                            color = slotBorderColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
