package com.dehong.duelofSuits.model

data class Player(
    val id: Int,
    val isHuman: Boolean,
    val hand: List<Card> = emptyList()
)
