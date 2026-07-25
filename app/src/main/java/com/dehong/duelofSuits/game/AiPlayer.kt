package com.dehong.duelofSuits.game

import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.model.TableSlot

object AiPlayer {

    private enum class GameEra { EARLY, MID, LATE }

    private fun gameEra(drawPileSize: Int) = when {
        drawPileSize > 16 -> GameEra.EARLY
        drawPileSize == 0 -> GameEra.LATE
        else -> GameEra.MID
    }

    // Cards confirmed out of play: in the discard pile or currently visible on the table.
    private fun knownPlayedCards(state: GameState): Set<Card> {
        val tableCards = state.tableSlots.flatMap { listOfNotNull(it.attackCard, it.defenseCard) }
        return (state.discardPile + tableCards).toSet()
    }

    // For each suit, returns the card in `hand` that is the highest remaining unplayed copy
    // in the entire deck — i.e. a "stopper" the AI should conserve.
    private fun stopperCards(state: GameState, hand: List<Card>): Set<Card> {
        val played = knownPlayedCards(state)
        val result = mutableSetOf<Card>()
        for (suit in Suit.values()) {
            val highestInHand = hand.filterIsInstance<Card.SuitedCard>()
                .filter { it.suit == suit }
                .maxByOrNull { it.rank.ordinal } ?: continue
            val higherExists = Rank.values().any { rank ->
                rank.ordinal > highestInHand.rank.ordinal &&
                        Card.SuitedCard(suit, rank) !in played
            }
            if (!higherExists) result.add(highestInHand)
        }
        return result
    }

    // Simulates the next replenishment draw order and returns which player index draws the
    // last remaining card (the trump indicator card).
    private fun whoDrawsLastCard(state: GameState): Int? {
        if (state.drawPile.isEmpty()) return null
        val order = (0 until state.playerCount).map { (state.attackerIndex + it) % state.playerCount }
        var remaining = state.drawPile.size
        for (idx in order) {
            val needed = (8 - state.players[idx].hand.size).coerceAtLeast(0)
            if (remaining <= needed) return idx
            remaining -= needed
        }
        return order.last()
    }

    // ── Public API ────────────────────────────────────────────────────────────────

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

        if (hardMode && hand.size - toThrowIn.size < 4) return emptyList()

