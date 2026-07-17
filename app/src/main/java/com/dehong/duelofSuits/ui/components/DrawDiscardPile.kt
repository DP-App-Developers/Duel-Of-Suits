package com.dehong.duelofSuits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.ui.animation.PositionKey
import com.dehong.duelofSuits.ui.animation.PositionRegistry
import com.dehong.duelofSuits.ui.theme.Gold

@Composable
fun DrawDiscardPiles(
    drawPileCount: Int,
    discardTopCard: Card?,
    registry: PositionRegistry,
    trumpCard: Card? = null,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        DrawPile(count = drawPileCount, registry = registry, trumpCard = trumpCard)
        DiscardPile(topCard = discardTopCard, registry = registry)
    }
}

@Composable
fun DrawPile(
    count: Int,
    registry: PositionRegistry,
    trumpCard: Card? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // spacedBy(-27): pile left edge lands at trump card's layout center (27dp),
            // so exactly half the rotated card's visual width (39dp of 78dp) is revealed
            horizontalArrangement = Arrangement.spacedBy((-27).dp)
        ) {
            // Trump card to the left (first = rendered behind the pile), rotated 90° (perpendicular)
            if (trumpCard != null && count > 0) {
                CardView(
                    card = trumpCard,
                    faceDown = false,
                    modifier = Modifier.rotate(90f)
                )
            }
            // Draw pile on top (second = rendered in front)
            Box(contentAlignment = Alignment.TopStart) {
                if (count > 0) {
                    Box(modifier = Modifier.offset { IntOffset(4, 4) }) {
                        CardView(card = Card.SuitedCard(Suit.SPADES, Rank.ACE), faceDown = true,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                registry.register(PositionKey.DrawPile, coords)
                            })
                    }
                    CardView(card = Card.SuitedCard(Suit.SPADES, Rank.ACE), faceDown = true,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            registry.register(PositionKey.DrawPile, coords)
                        })
                } else {
                    CardPlaceholder(modifier = Modifier.onGloballyPositioned { coords ->
                        registry.register(PositionKey.DrawPile, coords)
                    })
                }
                val badgeShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(-4, -4) }
                        .shadow(6.dp, badgeShape)
                        .background(Color(0xEE040C07), badgeShape)
                        .border(0.5.dp, Gold.copy(alpha = 0.30f), badgeShape)
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        color = Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                }
            }
        }
    }
}

@Composable
fun DiscardPile(
    topCard: Card?,
    registry: PositionRegistry,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "DISCARD",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        if (topCard != null) {
            CardView(
                card = topCard,
                faceDown = false,
                modifier = Modifier.onGloballyPositioned { coords ->
                    registry.register(PositionKey.DiscardPile, coords)
                }
            )
        } else {
            CardPlaceholder(modifier = Modifier.onGloballyPositioned { coords ->
                registry.register(PositionKey.DiscardPile, coords)
            })
        }
    }
}
