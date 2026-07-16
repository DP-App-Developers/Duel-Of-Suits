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
        val shuffled = cards.shuffled().toMutableList()
        // Trump card is the last card; it must have a suit, so ensure no Joker ends up last.
        if (shuffled.last() is Card.Joker) {
            val lastSuitedIdx = shuffled.indexOfLast { it is Card.SuitedCard }
            val last = shuffled.size - 1
            shuffled[lastSuitedIdx] = shuffled[last].also { shuffled[last] = shuffled[lastSuitedIdx] }
        }
        return shuffled
    }
}
