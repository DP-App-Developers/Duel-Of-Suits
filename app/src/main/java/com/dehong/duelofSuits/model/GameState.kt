package com.dehong.duelofSuits.model

enum class GamePhase {
    DEALING,
    ATTACK_PHASE,
    THROW_IN_PHASE,
    DEFENSE_PHASE,
    REPLENISH_PHASE,
    GAME_OVER
}

enum class CardSelectionState {
    NORMAL,
    SELECTED,
    HIGHLIGHTED,
    DISABLED,
    COMMITTED
}

data class TableSlot(
    val attackCard: Card,
    val defenseCard: Card? = null
)

data class GameState(
    val players: List<Player>,
    val drawPile: List<Card>,
    val discardPile: List<Card>,
    val tableSlots: List<TableSlot>,
    val phase: GamePhase,
    val attackerIndex: Int,
    val defenderIndex: Int,
    val selectedCards: Set<Card>,
    val selectedHandCardForDefense: Card?,
    val message: String,
    val playerCount: Int = 3,
    val trumpSuit: Suit = Suit.SPADES,
    val trumpCard: Card? = null,
    val defenderStartingHandCount: Int = 8,
    val winnerId: Int? = null,
    val throwInPassedIndices: Set<Int> = emptySet(),
    val animating: Boolean = false,
    val tableClearing: Boolean = false,
    // Pre-reserves table rows so the board can animate to the correct size before cards fly in.
    val reservedSlotCount: Int = 0
) {
    val otherIndices: List<Int> get() = (0 until playerCount).filter { it != attackerIndex && it != defenderIndex }
    val otherIndex: Int get() = otherIndices.firstOrNull() ?: -1
    val nonDefenderIndices: List<Int> get() = (0 until playerCount).filter { it != defenderIndex }
    val attacker: Player get() = players[attackerIndex]
    val defender: Player get() = players[defenderIndex]
    val undefendedSlots: List<TableSlot> get() = tableSlots.filter { it.defenseCard == null }
    val allSlotsDefended: Boolean get() = tableSlots.isNotEmpty() && undefendedSlots.isEmpty()
    val isHumanTurn: Boolean get() = when (phase) {
        GamePhase.ATTACK_PHASE -> attackerIndex == 0
        GamePhase.THROW_IN_PHASE -> {
            if (defenderIndex == 0 || 0 in throwInPassedIndices) false
            else {
                val startIdx = (attackerIndex + 1) % playerCount
                val nextPending = (0 until playerCount)
                    .map { (startIdx + it) % playerCount }
                    .filter { it != defenderIndex && it !in throwInPassedIndices }
                    .firstOrNull()
                nextPending == 0
            }
        }
        GamePhase.DEFENSE_PHASE -> defenderIndex == 0
        else -> false
    }
    val isHumanDefender: Boolean get() = defenderIndex == 0
    val isHumanAttacker: Boolean get() = attackerIndex == 0
    val isHumanOther: Boolean get() = defenderIndex != 0 && attackerIndex != 0
    val drawPileCount: Int get() = drawPile.size
}
