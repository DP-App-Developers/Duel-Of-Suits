package com.dehong.duelofSuits.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import com.dehong.duelofSuits.model.Card

sealed class PositionKey {
    object DrawPile : PositionKey()
    object DiscardPile : PositionKey()
    data class HandCard(val playerId: Int, val cardIndex: Int) : PositionKey()
    data class AttackSlot(val slotIndex: Int) : PositionKey()
    data class DefenseSlot(val slotIndex: Int) : PositionKey()
    data class PlayerArea(val playerId: Int) : PositionKey()
}

class PositionRegistry {
    private val positions = mutableStateMapOf<String, Offset>()

    fun register(key: PositionKey, coordinates: LayoutCoordinates) {
        positions[key.toKey()] = coordinates.positionInRoot()
    }

    fun getOffset(key: PositionKey): Offset = positions[key.toKey()] ?: Offset.Zero

    private fun PositionKey.toKey(): String = when (this) {
        is PositionKey.DrawPile -> "draw"
        is PositionKey.DiscardPile -> "discard"
        is PositionKey.HandCard -> "hand_${playerId}_$cardIndex"
        is PositionKey.AttackSlot -> "attack_$slotIndex"
        is PositionKey.DefenseSlot -> "defense_$slotIndex"
        is PositionKey.PlayerArea -> "player_$playerId"
    }
}

class FlyingCard(
    val id: String,
    val card: Card,
    val faceDown: Boolean,
    val animatable: Animatable<Offset, AnimationVector2D>
)

sealed class AnimationEvent {
    data class DealCard(
        val card: Card,
        val targetPlayerId: Int,
        val cardIndex: Int,
        val delayMs: Long
    ) : AnimationEvent()

    data class PlayCardToTable(
        val card: Card,
        val fromPlayerId: Int,
        val toSlotIndex: Int
    ) : AnimationEvent()

    data class DrawCardFromPile(
        val playerId: Int,
        val count: Int
    ) : AnimationEvent()

    data class DefenseCard(
        val card: Card,
        val fromPlayerId: Int,
        val toSlotIndex: Int
    ) : AnimationEvent()

    data class TableToPlayer(val targetPlayerId: Int, val cards: List<Card>) : AnimationEvent()

    data class PlayerPassed(val playerIdx: Int) : AnimationEvent()

    data class PlayerTookCards(val playerIdx: Int) : AnimationEvent()
}
