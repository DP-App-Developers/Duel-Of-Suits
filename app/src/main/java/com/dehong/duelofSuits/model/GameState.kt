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
    val winnerId: Int? = null,
    val attackerPassedThrowIn: Boolean = false,
    val otherPassedThrowIn: Boolean = false,
    val animating: Boolean = false
) {
    val otherIndex: Int get() = (0 + 1 + 2) - attackerIndex - defenderIndex
    val attacker: Player get() = players[attackerIndex]
    val defender: Player get() = players[defenderIndex]
    val otherPlayer: Player get() = players[otherIndex]
    val undefendedSlots: List<TableSlot> get() = tableSlots.filter { it.defenseCard == null }
    val allSlotsDefended: Boolean get() = tableSlots.isNotEmpty() && undefendedSlots.isEmpty()
    val isHumanTurn: Boolean get() = when (phase) {
        GamePhase.ATTACK_PHASE -> attackerIndex == 0
        GamePhase.THROW_IN_PHASE -> attackerIndex == 0 || otherIndex == 0
        GamePhase.DEFENSE_PHASE -> defenderIndex == 0
        else -> false
    }
    val isHumanDefender: Boolean get() = defenderIndex == 0
    val isHumanAttacker: Boolean get() = attackerIndex == 0
    val isHumanOther: Boolean get() = otherIndex == 0
    val drawPileCount: Int get() = drawPile.size
}