        return toThrowIn
    }

    fun decideDefense(
        tableSlots: List<TableSlot>,
        hand: List<Card>,
        trumpSuit: Suit,
        hardMode: Boolean = false
    ): Map<Card, Card>? = decideDefenseInternal(tableSlots, hand, trumpSuit, hardMode, GameEra.MID)

    fun shouldTakeCards(tableSlots: List<TableSlot>, hand: List<Card>, trumpSuit: Suit): Boolean {
        return decideDefense(tableSlots, hand, trumpSuit) == null
    }

    fun decideAttackFromState(state: GameState, playerIndex: Int, hardMode: Boolean = false): List<Card> {
        val hand = state.players[playerIndex].hand
        val defenderCount = state.defender.hand.size
        if (!hardMode) return decideAttack(hand, defenderCount, state.trumpSuit, false)
        return decideAttackHard(hand, defenderCount, state.trumpSuit, state, playerIndex)
    }

    fun decideThrowInFromState(state: GameState, playerIndex: Int, hardMode: Boolean = false): List<Card> {
        val hand = state.players[playerIndex].hand
        val result = decideThrowIn(hand, state.tableSlots, state.defenderStartingHandCount, state.trumpSuit, hardMode)

        // Joker-only avoidance: if the draw pile is nearly empty and throwing in these cards
        // would leave us holding only Jokers, skip the throw-in to avoid the forced-draw penalty.
        if (hardMode && state.drawPile.size <= 4 && result.isNotEmpty()) {
            val remainingHand = hand - result.toSet()
            if (remainingHand.isNotEmpty() && remainingHand.all { it is Card.Joker }) return emptyList()
        }

        return result
    }

    fun decideDefenseFromState(state: GameState, hardMode: Boolean = false): Map<Card, Card>? {
        val era = gameEra(state.drawPile.size)
        val plan = decideDefenseInternal(state.tableSlots, state.defender.hand, state.trumpSuit, hardMode, era)
            ?: return null
        if (hardMode && plan.isNotEmpty() && shouldVoluntarilyTakeHard(state, plan, era)) {
            return null
        }
        return plan
    }

    // ── Hard-mode attack logic ────────────────────────────────────────────────────

    private fun decideAttackHard(
        hand: List<Card>,
        defenderHandCount: Int,
        trumpSuit: Suit,
        state: GameState,
        playerIndex: Int
    ): List<Card> {
        val attackable = hand.filterIsInstance<Card.SuitedCard>()
        if (attackable.isEmpty()) return emptyList()

        val era = gameEra(state.drawPile.size)
        val stoppers = stopperCards(state, hand)
        // Will this AI player draw the trump card in the upcoming replenishment?
        val iGetTrump = state.drawPile.isNotEmpty() && whoDrawsLastCard(state) == playerIndex
        val byRank = attackable.groupBy { it.rank }

        val scored = byRank.values.map { group ->
            val card = group[0]
            var score = group.size * 100
            score -= card.rank.ordinal  // Prefer lower-rank cards as attack fodder

            // Trump penalty scaled by game era and whether we'll draw the trump card anyway
            if (card.suit == trumpSuit) {
                score -= when (era) {
                    GameEra.EARLY -> 80   // Very reluctant to spend trumps early
                    GameEra.MID -> if (iGetTrump) 35 else 50  // Save harder when trump will go to someone else
                    GameEra.LATE -> 20    // Trumps still matter but we must play something
                }
            }

            // Don't squander high non-trump cards before the mid/late game pressure kicks in
            if (era != GameEra.LATE && card.suit != trumpSuit && card.rank.ordinal >= Rank.KING.ordinal) {
                score -= 15
            }

            // Stopper penalty: conserve the highest remaining card of each suit
            if (era != GameEra.LATE && card in stoppers) {
                score -= 30
            }

            group to score
        }.sortedByDescending { it.second }

        val bestGroup = scored.firstOrNull()?.first ?: return emptyList()
        val maxByRules = minOf(bestGroup.size, 4, defenderHandCount)

        // Attack diversity: playing all copies of a rank too early wastes follow-up potential.
        // Holding a second copy gives us a devastating follow-up if the first attack succeeds.
        val playCount = when {
            era == GameEra.LATE -> maxByRules
            bestGroup.size >= 3 && era == GameEra.EARLY -> 1   // Save 2 of 3+ for later
            bestGroup.size >= 3 -> minOf(2, maxByRules)         // Play at most 2 in mid game
            else -> maxByRules
        }

        return bestGroup.take(playCount)
    }

    // ── Defense internals ─────────────────────────────────────────────────────────

    private fun decideDefenseInternal(
        tableSlots: List<TableSlot>,
        hand: List<Card>,
        trumpSuit: Suit,
        hardMode: Boolean,
        era: GameEra
    ): Map<Card, Card>? {
        val undefended = tableSlots.filter { it.defenseCard == null }
        if (undefended.isEmpty()) return emptyMap()

        val result = mutableMapOf<Card, Card>()
        val available = hand.toMutableList()

        val sortedAttacks = undefended.sortedBy { attackCost(it.attackCard, trumpSuit) }

        for (slot in sortedAttacks) {
            val cheapest = available
                .filter { GameEngine.canDefend(slot.attackCard, it, trumpSuit) }
                .minByOrNull { defenseCost(it, trumpSuit, hardMode, era) }
                ?: return null

            result[slot.attackCard] = cheapest
            available.remove(cheapest)
        }
        return result
    }

    private fun shouldVoluntarilyTakeHard(state: GameState, plan: Map<Card, Card>, era: GameEra): Boolean {
        val tableSlots = state.tableSlots
        val trumpSuit = state.trumpSuit

        val jokerSpent = plan.values.any { it is Card.Joker }
        val trumpsSpent = plan.values.count { it is Card.SuitedCard && it.suit == trumpSuit }
        val cheapAttacks = tableSlots.count { slot ->
            val a = slot.attackCard
            a is Card.SuitedCard && a.suit != trumpSuit && a.rank.ordinal < Rank.NINE.ordinal
        }
        val strongAttacks = tableSlots.count { slot ->
            val a = slot.attackCard
            a is Card.SuitedCard && (a.suit == trumpSuit || a.rank.ordinal >= Rank.KING.ordinal)
        }

        // Classic: Joker or 2+ trumps burned defending cheap junk is a bad trade
        if (jokerSpent && cheapAttacks >= 2) return true
        if (trumpsSpent >= 2 && cheapAttacks >= 2) return true

        // Early game: collecting strong cards can be more valuable than burning a Joker to deflect them
        if (era == GameEra.EARLY && jokerSpent && strongAttacks >= 1) return true

        // Late game: if neither Joker has been played yet, spending yours here is almost certainly wrong
        if (era == GameEra.LATE && jokerSpent) {
            val played = knownPlayedCards(state)
            val jokersPlayed = played.filterIsInstance<Card.Joker>().size
            if (jokersPlayed == 0 && cheapAttacks >= 1) return true
        }

        return false
    }

    // ── Cost functions ────────────────────────────────────────────────────────────

    private fun defenseCost(card: Card, trumpSuit: Suit, hardMode: Boolean = false, era: GameEra = GameEra.MID): Int =
        when (card) {
            is Card.Joker -> 10_000
            is Card.SuitedCard -> {
                val trumpPenalty = if (card.suit == trumpSuit) {
                    if (!hardMode) 13
                    else when (era) {
                        GameEra.EARLY, GameEra.MID -> 26
                        GameEra.LATE -> 35  // Every trump is precious once the draw pile is gone
                    }
                } else 0
                card.rank.ordinal + trumpPenalty
            }
        }

    private fun attackCost(card: Card, trumpSuit: Suit): Int = when (card) {
        is Card.Joker -> 0
        is Card.SuitedCard -> card.rank.ordinal + if (card.suit == trumpSuit) 13 else 0
    }
}
