package com.dehong.duelofSuits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.model.Card
import com.dehong.duelofSuits.model.CardSelectionState
import com.dehong.duelofSuits.model.Rank
import com.dehong.duelofSuits.model.Suit
import com.dehong.duelofSuits.ui.theme.CardBackBlue
import com.dehong.duelofSuits.ui.theme.CardBackBlueLight
import com.dehong.duelofSuits.ui.theme.CardFace
import com.dehong.duelofSuits.ui.theme.CardRed
import com.dehong.duelofSuits.ui.theme.CardBlack
import com.dehong.duelofSuits.ui.theme.DisabledGray
import com.dehong.duelofSuits.ui.theme.Gold
import com.dehong.duelofSuits.ui.theme.HighlightCyan
import com.dehong.duelofSuits.ui.theme.HighlightCyanOverlay
import com.dehong.duelofSuits.ui.theme.SelectedBorder

val CARD_WIDTH  = 54.dp
val CARD_HEIGHT = 78.dp

val LocalCardWidth  = compositionLocalOf { CARD_WIDTH }
val LocalCardHeight = compositionLocalOf { CARD_HEIGHT }

private val CARD_SHAPE = RoundedCornerShape(8.dp)

@Composable
fun CardView(
    card: Card,
    modifier: Modifier = Modifier,
    faceDown: Boolean = false,
    selectionState: CardSelectionState = CardSelectionState.NORMAL,
    elevation: Dp = 4.dp
) {
    val borderColor = when (selectionState) {
        CardSelectionState.SELECTED    -> SelectedBorder
        CardSelectionState.HIGHLIGHTED -> HighlightCyan
        CardSelectionState.COMMITTED   -> Color(0xFF00C853)
        else                           -> Color(0xFF3A3A3A)
    }
    val borderWidth = when (selectionState) {
        CardSelectionState.SELECTED                              -> 2.5.dp
        CardSelectionState.HIGHLIGHTED, CardSelectionState.COMMITTED -> 2.dp
        else                                                     -> 0.dp
    }
    val alpha = if (selectionState == CardSelectionState.DISABLED) 0.3f else 1f

    val cardWidth  = LocalCardWidth.current
    val cardHeight = LocalCardHeight.current
    Surface(
        shape = CARD_SHAPE,
        shadowElevation = when (selectionState) {
            CardSelectionState.SELECTED    -> 12.dp
            CardSelectionState.HIGHLIGHTED -> 8.dp
            else                           -> elevation
        },
        color = if (faceDown) CardBackBlue else CardFace,
        modifier = modifier
            .size(cardWidth, cardHeight)
            .then(
                if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, CARD_SHAPE)
                else Modifier
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when (selectionState) {
                        CardSelectionState.DISABLED    -> Modifier.background(DisabledGray)
                        CardSelectionState.HIGHLIGHTED -> Modifier.background(HighlightCyanOverlay)
                        else                           -> Modifier
                    }
                )
        ) {
            if (faceDown) {
                CardBack(alpha)
            } else {
                when (card) {
                    is Card.SuitedCard -> CardFrontSuited(card, alpha)
                    is Card.Joker      -> CardFrontJoker(card, alpha)
                }
            }
        }
    }
}

@Composable
private fun CardBack(alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(CardBackBlue, CardBackBlueLight))
            )
            .drawWithContent {
                drawContent()
                drawCardBackPattern()
            }
    )
}

private fun DrawScope.drawCardBackPattern() {
    val gold = Gold
    val spacing = 7.dp.toPx()

    // Diamond cross-hatch: two diagonal directions
    val lineAlpha = gold.copy(alpha = 0.14f)
    var x = -size.height
    while (x < size.width + size.height) {
        drawLine(lineAlpha, Offset(x, 0f), Offset(x + size.height, size.height), 0.8f)
        x += spacing
    }
    x = -size.height
    while (x < size.width + size.height) {
        drawLine(lineAlpha, Offset(x + size.height, 0f), Offset(x, size.height), 0.8f)
        x += spacing
    }

    // Outer gold border
    val outerInset = 3.5f.dp.toPx()
    drawRoundRect(
        color = gold.copy(alpha = 0.55f),
        topLeft = Offset(outerInset, outerInset),
        size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
        cornerRadius = CornerRadius(5.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Inner gold border
    val innerInset = 6.5f.dp.toPx()
    drawRoundRect(
        color = gold.copy(alpha = 0.35f),
        topLeft = Offset(innerInset, innerInset),
        size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = Stroke(width = 0.8.dp.toPx())
    )

    // Center diamond ornament
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = 7.dp.toPx()
    val diamond = Path().apply {
        moveTo(cx, cy - r); lineTo(cx + r, cy)
        lineTo(cx, cy + r); lineTo(cx - r, cy)
        close()
    }
    drawPath(diamond, gold.copy(alpha = 0.30f), style = Stroke(0.9.dp.toPx()))
}

@Composable
private fun CardFrontSuited(card: Card.SuitedCard, alpha: Float) {
    val textColor = if (card.suit.isRed) CardRed else CardBlack
    val scale = LocalCardWidth.current.value / CARD_WIDTH.value
    val rankSp  = (14f * scale).sp
    val suitSp  = (11f * scale).sp
    val centerSp = (22f * scale).sp
    val padH = (3f * scale).dp
    val padV = (2f * scale).dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CardFace)
            .drawWithContent {
                drawContent()
                // Subtle ivory inner border
                val inset = 2.5f.dp.toPx()
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.07f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = padH, top = padV)
        ) {
            Text(
                text = card.rank.displayName,
                color = textColor.copy(alpha = alpha),
                fontSize = rankSp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = rankSp
            )
            Text(
                text = card.suit.symbol,
                color = textColor.copy(alpha = alpha),
                fontSize = suitSp,
                lineHeight = suitSp
            )
        }
        Text(
            text = card.suit.symbol,
            color = textColor.copy(alpha = alpha),
            fontSize = centerSp,
            modifier = Modifier.align(Alignment.Center)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = padH, bottom = padV),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.suit.symbol,
                color = textColor.copy(alpha = alpha),
                fontSize = suitSp,
                lineHeight = suitSp
            )
            Text(
                text = card.rank.displayName,
                color = textColor.copy(alpha = alpha),
                fontSize = rankSp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = rankSp
            )
        }
    }
}

@Composable
private fun CardFrontJoker(card: Card.Joker, alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CardFace)
            .drawWithContent {
                drawContent()
                val inset = 2.5f.dp.toPx()
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.07f),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                    style = Stroke(width = 0.8.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🃏", fontSize = 22.sp)
            Text(
                text = "JOKER",
                color = Color(0xFF4A148C).copy(alpha = alpha),
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun CardPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(LocalCardWidth.current, LocalCardHeight.current)
            .border(1.dp, Gold.copy(alpha = 0.2f), CARD_SHAPE)
            .clip(CARD_SHAPE)
            .background(Color.White.copy(alpha = 0.03f))
    )
}
