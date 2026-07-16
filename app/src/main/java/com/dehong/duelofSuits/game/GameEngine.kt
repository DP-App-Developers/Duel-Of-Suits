package com.dehong.duelofSuits.game

import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Player
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.model.TableSlot

object GameEngine {

    fun canDefend(attackCard: Card, defenseCard: Card, trumpSuit: Suit): Boolean {
        if (defenseCard is Card.Joker) return true
        if (attackCard is Card.Joker) return false
        val attack = attackCard as Card.SuitedCard
        val defense = defenseCard as Card.SuitedCard
        return when {
            defense.suit == trumpSuit && attack.suit != trumpSuit -> true
            defense.suit == attack.suit && defense.rank.ordinal > attack.rank.ordinal -> true
            else -> false
        }
    }

    fun validateAttack(cards: List<Card>, state: GameState): String? {
        if (cards.isEmpty()) return "Select cards to attack"
        if (cards.any { it is Card.Joker }) return "Jokers cannot be used to attack"
        val suited = cards.filterIsInstance<Card.SuitedCard>()
        if (suited.map { it.rank }.toSet().size != 1) return "All attack cards must be the same rank"
        if (cards.size > 4) return "Can play at most 4 cards"
        val newTotal = state.tableSlots.size + cards.size
        if (newTotal > state.defender.hand.size) return "Too many attack cards (defender has ${state.defender.hand.size} cards)"
        return null
    }

    fun validateThrowIn(cards: List<Card>, state: GameState): String? {
        if (cards.isEmpty()) return "Select cards to throw in"
        if (cards.any { it is Card.Joker }) return "Jokers cannot be thrown in"
        val existingRanks = getTableRanks(state)
        val throwRanks = cards.filterIsInstance<Card.SuitedCard>().map { it.rank }.toSet()
        if (!throwRanks.all { it in existingRanks }) return "Thrown cards must match a rank on the table"
        val newTotal = state.tableSlots.size + cards.size
        if (newTotal > state.defenderStartingHandCount) return "Too many cards (defender had ${state.defenderStartingHandCount} cards at start of turn)"
        return null
    }

    fun processAttack(cards: List<Card>, playerIndex: Int, state: GameState): GameState {
        val newSlots = state.tableSlots + cards.map { TableSlot(attackCard = it) }
        val updatedPlayers = state.players.toMutableList()
        val player = updatedPlayers[playerIndex]
        updatedPlayers[playerIndex] = player.copy(hand = player.hand - cards.toSet())
        return state.copy(
            players = updatedPlayers,
            tableSlots = newSlots,
            selectedCards = emptySet(),
            phase = GamePhase.THROW_IN_PHASE,
            throwInPassedIndices = emptySet(),
            defenderStartingHandCount = state.defender.hand.size,
            message = "${state.defender.name} must defend"
        )
    }

    fun processThrowIn(cards: List<Card>, playerIndex: Int, state: GameState): GameState {
        val newSlots = state.tableSlots + cards.map { TableSlot(attackCard = it) }
        val updatedPlayers = state.players.toMutableList()
        val player = updatedPlayers[playerIndex]
        updatedPlayers[playerIndex] = player.copy(hand = player.hand - cards.toSet())
        val newPassed = state.throwInPassedIndices + playerIndex
        val newState = state.copy(
            players = updatedPlayers,
            tableSlots = newSlots,
            selectedCards = emptySet(),
            throwInPassedIndices = newPassed,
            message = "${state.defender.name} must defend ${newSlots.size} card(s)"
        )
        return checkThrowInEnd(newState)
    }

    fun processPass(playerIndex: Int, state: GameState): GameState {
        val newPassed = state.throwInPassedIndices + playerIndex
        val newState = state.copy(throwInPassedIndices = newPassed)
        return checkThrowInEnd(newState)
    }

    private fun checkThrowInEnd(state: GameState): GameState {
        val nonDefenders = state.nonDefenderIndices
        if (!nonDefenders.all { it in state.throwInPassedIndices }) return state
        return if (state.tableSlots.any { it.defenseCard == null }) {
            state.copy(
                phase = GamePhase.DEFENSE_PHASE,
                throwInPassedIndices = emptySet(),
                message = "${state.defender.name} is defending"
            )
        } else {
            state.copy(
                phase = GamePhase.REPLENISH_PHASE,
                throwInPassedIndices = emptySet(),
                message = "Defense successful!"
            )
        }
    }

    fun processDefenseCard(attackCard: Card, defenseCard: Card, playerIndex: Int, state: GameState): GameState {
        val updatedSlots = state.tableSlots.map { slot ->
            if (slot.attackCard == attackCard && slot.defenseCard == null) {
                slot.copy(defenseCard = defenseCard)
            } else slot
        }
        val updatedPlayers = state.players.toMutableList()
        val player = updatedPlayers[playerIndex]
        updatedPlayers[playerIndex] = player.copy(hand = player.hand - setOf(defenseCard))
        val newState = state.copy(
            players = updatedPlayers,
            tableSlots = updatedSlots,
            selectedCards = emptySet(),
            selectedHandCardForDefense = null
        )
        return if (newState.allSlotsDefended) {
            newState.copy(message = "All defended! Continue or end turn")
        } else {
            newState.copy(message = "${newState.undefendedSlots.size} card(s) left to defend")
        }
    }

    fun resolveSuccessfulDefense(state: GameState): GameState {
        val allTableCards = state.tableSlots.flatMap { slot ->
            listOfNotNull(slot.attackCard, slot.defenseCard)
        }
        return state.copy(
            discardPile = state.discardPile + allTableCards,
            tableSlots = emptyList(),
            phase = GamePhase.REPLENISH_PHASE,
            message = "Defense successful! Drawing cards..."
        )
    }

