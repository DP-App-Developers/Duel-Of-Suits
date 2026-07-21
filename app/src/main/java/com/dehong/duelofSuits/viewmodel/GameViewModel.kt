package com.dehong.duelofSuits.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dehong.duelofSuits.R
import com.dehong.duelofSuits.game.AiPlayer
import com.dehong.duelofSuits.game.AttackError
import com.dehong.duelofSuits.game.GameEngine
import com.dehong.duelofSuits.game.ThrowInError
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

// Time to wait after reserving slots before emitting fly animations, so the board
// resize animation (tween 350ms) finishes before cards start moving.
private const val RESIZE_ANIM_WAIT_MS = 400L

class GameViewModel(application: Application, private val playerCount: Int = 3) : AndroidViewModel(application) {

    // Must be initialized before _gameState, which calls createInitialState() → playerNames.
    private val playerNames: List<String> = listOf(
        application.getString(R.string.player_name_human),
        application.getString(R.string.player_name_ai_1),
        application.getString(R.string.player_name_ai_2),
        application.getString(R.string.player_name_ai_3)
    )

    private val _gameState = MutableStateFlow(createInitialState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _animationEvents = MutableSharedFlow<AnimationEvent>(extraBufferCapacity = 32)
    val animationEvents: SharedFlow<AnimationEvent> = _animationEvents.asSharedFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Tracks the column count computed by GameTable so animation sequencing uses the real value.
    private val _boardNumCols = MutableStateFlow(4)
    val boardNumCols: StateFlow<Int> = _boardNumCols.asStateFlow()

    fun updateBoardNumCols(cols: Int) {
        if (_boardNumCols.value != cols) _boardNumCols.value = cols
    }

    init {
        startGame()
    }

    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
    private fun getString(resId: Int, vararg args: Any): String = getApplication<Application>().getString(resId, *args)

    private fun AttackError.toMessage(): String = when (this) {
        AttackError.EmptySelection -> getString(R.string.error_select_to_attack)
        AttackError.JokerForbidden -> getString(R.string.error_joker_cannot_attack)
        AttackError.MixedRanks -> getString(R.string.error_same_rank_required)
        AttackError.TooManyCards -> getString(R.string.error_max_four_cards)
        is AttackError.ExceedsDefenderHand -> getString(R.string.error_too_many_attack_cards, defenderHandSize)
    }

    private fun ThrowInError.toMessage(): String = when (this) {
        ThrowInError.EmptySelection -> getString(R.string.error_select_to_throw_in)
        ThrowInError.JokerForbidden -> getString(R.string.error_joker_cannot_throw_in)
        ThrowInError.RankMismatch -> getString(R.string.error_thrown_must_match_rank)
        is ThrowInError.ExceedsLimit -> getString(R.string.error_too_many_throw_in_cards, startCount)
    }

    private fun createInitialState(): GameState {
        val players = (0 until playerCount).map { i ->
            Player(id = i, name = playerNames[i], isHuman = i == 0)
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
            message = ""
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
                Player(id = i, name = playerNames[i], isHuman = i == 0, hand = hands[i])
            }

            val attackerIdx = (0 until playerCount).random()
            val defenderIdx = (attackerIdx + 1) % playerCount

            val totalCards = playerCount * 8
            // Show the draw pile and trump card before dealing so it doesn't look empty
            _gameState.value = _gameState.value.copy(
                drawPile = deck,
                trumpCard = trumpCard,
                trumpSuit = trumpSuit
            )
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
                message = "",
                trumpSuit = trumpSuit,
                trumpCard = trumpCard
            )

            // Wait for the hand fan-out animation (460ms) to finish before announcing
            delay(550L)
            _animationEvents.emit(AnimationEvent.GameStarting(attackerIdx))
            // AI has an 800ms think delay, so starting 200ms after the bubble means
            // cards land ~1 second after the bubble appears
            delay(200L)
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
            _errorMessage.value = getString(R.string.error_joker_cannot_attack)
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
            _errorMessage.value = getString(R.string.error_joker_cannot_throw_in)
            return
        }
        val rank = (card as? Card.SuitedCard)?.rank
        val tableRanks = GameEngine.getTableRanks(state)
        if (rank == null || rank !in tableRanks) {
            _errorMessage.value = getString(R.string.error_card_must_match_table_rank)
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
            _errorMessage.value = getString(R.string.error_card_cannot_defend_any)
            return
        }
        if (validSlots.size == 1) {
            val attackCard = validSlots[0].attackCard
            val slotIdx = state.tableSlots.indexOf(validSlots[0])
            val newState = GameEngine.processDefenseCard(attackCard, card, 0, state)
            val updatedPlayers = state.players.toMutableList()
            updatedPlayers[0] = updatedPlayers[0].copy(hand = updatedPlayers[0].hand - setOf(card))
            _gameState.value = state.copy(players = updatedPlayers, selectedHandCardForDefense = null, animating = true)
            emitDefenseCardAnimation(card, 0, slotIdx)
            viewModelScope.launch {
                delay(500L)
                val finalState = newState.copy(selectedHandCardForDefense = null, animating = false)
                _gameState.value = finalState
                if (finalState.allSlotsDefended) autoAdvanceDefense()
            }
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
            _errorMessage.value = getString(R.string.error_card_cannot_defend_that)
            return
        }
        val newState = GameEngine.processDefenseCard(slot.attackCard, selectedCard, 0, state)
        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[0] = updatedPlayers[0].copy(hand = updatedPlayers[0].hand - setOf(selectedCard))
        _gameState.value = state.copy(players = updatedPlayers, selectedHandCardForDefense = null, animating = true)
        emitDefenseCardAnimation(selectedCard, 0, slotIndex)
        viewModelScope.launch {
            delay(500L)
            val finalState = newState.copy(selectedHandCardForDefense = null, animating = false)
            _gameState.value = finalState
            if (finalState.allSlotsDefended) autoAdvanceDefense()
        }
    }

    fun onPlaySelectedPressed() {
        val state = _gameState.value
        if (state.animating || !state.isHumanTurn) return
        val selected = state.selectedCards.toList()

        when (state.phase) {
            GamePhase.ATTACK_PHASE -> {
                if (state.attackerIndex != 0) return
                val error = GameEngine.validateAttack(selected, state)
                if (error != null) { _errorMessage.value = error.toMessage(); return }
                viewModelScope.launch {
                    val newState = GameEngine.processAttack(selected, 0, state)
                    _gameState.value = state.copy(players = newState.players, selectedCards = emptySet(), animating = true)
                    emitPlayCardAnimations(selected, 0, state.tableSlots.size)
                    delay(500L)
                    val finalState = newState.copy(animating = false, reservedSlotCount = 0)
                    if (checkAndApplyImmediateWin(finalState)) return@launch
                    _gameState.value = finalState
                    checkAndRunAiTurn()
                }
            }
            GamePhase.THROW_IN_PHASE -> {
                if (state.defenderIndex == 0) return
                val error = GameEngine.validateThrowIn(selected, state)
                if (error != null) { _errorMessage.value = error.toMessage(); return }
                viewModelScope.launch {
                    val newState = GameEngine.processThrowIn(selected, 0, state)
                    _gameState.value = state.copy(players = newState.players, selectedCards = emptySet(), animating = true)
                    emitPlayCardAnimations(selected, 0, state.tableSlots.size)
                    delay(500L)
                    val finalState = newState.copy(animating = false, reservedSlotCount = 0)
                    if (checkAndApplyImmediateWin(finalState)) return@launch
                    _gameState.value = finalState
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
            val state = _gameState.value
            if (checkAndApplyImmediateWin(state)) return@launch
            val nextPhaseState = if (state.defender.hand.isEmpty()) {
                state.copy(
                    phase = GamePhase.REPLENISH_PHASE,
                    throwInPassedIndices = emptySet(),
                    message = ""
                )
            } else {
                state.copy(
                    phase = GamePhase.THROW_IN_PHASE,
                    throwInPassedIndices = emptySet(),
                    message = ""
                )
            }
            _gameState.value = nextPhaseState
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

    private fun checkAndApplyImmediateWin(state: GameState): Boolean {
        val winner = GameEngine.checkWinner(state) ?: return false
        _gameState.value = state.copy(
            phase = GamePhase.GAME_OVER,
            winnerId = winner,
            message = ""
        )
        return true
    }

    // ── AI Turn Logic ───────────────────────────────────────────────────────

    private fun checkAndRunAiTurn() {
        val state = _gameState.value
        if (state.phase == GamePhase.GAME_OVER || state.animating) return

        when (state.phase) {
            GamePhase.THROW_IN_PHASE -> {
                val startIdx = (state.attackerIndex + 1) % state.playerCount
                val nextPlayer = (0 until state.playerCount)
                    .map { (startIdx + it) % state.playerCount }
                    .filter { it != state.defenderIndex }
                    .firstOrNull { it !in state.throwInPassedIndices }
                if (nextPlayer != null && nextPlayer != 0) {
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
                if (state.isHumanTurn) {
                    if (state.phase == GamePhase.ATTACK_PHASE &&
                        state.attacker.hand.isNotEmpty() &&
                        state.attacker.hand.all { it is Card.Joker }
                    ) {
                        viewModelScope.launch {
                            val fixed = GameEngine.applyJokerOnlyRule(state)
                            if (fixed !== state) {
                                _animationEvents.emit(AnimationEvent.JokerOnly(state.attackerIndex))
                                _gameState.value = fixed
                            } else {
                                delay(800L)
                                _gameState.value = GameEngine.skipAttackerRound(state)
                            }
                            checkAndRunAiTurn()
                        }
                    }
                    return
                }
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
            _animationEvents.emit(AnimationEvent.JokerOnly(attackerIdx))
            _gameState.value = currentState
            delay(600L)
            currentState = _gameState.value
        }

        val cards = AiPlayer.decideAttackFromState(currentState, attackerIdx)
        if (cards.isEmpty()) {
            _gameState.value = GameEngine.skipAttackerRound(currentState)
            delay(600L)
            checkAndRunAiTurn()
            return
        }

        val newState = GameEngine.processAttack(cards, attackerIdx, currentState)
        _gameState.value = currentState.copy(players = newState.players, selectedCards = emptySet(), animating = true)
        emitPlayCardAnimations(cards, attackerIdx, currentState.tableSlots.size)
        delay(500L)
        val finalState = newState.copy(animating = false, reservedSlotCount = 0)
        if (checkAndApplyImmediateWin(finalState)) return
        _gameState.value = finalState

        delay(600L)
        checkAndRunAiTurn()
    }

    private suspend fun runAiThrowIn(state: GameState) {
        val startIdx = (state.attackerIndex + 1) % state.playerCount
        val throwInOrder = (0 until state.playerCount)
            .map { (startIdx + it) % state.playerCount }
            .filter { it != state.defenderIndex }

        val playerIdx = throwInOrder.firstOrNull { it !in state.throwInPassedIndices }
        if (playerIdx == null || playerIdx == 0) {
            delay(300L)
            checkAndRunAiTurn()
            return
        }

        val currentState = _gameState.value
        if (currentState.phase != GamePhase.THROW_IN_PHASE || currentState.animating) return
        if (playerIdx in currentState.throwInPassedIndices) {
            delay(300L)
            checkAndRunAiTurn()
            return
        }

        val cards = AiPlayer.decideThrowInFromState(currentState, playerIdx)
        if (cards.isEmpty()) {
            val newState = GameEngine.processPass(playerIdx, currentState)
            _gameState.value = newState
            _animationEvents.emit(AnimationEvent.PlayerPassed(playerIdx))
        } else {
            val newState = GameEngine.processThrowIn(cards, playerIdx, currentState)
            _gameState.value = currentState.copy(players = newState.players, selectedCards = emptySet(), animating = true)
            emitPlayCardAnimations(cards, playerIdx, currentState.tableSlots.size)
            delay(500L)
            val finalState = newState.copy(animating = false, reservedSlotCount = 0)
            if (checkAndApplyImmediateWin(finalState)) return
            _gameState.value = finalState
            delay(300L)
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
            if (checkAndApplyImmediateWin(currentState)) return
        }

        delay(500L)
        val nextPhaseState = if (currentState.defender.hand.isEmpty()) {
            currentState.copy(
                phase = GamePhase.REPLENISH_PHASE,
                throwInPassedIndices = emptySet(),
                message = ""
            )
        } else {
            currentState.copy(
                phase = GamePhase.THROW_IN_PHASE,
                throwInPassedIndices = emptySet(),
                message = ""
            )
        }
        _gameState.value = nextPhaseState
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
                message = ""
            )
            return
        }

        if (resolvedState.drawPile.isNotEmpty()) {
            _animationEvents.emit(AnimationEvent.DrawCardFromPile(replenishedState.attackerIndex, 1))
            delay(600L)
        }
        _gameState.value = replenishedState

        delay(400L)
        checkAndRunAiTurn()
    }

    private suspend fun resolveFailedDefenseFlow(state: GameState) {
        val allCards = state.tableSlots.flatMap { listOfNotNull(it.attackCard, it.defenseCard) }
        _animationEvents.emit(AnimationEvent.PlayerTookCards(state.defenderIndex))
        delay(1000L)
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
                message = ""
            )
            return
        }

        _gameState.value = newState
        delay(600L)
        checkAndRunAiTurn()
    }

    // ── Animation Helpers ───────────────────────────────────────────────────

    // Emits flying-card events one by one (200 ms apart). If the new cards require an extra row,
    // first reserves the slots so the board can animate to the new size, then waits for the
    // resize to finish before the first card event is emitted.
    private suspend fun emitPlayCardAnimations(cards: List<Card>, playerIdx: Int, startSlotIdx: Int) {
        val numCols = _boardNumCols.value.coerceAtLeast(1)
        val oldRows = rowsNeeded(startSlotIdx, numCols)
        val newRows = rowsNeeded(startSlotIdx + cards.size, numCols)
        if (newRows > oldRows) {
            _gameState.value = _gameState.value.copy(reservedSlotCount = startSlotIdx + cards.size)
            delay(RESIZE_ANIM_WAIT_MS)
        }
        cards.forEachIndexed { i, card ->
            if (i > 0) delay(200L)
            _animationEvents.emit(
                AnimationEvent.PlayCardToTable(
                    card = card,
                    fromPlayerId = playerIdx,
                    toSlotIndex = startSlotIdx + i
                )
            )
        }
    }

    private fun rowsNeeded(slotCount: Int, numCols: Int): Int =
        if (slotCount == 0) 1 else (slotCount + numCols - 1) / numCols

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
        if (!state.isHumanTurn) return CardSelectionState.DISABLED
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

class GameViewModelFactory(private val application: Application, private val playerCount: Int) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(application, playerCount) as T
    }
}
