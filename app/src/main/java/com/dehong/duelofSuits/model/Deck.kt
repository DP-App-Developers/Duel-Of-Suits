package com.dehong.duelofSuits.model

object Deck {
    fun create(): List<Card> {
        val cards = mutableListOf<Card>()
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                cards.add(Card.SuitedCard(suit, rank))
            }
        }
        cards.add(Card.Joker(1))
        cards.add(Card.Joker(2))
        return cards.shuffled()
    }
}
