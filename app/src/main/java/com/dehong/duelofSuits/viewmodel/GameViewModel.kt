package com.dehong.duelofSuits.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dehong.duelofSuits.game.AiPlayer
import com.dehong.duelofSuits.game.GameEngine
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.CardSelectionState
import com.dehong.duelofSuits.model.Deck
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.model.Player
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.ui.animation.AnimationEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val PLAYER_NAMES = listOf("You", "Alex", "Sam", "Ryan")

class GameViewModel(private val playerCount: Int = 3) : ViewModel() {

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
        val players = (0 until playerCount).map { i ->
            Player(id = i, name = PLAYER_NAMES[i], isHuman = i == 0)
        }
        return GameState(
            players = players,
            drawPile = emptyList(),
            discardPile = emptyList(),
            tableSlots = emptyList(),
            phase = GamePhase.DEALING,
            playerCount = playerCount,
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
            val hands = List(playerCount) { deck.drop(it * 8).take(8) }
            val drawPile = deck.drop(playerCount * 8)

            val trumpCard = drawPile.lastOrNull()
            val trumpSuit = (trumpCard as? Card.SuitedCard)?.suit ?: Suit.SPADES

            val players = (0 until playerCount).map { i ->
                Player(id = i, name = PLAYER_NAMES[i], isHuman = i == 0, hand = hands[i])
            }

            val attackerIdx = (0 until playerCount).random()
            val defenderIdx = (attackerIdx + 1) % playerCount

            val totalCards = playerCount * 8
            delay(400L) // wait for UI layout pass so player area positions are registered
            for (i in 0 until totalCards) {
                val playerIdx = i % playerCount
                val cardIdx = i / playerCount
                _animationEvents.emit(
                    AnimationEvent.DealCard(
                        card = hands[playerIdx][cardIdx],
                        targetPlayerId = playerIdx,
                        cardIndex = cardIdx,
                        delayMs = i * 60L
                    )
                )
            }

            delay(totalCards * 60L + 650L)

            _gameState.value = GameState(
                players = players,
                drawPile = drawPile,
                discardPile = emptyList(),
                tableSlots = emptyList(),
                phase = GamePhase.ATTACK_PHASE,
                playerCount = playerCount,
                attackerIndex = attackerIdx,
                defenderIndex = defenderIdx,
                selectedCards = emptySet(),
                selectedHandCardForDefense = null,
                message = "${players[attackerIdx].name} attacks!",
                trumpSuit = trumpSuit,
                trumpCard = trumpCard
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
            GamePhase.ATTACK_PHASE -> handleCardSelectionForAttack(card, state)
            GamePhase.THROW_IN_PHASE -> handleCardSelectionForThrowIn(card, state)
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

    private fun handleCardSelectionForThrowIn(card: Card, state: GameState) {
        if (card is Card.Joker) {
            _errorMessage.value = "Jokers cannot be thrown in"
            return
        }
        val rank = (card as? Card.SuitedCard)?.rank
        val tableRanks = GameEngine.getTableRanks(state)
        if (rank == null || rank !in tableRanks) {
            _errorMessage.value = "Card must match a rank already on the table"
            return
        }
        val selected = state.selectedCards.toMutableSet()
        if (card in selected) selected.remove(card) else selected.add(card)
        _gameState.value = state.copy(selectedCards = selected)
    }

    private fun handleCardSelectionForDefense(card: Card, state: GameState) {
        if (state.selectedHandCardForDefense == card) {
            _gameState.value = state.copy(selectedHandCardForDefense = null)
            return
        }
        val validSlots = state.tableSlots.filter { slot ->
            slot.defenseCard == null && GameEngine.canDefend(slot.attackCard, card, state.trumpSuit)
        }
        if (validSlots.isEmpty()) {
            _errorMessage.value = "This card cannot defend any attack card"
            return
        }
        if (validSlots.size == 1) {
            val attackCard = validSlots[0].attackCard
            val newState = GameEngine.processDefenseCard(attackCard, card, 0, state)
            val updated = newState.copy(selectedHandCardForDefense = null)
            _gameState.value = updated
            emitDefenseCardAnimation(card, 0, state.tableSlots.indexOf(validSlots[0]))
            if (updated.allSlotsDefended) autoAdvanceDefense()
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
        if (!GameEngine.canDefend(slot.attackCard, selectedCard, state.trumpSuit)) {
            _errorMessage.value = "This card cannot beat that attack"
            return
        }
        val newState = GameEngine.processDefenseCard(slot.attackCard, selectedCard, 0, state)
        val updated = newState.copy(selectedHandCardForDefense = null)
        _gameState.value = updated
        emitDefenseCardAnimation(selectedCard, 0, slotIndex)
        if (updated.allSlotsDefended) autoAdvanceDefense()
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
                    _gameState.value = newState.copy(animating = true)
                    delay(50L)
                    emitPlayCardAnimations(selected, 0, state.tableSlots.size)
                    delay(300L * selected.size)
                    _gameState.value = _gameState.value.copy(animating = false)
                    checkAndRunAiTurn()
                }
            }
            GamePhase.THROW_IN_PHASE -> {
                if (state.defenderIndex == 0) return
                val error = GameEngine.validateThrowIn(selected, state)
                if (error != null) { _errorMessage.value = error; return }
                viewModelScope.launch {
                    val newState = GameEngine.processThrowIn(selected, 0, state)
                    _gameState.value = newState.copy(animating = true)
                    delay(50L)
                    emitPlayCardAnimations(selected, 0, state.tableSlots.size)
                    delay(300L * selected.size)
                    _gameState.value = _gameState.value.copy(animating = false)
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

        val newState = GameEngine.processPass(0, state)
        _gameState.value = newState.copy(selectedCards = emptySet())
        viewModelScope.launch { _animationEvents.emit(AnimationEvent.PlayerPassed(0)) }
        checkAndRunAiTurn()
    }

    private fun autoAdvanceDefense() {
        viewModelScope.launch {
            delay(500L)
            _gameState.value = _gameState.value.copy(
                phase = GamePhase.THROW_IN_PHASE,
                throwInPassedIndices = emptySet(),
                message = "Defended! Throw in more cards or pass"
            )
            checkAndRunAiTurn()
        }
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
                val aiNonDefendersPending = state.nonDefenderIndices
                    .filter { it != 0 && it !in state.throwInPassedIndices }
                if (aiNonDefendersPending.isNotEmpty()) {
                    viewModelScope.launch {
                        delay(800L)
                        runAiThrowIn(_gameState.value)
                    }
                }
            }
            GamePhase.REPLENISH_PHASE -> {
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
        _gameState.value = newState.copy(animating = true)
        delay(50L)
        emitPlayCardAnimations(cards, attackerIdx, currentState.tableSlots.size)
        delay(300L * cards.size)
        _gameState.value = _gameState.value.copy(animating = false)

        delay(600L)
        checkAndRunAiTurn()
    }

    private suspend fun runAiThrowIn(state: GameState) {
        // others act before attacker; all must be AI players not yet passed
        val orderedAiNonDefenders = (state.otherIndices + listOf(state.attackerIndex))
            .filter { it != 0 }

        for (playerIdx in orderedAiNonDefenders) {
            val currentState = _gameState.value
            if (currentState.phase != GamePhase.THROW_IN_PHASE) break
            if (currentState.animating) break
            if (playerIdx in currentState.throwInPassedIndices) continue

            val cards = AiPlayer.decideThrowInFromState(currentState, playerIdx)
            if (cards.isEmpty()) {
                val newState = GameEngine.processPass(playerIdx, currentState)
                _gameState.value = newState
                _animationEvents.emit(AnimationEvent.PlayerPassed(playerIdx))
            } else {
                val newState = GameEngine.processThrowIn(cards, playerIdx, currentState)
                _gameState.value = newState.copy(animating = true)
                delay(50L)
                emitPlayCardAnimations(cards, playerIdx, currentState.tableSlots.size)
                delay(300L * cards.size)
                _gameState.value = _gameState.value.copy(animating = false)
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

        delay(500L)
        _gameState.value = currentState.copy(
            phase = GamePhase.THROW_IN_PHASE,
            throwInPassedIndices = emptySet(),
            message = "Defended! Throw in more cards or pass"
        )
        checkAndRunAiTurn()
    }

    private suspend fun resolveSuccessfulDefenseFlow(state: GameState) {
        _gameState.value = state.copy(tableClearing = true)
        delay(750L)
        val resolvedState = GameEngine.resolveSuccessfulDefense(state)
        _gameState.value = resolvedState

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
        var newState = GameEngine.resolveFailedDefense(state)

        // Replenish after failed defense: draw in clockwise order from old attacker
        val drawOrder = (0 until state.playerCount).map { (state.attackerIndex + it) % state.playerCount }
        var drawPile = newState.drawPile
        val players = newState.players.toMutableList()
        for (idx in drawOrder) {
            val player = players[idx]
            val needed = (8 - player.hand.size).coerceAtLeast(0)
            if (needed > 0 && drawPile.isNotEmpty()) {
                val drawn = drawPile.take(needed)
                drawPile = drawPile.drop(drawn.size)
                players[idx] = player.copy(hand = player.hand + drawn)
            }
        }
        newState = newState.copy(players = players, drawPile = drawPile)

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
                        val hand = state.players[0].hand
                        if (GameEngine.cardCanParticipateInDefense(card, hand, state.tableSlots, state.trumpSuit)) {
                            CardSelectionState.HIGHLIGHTED
                        } else {
                            CardSelectionState.DISABLED
                        }
                    }
                }
            }
            else -> CardSelectionState.NORMAL
        }
    }
}

class GameViewModelFactory(private val playerCount: Int) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(playerCount) as T
    }
}
