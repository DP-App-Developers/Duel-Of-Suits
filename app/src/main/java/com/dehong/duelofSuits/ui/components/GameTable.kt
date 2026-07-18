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
import androidx.compose.ui.draw.alpha
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
import com.dehong.duelofSuits.ui.animation.LocalFlyingCards
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.theme.HighlightCyan
import com.dehong.duelofSuits.ui.theme.SelectedBorder

private val DEFENSE_X_RATIO = 20f / 54f
private val DEFENSE_Y_RATIO = 16f / 78f

@Composable
fun GameTable(
    state: GameState,
    registry: PositionRegistry,
    onDefenseSlotTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardWidth  = LocalCardWidth.current
    val cardHeight = LocalCardHeight.current
    val defenseX   = cardWidth  * DEFENSE_X_RATIO
    val defenseY   = cardHeight * DEFENSE_Y_RATIO
    val slots = state.tableSlots
    if (slots.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(cardHeight + defenseY)
                .onGloballyPositioned { coords ->
                    registry.register(PositionKey.TableArea, coords)
                },
            contentAlignment = Alignment.Center
        ) {
        }
        return
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .onGloballyPositioned { coords ->
                registry.register(PositionKey.TableArea, coords)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(slots) { index, slot ->
            TableSlotView(
                slot = slot,
                slotIndex = index,
                state = state,
                registry = registry,
                defenseX = defenseX,
                defenseY = defenseY,
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
    defenseX: androidx.compose.ui.unit.Dp,
    defenseY: androidx.compose.ui.unit.Dp,
    onDefenseSlotTapped: (Int) -> Unit
) {
    val cardWidth  = LocalCardWidth.current
    val cardHeight = LocalCardHeight.current
    val isHumanDefender = state.isHumanDefender && state.phase == GamePhase.DEFENSE_PHASE
    val selectedHandCard = state.selectedHandCardForDefense
    val canDefendThisSlot = isHumanDefender && slot.defenseCard == null &&
            selectedHandCard != null &&
            GameEngine.canDefend(slot.attackCard, selectedHandCard, state.trumpSuit)
    val flyingCards = LocalFlyingCards.current

    Box(
        modifier = Modifier.size(cardWidth + defenseX, cardHeight + defenseY)
    ) {
        CardView(
            card = slot.attackCard,
            modifier = Modifier
                .align(Alignment.TopStart)
                .alpha(if (slot.attackCard in flyingCards) 0f else 1f)
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
                    .offset(x = defenseX, y = defenseY)
                    .alpha(if (slot.defenseCard in flyingCards) 0f else 1f)
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
                    .offset(x = defenseX, y = defenseY)
                    .size(cardWidth, cardHeight)
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
