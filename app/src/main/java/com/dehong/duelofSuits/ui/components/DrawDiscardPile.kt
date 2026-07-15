package com.dehong.duelofSuits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.dehong.duelofSuits.ui.theme.CounterBackground
import com.dehong.duelofSuits.ui.theme.SelectedBorder

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
        Text(
            text = "DRAW",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
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
                Box(
                    modifier = Modifier
                        .background(CounterBackground, CircleShape)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                        .offset { IntOffset(4, (-4)) }
                ) {
                    Text(
                        text = "$count",
                        color = SelectedBorder,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Trump card shown face-up and rotated 90° to indicate it's at the bottom of the pile
            if (trumpCard != null && count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(CARD_HEIGHT, CARD_WIDTH),
                    contentAlignment = Alignment.Center
                ) {
                    CardView(
                        card = trumpCard,
                        faceDown = false,
                        modifier = Modifier.rotate(90f)
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
