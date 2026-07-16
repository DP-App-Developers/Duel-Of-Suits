package com.dehong.duelofSuits.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.game.GameEngine
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.CardSelectionState
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.TableSlot
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.theme.HighlightCyan
import com.dehong.duelofSuits.ui.theme.SelectedBorder

private val DEFENSE_X = 20.dp
private val DEFENSE_Y = 16.dp

@Composable
fun GameTable(
    state: GameState,
    registry: PositionRegistry,
    onDefenseSlotTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val slots = state.tableSlots
    if (slots.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(CARD_HEIGHT + 20.dp),
            contentAlignment = Alignment.Center
        ) {
        }
        return
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(slots) { index, slot ->
            TableSlotView(
                slot = slot,
                slotIndex = index,
                state = state,
                registry = registry,
                onDefenseSlotTapped = onDefenseSlotTapped
            )
        }
    }
}

@Composable
private fun TableSlotView(
    slot: TableSlot,
    slotIndex: Int,
    state: GameState,
    registry: PositionRegistry,
    onDefenseSlotTapped: (Int) -> Unit
) {
    val isHumanDefender = state.isHumanDefender && state.phase == GamePhase.DEFENSE_PHASE
    val selectedHandCard = state.selectedHandCardForDefense
    val canDefendThisSlot = isHumanDefender && slot.defenseCard == null &&
            selectedHandCard != null &&
            GameEngine.canDefend(slot.attackCard, selectedHandCard, state.trumpSuit)

    Box(
        modifier = Modifier.size(CARD_WIDTH + DEFENSE_X, CARD_HEIGHT + DEFENSE_Y)
    ) {
        CardView(
            card = slot.attackCard,
            modifier = Modifier
                .align(Alignment.TopStart)
                .onGloballyPositioned { coords ->
                    registry.register(PositionKey.AttackSlot(slotIndex), coords)
                }
        )

        if (slot.defenseCard != null) {
            CardView(
                card = slot.defenseCard,
                selectionState = CardSelectionState.COMMITTED,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = DEFENSE_X, y = DEFENSE_Y)
                    .onGloballyPositioned { coords ->
                        registry.register(PositionKey.DefenseSlot(slotIndex), coords)
                    }
            )
        } else {
            val slotBorderColor = when {
                canDefendThisSlot -> HighlightCyan
                isHumanDefender -> Color.White.copy(alpha = 0.25f)
                else -> Color.White.copy(alpha = 0.15f)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = DEFENSE_X, y = DEFENSE_Y)
                    .size(CARD_WIDTH, CARD_HEIGHT)
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
