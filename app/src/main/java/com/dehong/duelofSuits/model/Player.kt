package com.dehong.duelofSuits.model

data class Player(
    val id: Int,
    val name: String,
    val isHuman: Boolean,
    val hand: List<Card> = emptyList(),
    val skipNextAttack: Boolean = false
)
