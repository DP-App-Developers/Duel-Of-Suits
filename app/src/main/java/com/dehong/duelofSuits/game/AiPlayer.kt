package com.dehong.duelofSuits.game

import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.model.TableSlot

object AiPlayer {

    fun decideAttack(hand: List<Card>, defenderHandCount: Int, trumpSuit: Suit, hardMode: Boolean = false): List<Card> {
        val attackable = hand.filterIsInstance<Card.SuitedCard>()
        if (attackable.isEmpty()) return emptyList()

        val byRank = attackable.groupBy { it.rank }
        val trumpPenalty = if (hardMode) 40 else 20

        val scored = byRank.values.map { group ->
            val countScore = group.size * 100
            val rankPenalty = group[0].rank.ordinal
            val isTrumpPenalty = if (group[0].suit == trumpSuit) trumpPenalty else 0
            group to (countScore - rankPenalty - isTrumpPenalty)
        }.sortedByDescending { it.second }

        val bestGroup = scored.firstOrNull()?.first ?: return emptyList()
        val maxPlay = minOf(bestGroup.size, 4, defenderHandCount)
        return bestGroup.take(maxPlay)
    }

    fun decideThrowIn(
        hand: List<Card>,
        tableSlots: List<TableSlot>,
        defenderStartingHandCount: Int,
        trumpSuit: Suit,
        hardMode: Boolean = false
    ): List<Card> {
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

        val toThrowIn = candidates.take(maxMoreCards)

        // Hard mode: don't throw in if hand would drop too low — preserve options for later
        if (hardMode && hand.size - toThrowIn.size < 4) return emptyList()

        return toThrowIn
    }

    fun decideDefense(
        tableSlots: List<TableSlot>,
        hand: List<Card>,
        trumpSuit: Suit,
        hardMode: Boolean = false
    ): Map<Card, Card>? {
        val undefended = tableSlots.filter { it.defenseCard == null }
        if (undefended.isEmpty()) return emptyMap()

        val result = mutableMapOf<Card, Card>()
        val available = hand.toMutableList()

        val sortedAttacks = undefended.sortedBy { attackCost(it.attackCard, trumpSuit) }

        for (slot in sortedAttacks) {
            val cheapest = available
                .filter { GameEngine.canDefend(slot.attackCard, it, trumpSuit) }
                .minByOrNull { defenseCost(it, trumpSuit, hardMode) }
                ?: return null

            result[slot.attackCard] = cheapest
            available.remove(cheapest)
        }
        return result
    }

    fun shouldTakeCards(tableSlots: List<TableSlot>, hand: List<Card>, trumpSuit: Suit): Boolean {
        return decideDefense(tableSlots, hand, trumpSuit) == null
    }

    // Hard mode: even when a defense is possible, sometimes taking cards preserves stronger cards.
    // Returns true if the planned defense wastes jokers or multiple trumps on cheap attacks.
    private fun shouldVoluntarilyTake(tableSlots: List<TableSlot>, plan: Map<Card, Card>, trumpSuit: Suit): Boolean {
        val jokerSpent = plan.values.any { it is Card.Joker }
        val trumpsSpent = plan.values.count { it is Card.SuitedCard && it.suit == trumpSuit }
        val cheapAttacks = tableSlots.count { slot ->
            val a = slot.attackCard
            a is Card.SuitedCard && a.suit != trumpSuit && a.rank.ordinal < Rank.NINE.ordinal
        }
        // Spending a Joker against 2+ cheap cards is wasteful
        if (jokerSpent && cheapAttacks >= 2) return true
        // Spending 2+ trumps against 2+ cheap cards is wasteful
        if (trumpsSpent >= 2 && cheapAttacks >= 2) return true
        return false
    }

    private fun defenseCost(card: Card, trumpSuit: Suit, hardMode: Boolean = false): Int = when (card) {
        is Card.Joker -> 10_000
        is Card.SuitedCard -> {
            // Hard mode saves trumps more aggressively with a higher penalty
            val trumpPenalty = if (card.suit == trumpSuit) (if (hardMode) 26 else 13) else 0
            card.rank.ordinal + trumpPenalty
        }
    }

    private fun attackCost(card: Card, trumpSuit: Suit): Int = when (card) {
        is Card.Joker -> 0
        is Card.SuitedCard -> card.rank.ordinal + if (card.suit == trumpSuit) 13 else 0
    }

    fun decideAttackFromState(state: GameState, playerIndex: Int, hardMode: Boolean = false): List<Card> {
        val hand = state.players[playerIndex].hand
        val defenderCount = state.defender.hand.size
        return decideAttack(hand, defenderCount, state.trumpSuit, hardMode)
    }

    fun decideThrowInFromState(state: GameState, playerIndex: Int, hardMode: Boolean = false): List<Card> {
        val hand = state.players[playerIndex].hand
        return decideThrowIn(hand, state.tableSlots, state.defenderStartingHandCount, state.trumpSuit, hardMode)
    }

    fun decideDefenseFromState(state: GameState, hardMode: Boolean = false): Map<Card, Card>? {
        val plan = decideDefense(state.tableSlots, state.defender.hand, state.trumpSuit, hardMode)
            ?: return null
        // Hard mode: take voluntarily if defending costs too much
        if (hardMode && plan.isNotEmpty() && shouldVoluntarilyTake(state.tableSlots, plan, state.trumpSuit)) {
            return null
        }
        return plan
    }
}
