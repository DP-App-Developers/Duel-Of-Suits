package com.dehong.duelofSuits.model

enum class Suit(val symbol: String, val isRed: Boolean) {
    CLUBS("♣", false),
    DIAMONDS("♦", true),
    HEARTS("♥", true),
    SPADES("♠", false)
}

enum class Rank(val displayName: String) {
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    TEN("10"),
    JACK("J"),
    QUEEN("Q"),
    KING("K"),
    ACE("A")
}

sealed class Card {
    data class SuitedCard(val suit: Suit, val rank: Rank) : Card()
    data class Joker(val id: Int) : Card()
}

fun Card.displayName(): String = when (this) {
    is Card.SuitedCard -> "${suit.symbol}${rank.displayName}"
    is Card.Joker -> "Joker"
}