    fun resolveFailedDefense(state: GameState): GameState {
        val allTableCards = state.tableSlots.flatMap { slot ->
            listOfNotNull(slot.attackCard, slot.defenseCard)
        }
        val defenderIdx = state.defenderIndex
        val updatedPlayers = state.players.toMutableList()
        val defender = updatedPlayers[defenderIdx]
        updatedPlayers[defenderIdx] = defender.copy(
            hand = defender.hand + allTableCards,
            skipNextAttack = true
        )
        val candidateNextAttacker = (state.defenderIndex + 1) % state.playerCount
        val (nextAttackerIdx, nextDefenderIdx) = resolveNextRoles(updatedPlayers.toList(), candidateNextAttacker, state.playerCount)
        return state.copy(
            players = updatedPlayers,
            tableSlots = emptyList(),
            phase = GamePhase.ATTACK_PHASE,
            attackerIndex = nextAttackerIdx,
            defenderIndex = nextDefenderIdx,
            selectedCards = emptySet(),
            selectedHandCardForDefense = null,
            throwInPassedIndices = emptySet(),
            message = "${defender.name} takes all cards. ${updatedPlayers[nextAttackerIdx].name} attacks!"
        )
    }

    fun replenish(state: GameState): GameState {
        val order = (0 until state.playerCount).map { (state.attackerIndex + it) % state.playerCount }
        var drawPile = state.drawPile
        val players = state.players.toMutableList()

        for (idx in order) {
            val player = players[idx]
            val needed = (8 - player.hand.size).coerceAtLeast(0)
            if (needed > 0 && drawPile.isNotEmpty()) {
                val drawn = drawPile.take(needed)
                drawPile = drawPile.drop(drawn.size)
                players[idx] = player.copy(hand = player.hand + drawn)
            }
        }

        val (nextAttackerIdx, nextDefenderIdx) = resolveNextRoles(players, state.defenderIndex, state.playerCount)

        return state.copy(
            players = players,
            drawPile = drawPile,
            phase = GamePhase.ATTACK_PHASE,
            attackerIndex = nextAttackerIdx,
            defenderIndex = nextDefenderIdx,
            message = "${players[nextAttackerIdx].name} attacks!"
        )
    }

    private fun resolveNextRoles(players: List<Player>, candidateAttackerIdx: Int, playerCount: Int): Pair<Int, Int> {
        val mutablePlayers = players.toMutableList()
        var attackerIdx = candidateAttackerIdx
        var checked = 0
        while (mutablePlayers[attackerIdx].skipNextAttack && checked < playerCount) {
            mutablePlayers[attackerIdx] = mutablePlayers[attackerIdx].copy(skipNextAttack = false)
            attackerIdx = (attackerIdx + 1) % playerCount
            checked++
        }
        val defenderIdx = (attackerIdx + 1) % playerCount
        return Pair(attackerIdx, defenderIdx)
    }

    fun applyJokerOnlyRule(state: GameState): GameState {
        val attacker = state.attacker
        if (state.drawPile.isNotEmpty()) return state
        if (attacker.hand.isEmpty()) return state
        if (!attacker.hand.all { it is Card.Joker }) return state
        if (state.discardPile.isEmpty()) return state

        val toDraw = state.discardPile.shuffled().take(3)
        val newDiscard = state.discardPile.filter { it !in toDraw }
        val updatedPlayers = state.players.toMutableList()
        val p = updatedPlayers[state.attackerIndex]
        updatedPlayers[state.attackerIndex] = p.copy(hand = p.hand + toDraw)
        return state.copy(
            players = updatedPlayers,
            discardPile = newDiscard,
            message = "${attacker.name} has only Jokers — drew ${toDraw.size} cards from discard"
        )
    }

    fun checkWinner(state: GameState): Int? {
        if (state.drawPile.isNotEmpty()) return null
        return state.players.firstOrNull { it.hand.isEmpty() }?.id
    }

    // Returns true if `hand` can cover every attack card in `attackCards` with a unique card each.
    private fun canAssignAll(hand: List<Card>, attackCards: List<Card>, trumpSuit: Suit): Boolean {
        if (attackCards.isEmpty()) return true
        if (hand.size < attackCards.size) return false
        val used = BooleanArray(hand.size)
        fun match(slotIdx: Int): Boolean {
            if (slotIdx == attackCards.size) return true
            val attack = attackCards[slotIdx]
            for (i in hand.indices) {
                if (!used[i] && canDefend(attack, hand[i], trumpSuit)) {
                    used[i] = true
                    if (match(slotIdx + 1)) return true
                    used[i] = false
                }
            }
            return false
        }
        return match(0)
    }

    // Returns true only if there exists a complete defense plan that uses `card` for some slot.
    fun cardCanParticipateInDefense(card: Card, hand: List<Card>, slots: List<TableSlot>, trumpSuit: Suit): Boolean {
        val undefended = slots.filter { it.defenseCard == null }
        for (targetSlot in undefended) {
            if (!canDefend(targetSlot.attackCard, card, trumpSuit)) continue
            val remainingHand = hand - setOf(card)
            val remainingAttacks = undefended.filter { it !== targetSlot }.map { it.attackCard }
            if (canAssignAll(remainingHand, remainingAttacks, trumpSuit)) return true
        }
        return false
    }

    fun getTableRanks(state: GameState): Set<Rank> {
        return state.tableSlots.flatMap { slot ->
            listOfNotNull(
                (slot.attackCard as? Card.SuitedCard)?.rank,
                (slot.defenseCard as? Card.SuitedCard)?.rank
            )
        }.toSet()
    }
}
