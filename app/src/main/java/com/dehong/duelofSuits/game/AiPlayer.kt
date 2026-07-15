package com.dehong.duelofSuits.game

import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.model.TableSlot

object AiPlayer {

    fun decideAttack(hand: List<Card>, defenderHandCount: Int, trumpSuit: Suit): List<Card> {
        val attackable = hand.filterIsInstance<Card.SuitedCard>()
        if (attackable.isEmpty()) return emptyList()

        val byRank = attackable.groupBy { it.rank }

        val scored = byRank.values.map { group ->
            val countScore = group.size * 100
            val rankPenalty = group[0].rank.ordinal
            val trumpPenalty = if (group[0].suit == trumpSuit) 20 else 0
            group to (countScore - rankPenalty - trumpPenalty)
        }.sortedByDescending { it.second }

        val bestGroup = scored.firstOrNull()?.first ?: return emptyList()
        val maxPlay = minOf(bestGroup.size, 4, defenderHandCount)
        return bestGroup.take(maxPlay)
    }

    fun decideThrowIn(hand: List<Card>, tableSlots: List<TableSlot>, defenderStartingHandCount: Int, trumpSuit: Suit): List<Card> {
        if (hand.size < 4) return emptyList()

        val existingRanks = tableSlots.flatMap { slot ->
            listOfNotNull(
                (slot.attackCard as? Card.SuitedCard)?.rank,
                (slot.defenseCard as? Card.SuitedCard)?.rank
            )
        }.toSet()

        val maxMoreCards = defenderStartingHandCount - tableSlots.size
        if (maxMoreCards <= 0) return emptyList()

        val candidates = hand.filterIsInstance<Card.SuitedCard>()
            .filter { it.rank in existingRanks }
            .sortedWith(compareBy({ it.suit == trumpSuit }, { it.rank.ordinal }))

        return candidates.take(maxMoreCards)
    }

    fun decideDefense(tableSlots: List<TableSlot>, hand: List<Card>, trumpSuit: Suit): Map<Card, Card>? {
        val undefended = tableSlots.filter { it.defenseCard == null }
        if (undefended.isEmpty()) return emptyMap()

        val result = mutableMapOf<Card, Card>()
        val available = hand.toMutableList()

        val sortedAttacks = undefended.sortedBy { attackCost(it.attackCard, trumpSuit) }

        for (slot in sortedAttacks) {
            val cheapest = available
                .filter { GameEngine.canDefend(slot.attackCard, it, trumpSuit) }
                .minByOrNull { defenseCost(it, trumpSuit) }
                ?: return null

            result[slot.attackCard] = cheapest
            available.remove(cheapest)
        }
        return result
    }

    fun shouldTakeCards(tableSlots: List<TableSlot>, hand: List<Card>, trumpSuit: Suit): Boolean {
        return decideDefense(tableSlots, hand, trumpSuit) == null
    }

    private fun defenseCost(card: Card, trumpSuit: Suit): Int = when (card) {
        is Card.Joker -> 10_000
        is Card.SuitedCard -> {
            val trumpPenalty = if (card.suit == trumpSuit) 13 else 0
            card.rank.ordinal + trumpPenalty
        }
    }

    private fun attackCost(card: Card, trumpSuit: Suit): Int = when (card) {
        is Card.Joker -> 0
        is Card.SuitedCard -> card.rank.ordinal + if (card.suit == trumpSuit) 13 else 0
    }

    fun decideAttackFromState(state: GameState, playerIndex: Int): List<Card> {
        val hand = state.players[playerIndex].hand
        val defenderCount = state.defender.hand.size
        return decideAttack(hand, defenderCount, state.trumpSuit)
    }

    fun decideThrowInFromState(state: GameState, playerIndex: Int): List<Card> {
        val hand = state.players[playerIndex].hand
        return decideThrowIn(hand, state.tableSlots, state.defenderStartingHandCount, state.trumpSuit)
    }

    fun decideDefenseFromState(state: GameState): Map<Card, Card>? {
        return decideDefense(state.tableSlots, state.defender.hand, state.trumpSuit)
    }
}
