package com.dehong.duelofSuits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dehong.duelofSuits.game.AiPlayer
import com.dehong.duelofSuits.game.GameEngine
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.CardSelectionState
import com.dehong.duelofSuits.model.Deck
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Player
import com.dehong.duelofSuits.ui.animation.AnimationEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _animationEvents = MutableSharedFlow<AnimationEvent>(extraBufferCapacity = 32)
    val animationEvents: SharedFlow<AnimationEvent> = _animationEvents.asSharedFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        startGame()
    }

    private fun createInitialState(): GameState {
        val players = listOf(
            Player(id = 0, name = "You", isHuman = true),
            Player(id = 1, name = "Alex", isHuman = false),
            Player(id = 2, name = "Sam", isHuman = false)
        )
        return GameState(
            players = players,
            drawPile = emptyList(),
            discardPile = emptyList(),
            tableSlots = emptyList(),
            phase = GamePhase.DEALING,
            attackerIndex = 0,
            defenderIndex = 1,
            selectedCards = emptySet(),
            selectedHandCardForDefense = null,
            message = "Dealing cards..."
        )
    }

    private fun startGame() {
        viewModelScope.launch {
            val deck = Deck.create()
            val hands = List(3) { deck.drop(it * 8).take(8) }
            val drawPile = deck.drop(24)

            val players = listOf(
                Player(id = 0, name = "You", isHuman = true, hand = hands[0]),
                Player(id = 1, name = "Alex", isHuman = false, hand = hands[1]),
                Player(id = 2, name = "Sam", isHuman = false, hand = hands[2])
            )

            val attackerIdx = (0..2).random()
            val defenderIdx = (attackerIdx + 1) % 3

            for (i in 0 until 24) {
                val playerIdx = i % 3
                val cardIdx = i / 3
                _animationEvents.emit(
                    AnimationEvent.DealCard(
                        card = hands[playerIdx][cardIdx],
                        targetPlayerId = playerIdx,
                        cardIndex = cardIdx,
                        delayMs = i * 60L
                    )
                )
            }

            delay(24 * 60L + 400L)

            _gameState.value = GameState(
                players = players,
                drawPile = drawPile,
                discardPile = emptyList(),
                tableSlots = emptyList(),
                phase = GamePhase.ATTACK_PHASE,
                attackerIndex = attackerIdx,
                defenderIndex = defenderIdx,
                selectedCards = emptySet(),
                selectedHandCardForDefense = null,
                message = "${players[attackerIdx].name} attacks!"
            )

            checkAndRunAiTurn()
        }
    }

    fun restartGame() {
        _gameState.value = createInitialState()
        startGame()
    }

    // ── Human Input Handlers ────────────────────────────────────────────────

    fun onHumanCardTapped(card: Card) {
        val state = _gameState.value
        if (state.animating) return
        if (!state.isHumanTurn) return

        when (state.phase) {
            GamePhase.ATTACK_PHASE, GamePhase.THROW_IN_PHASE -> handleCardSelectionForAttack(card, state)
            GamePhase.DEFENSE_PHASE -> handleCardSelectionForDefense(card, state)
            else -> {}
        }
    }

    private fun handleCardSelectionForAttack(card: Card, state: GameState) {
        if (card is Card.Joker) {
            _errorMessage.value = "Jokers cannot be used to attack"
            return
        }
        val selected = state.selectedCards.toMutableSet()
        if (card in selected) {
            selected.remove(card)
        } else {
            val existingRank = selected.filterIsInstance<Card.SuitedCard>().firstOrNull()?.rank
            val newRank = (card as? Card.SuitedCard)?.rank
            if (existingRank != null && existingRank != newRank) {
                selected.clear()
            }
            selected.add(card)
        }
        _gameState.value = state.copy(selectedCards = selected)
    }

    private fun handleCardSelectionForDefense(card: Card, state: GameState) {
        if (state.selectedHandCardForDefense == card) {
            _gameState.value = state.copy(selectedHandCardForDefense = null)
            return
        }
        val validSlots = state.tableSlots.filter { slot ->
            slot.defenseCard == null && GameEngine.canDefend(slot.attackCard, card)
        }
        if (validSlots.isEmpty()) {
            _errorMessage.value = "This card cannot defend any attack card"
            return
        }
        if (validSlots.size == 1) {
            val attackCard = validSlots[0].attackCard
            val newState = GameEngine.processDefenseCard(attackCard, card, 0, state)
            _gameState.value = newState.copy(selectedHandCardForDefense = null)
            emitDefenseCardAnimation(card, 0, state.tableSlots.indexOf(validSlots[0]))
        } else {
            _gameState.value = state.copy(selectedHandCardForDefense = card)
        }
    }

    fun onDefenseSlotTapped(slotIndex: Int) {
        val state = _gameState.value
        if (state.animating) return
        val selectedCard = state.selectedHandCardForDefense ?: return
        val slot = state.tableSlots.getOrNull(slotIndex) ?: return
        if (slot.defenseCard != null) return
        if (!GameEngine.canDefend(slot.attackCard, selectedCard)) {
            _errorMessage.value = "This card cannot beat that attack"
            return
        }
        val newState = GameEngine.processDefenseCard(slot.attackCard, selectedCard, 0, state)
        _gameState.value = newState.copy(selectedHandCardForDefense = null)
        emitDefenseCardAnimation(selectedCard, 0, slotIndex)
    }

    fun onPlaySelectedPressed() {
        val state = _gameState.value
        if (state.animating || !state.isHumanTurn) return
        val selected = state.selectedCards.toList()

        when (state.phase) {
            GamePhase.ATTACK_PHASE -> {
                if (state.attackerIndex != 0) return
                val error = GameEngine.validateAttack(selected, state)
                if (error != null) { _errorMessage.value = error; return }
                viewModelScope.launch {
                    val newState = GameEngine.processAttack(selected, 0, state)
                    // Update state first so the new AttackSlot is registered in the position registry
                    // before the animation tries to look it up.
                    _gameState.value = newState.copy(attackerPassedThrowIn = true)
                    delay(50L)
                    emitPlayCardAnimations(selected, 0, state.tableSlots.size)
                    delay(300L * selected.size)
                    checkAndRunAiTurn()
                }
            }
            GamePhase.THROW_IN_PHASE -> {
                val isHumanAttacker = state.attackerIndex == 0
                val isHumanOther = state.otherIndex == 0
                if (!isHumanAttacker && !isHumanOther) return
                val error = GameEngine.validateThrowIn(selected, state)
                if (error != null) { _errorMessage.value = error; return }
                viewModelScope.launch {
                    var newState = GameEngine.processThrowIn(selected, 0, state)
                    // Auto-pass the human after throwing in — no need to press Pass.
                    newState = if (isHumanAttacker) {
                        newState.copy(attackerPassedThrowIn = true)
                    } else {
                        newState.copy(otherPassedThrowIn = true)
                    }
                    _gameState.value = newState
                    delay(50L)
                    emitPlayCardAnimations(selected, 0, state.tableSlots.size)
                    delay(300L * selected.size)
                    checkAndRunAiTurn()
                }
            }
            else -> {}
        }
    }

    fun onPassPressed() {
        val state = _gameState.value
        if (state.animating || !state.isHumanTurn) return
        if (state.phase != GamePhase.THROW_IN_PHASE) return

        val playerIdx = if (state.attackerIndex == 0) 0 else if (state.otherIndex == 0) 0 else return
        val newState = GameEngine.processPass(playerIdx, state)
        _gameState.value = newState.copy(selectedCards = emptySet())
        checkAndRunAiTurn()
    }

    fun onConfirmDefensePressed() {
        val state = _gameState.value
        if (state.animating || !state.isHumanDefender) return
        if (!state.allSlotsDefended) { _errorMessage.value = "Defend all cards first"; return }
        // Return to throw-in phase so non-defenders can add more cards before the turn ends.
        _gameState.value = state.copy(
            phase = GamePhase.THROW_IN_PHASE,
            attackerPassedThrowIn = false,
            otherPassedThrowIn = false,
            message = "Defended! Throw in more cards or pass"
        )
        checkAndRunAiTurn()
    }

    fun onTakeCardsPressed() {
        val state = _gameState.value
        if (state.animating || !state.isHumanDefender) return
        viewModelScope.launch {
            resolveFailedDefenseFlow(state)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ── AI Turn Logic ───────────────────────────────────────────────────────

    private fun checkAndRunAiTurn() {
        val state = _gameState.value
        if (state.phase == GamePhase.GAME_OVER || state.animating) return

        when (state.phase) {
            GamePhase.THROW_IN_PHASE -> {
                // Both non-defenders act independently; check pass flags to see if any AI still needs to act.
                val aiAttackerPending = state.attackerIndex != 0 && !state.attackerPassedThrowIn
                val aiOtherPending = state.otherIndex != 0 && !state.otherPassedThrowIn
                if (aiAttackerPending || aiOtherPending) {
                    viewModelScope.launch {
                        delay(800L)
                        runAiThrowIn(_gameState.value)
                    }
                }
            }
            GamePhase.REPLENISH_PHASE -> {
                // Both non-defenders passed in THROW_IN_PHASE with all slots defended → success.
                viewModelScope.launch {
                    resolveSuccessfulDefenseFlow(_gameState.value)
                }
            }
            else -> {
                if (state.isHumanTurn) return
                viewModelScope.launch {
                    delay(800L)
                    runAiAction()
                }
            }
        }
    }

    private fun runAiAction() {
        val state = _gameState.value
        if (state.phase == GamePhase.GAME_OVER) return

        viewModelScope.launch {
            when (state.phase) {
                GamePhase.ATTACK_PHASE -> runAiAttack(state)
                GamePhase.THROW_IN_PHASE -> runAiThrowIn(state)
                GamePhase.DEFENSE_PHASE -> runAiDefense(state)
                else -> {}
            }
        }
    }

    private suspend fun runAiAttack(state: GameState) {
        val attackerIdx = state.attackerIndex
        var currentState = GameEngine.applyJokerOnlyRule(state)
        if (currentState !== state) {
            _gameState.value = currentState
            delay(600L)
            currentState = _gameState.value
        }

        val cards = AiPlayer.decideAttackFromState(currentState, attackerIdx)
        if (cards.isEmpty()) {
            val newState = currentState.copy(
                phase = GamePhase.GAME_OVER,
                message = "No valid attack — game ends",
                winnerId = -1
            )
            _gameState.value = newState
            return
        }

        val newState = GameEngine.processAttack(cards, attackerIdx, currentState)
        _gameState.value = newState
        delay(50L)
        emitPlayCardAnimations(cards, attackerIdx, currentState.tableSlots.size)
        delay(300L * cards.size)

        delay(600L)
        checkAndRunAiTurn()
    }

    private suspend fun runAiThrowIn(state: GameState) {
        val nonDefenders = listOf(state.attackerIndex, state.otherIndex).filter { it != 0 }
        for (playerIdx in nonDefenders) {
            val currentState = _gameState.value
            if (currentState.phase != GamePhase.THROW_IN_PHASE) break
            if (currentState.animating) break

            // Snapshot human's pass state before AI may reset it via processThrowIn.
            val humanAttackerPassed = currentState.attackerIndex == 0 && currentState.attackerPassedThrowIn
            val humanOtherPassed = currentState.otherIndex == 0 && currentState.otherPassedThrowIn

            val cards = AiPlayer.decideThrowIn(
                currentState.players[playerIdx].hand,
                currentState.tableSlots,
                currentState.defender.hand.size
            )
            if (cards.isEmpty()) {
                val newState = GameEngine.processPass(playerIdx, currentState)
                _gameState.value = newState
            } else {
                var newState = GameEngine.processThrowIn(cards, playerIdx, currentState)
                // Restore human's implicit pass — processThrowIn resets both flags.
                if (humanAttackerPassed) newState = newState.copy(attackerPassedThrowIn = true)
                if (humanOtherPassed) newState = newState.copy(otherPassedThrowIn = true)
                _gameState.value = newState
                delay(50L)
                emitPlayCardAnimations(cards, playerIdx, currentState.tableSlots.size)
                delay(300L * cards.size)
                delay(500L)
            }
        }

        delay(300L)
        checkAndRunAiTurn()
    }

    private suspend fun runAiDefense(state: GameState) {
        val defenderIdx = state.defenderIndex
        if (defenderIdx == 0) return

        val defenseMap = AiPlayer.decideDefenseFromState(state)
        if (defenseMap == null) {
            delay(400L)
            resolveFailedDefenseFlow(state)
            return
        }

        var currentState = state
        for ((attackCard, defenseCard) in defenseMap) {
            val slotIdx = currentState.tableSlots.indexOfFirst {
                it.attackCard == attackCard && it.defenseCard == null
            }
            emitDefenseCardAnimation(defenseCard, defenderIdx, slotIdx)
            delay(350L)
            currentState = GameEngine.processDefenseCard(attackCard, defenseCard, defenderIdx, currentState)
            _gameState.value = currentState
        }

        // Return to throw-in phase so non-defenders can add more cards or pass.
        // Resolution only happens when both non-defenders pass with all slots defended.
        delay(500L)
        _gameState.value = currentState.copy(
            phase = GamePhase.THROW_IN_PHASE,
            attackerPassedThrowIn = false,
            otherPassedThrowIn = false,
            message = "Defended! Throw in more cards or pass"
        )
        checkAndRunAiTurn()
    }

    private suspend fun resolveSuccessfulDefenseFlow(state: GameState) {
        val resolvedState = GameEngine.resolveSuccessfulDefense(state)
        _animationEvents.emit(AnimationEvent.TableToDiscard(
            state.tableSlots.flatMap { listOfNotNull(it.attackCard, it.defenseCard) }
        ))
        delay(400L)
        _gameState.value = resolvedState

        delay(200L)
        val replenishedState = GameEngine.replenish(resolvedState)
        val winner = GameEngine.checkWinner(replenishedState)
        if (winner != null) {
            _gameState.value = replenishedState.copy(
                phase = GamePhase.GAME_OVER,
                winnerId = winner,
                message = "${replenishedState.players.first { it.id == winner }.name} wins!"
            )
            return
        }

        _animationEvents.emit(AnimationEvent.DrawCardFromPile(replenishedState.attackerIndex, 1))
        delay(600L)
        _gameState.value = replenishedState

        delay(400L)
        checkAndRunAiTurn()
    }

    private suspend fun resolveFailedDefenseFlow(state: GameState) {
        val allCards = state.tableSlots.flatMap { listOfNotNull(it.attackCard, it.defenseCard) }
        _animationEvents.emit(AnimationEvent.TableToPlayer(state.defenderIndex, allCards))
        delay(500L)
        val newState = GameEngine.resolveFailedDefense(state)

        val winner = GameEngine.checkWinner(newState)
        if (winner != null) {
            _gameState.value = newState.copy(
                phase = GamePhase.GAME_OVER,
                winnerId = winner,
                message = "${newState.players.first { it.id == winner }.name} wins!"
            )
            return
        }

        _gameState.value = newState
        delay(600L)
        checkAndRunAiTurn()
    }

    // ── Animation Helpers ───────────────────────────────────────────────────

    private fun emitPlayCardAnimations(cards: List<Card>, playerIdx: Int, startSlotIdx: Int) {
        viewModelScope.launch {
            cards.forEachIndexed { i, card ->
                delay(if (i == 0) 0L else 200L)
                _animationEvents.emit(
                    AnimationEvent.PlayCardToTable(
                        card = card,
                        fromPlayerId = playerIdx,
                        toSlotIndex = startSlotIdx + i
                    )
                )
            }
        }
    }

    private fun emitDefenseCardAnimation(card: Card, playerIdx: Int, slotIdx: Int) {
        viewModelScope.launch {
            _animationEvents.emit(
                AnimationEvent.DefenseCard(
                    card = card,
                    fromPlayerId = playerIdx,
                    toSlotIndex = slotIdx
                )
            )
        }
    }

    // ── Selection State Helpers ─────────────────────────────────────────────

    fun getCardSelectionState(card: Card, state: GameState): CardSelectionState {
        if (!state.isHumanTurn) return CardSelectionState.NORMAL
        return when (state.phase) {
            GamePhase.ATTACK_PHASE -> {
                when {
                    card is Card.Joker -> CardSelectionState.DISABLED
                    card in state.selectedCards -> CardSelectionState.SELECTED
                    else -> CardSelectionState.NORMAL
                }
            }
            GamePhase.THROW_IN_PHASE -> {
                when {
                    card is Card.Joker -> CardSelectionState.DISABLED
                    card in state.selectedCards -> CardSelectionState.SELECTED
                    else -> {
                        val tableRanks = GameEngine.getTableRanks(state)
                        val rank = (card as? Card.SuitedCard)?.rank
                        if (rank != null && rank in tableRanks) CardSelectionState.HIGHLIGHTED else CardSelectionState.DISABLED
                    }
                }
            }
            GamePhase.DEFENSE_PHASE -> {
                when {
                    card == state.selectedHandCardForDefense -> CardSelectionState.SELECTED
                    else -> {
                        val hasValidSlot = state.tableSlots.any { slot ->
                            slot.defenseCard == null && GameEngine.canDefend(slot.attackCard, card)
                        }
                        if (hasValidSlot) CardSelectionState.HIGHLIGHTED else CardSelectionState.DISABLED
                    }
                }
            }
            else -> CardSelectionState.NORMAL
        }
    }
}
