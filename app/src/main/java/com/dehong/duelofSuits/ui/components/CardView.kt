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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.R
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
            .background(Brush.linearGradient(listOf(CardBackBlue, CardBackBlueLight)))
            .drawWithCache {
                val gold = Gold
                val lineColor = gold.copy(alpha = 0.14f)
                val spacing = 7.dp.toPx()

                val lineStarts = mutableListOf<Offset>()
                val lineEnds = mutableListOf<Offset>()
                var x = -size.height
                while (x < size.width + size.height) {
                    lineStarts += Offset(x, 0f);              lineEnds += Offset(x + size.height, size.height)
                    x += spacing
                }
                x = -size.height
                while (x < size.width + size.height) {
                    lineStarts += Offset(x + size.height, 0f); lineEnds += Offset(x, size.height)
                    x += spacing
                }
                val lineCount = lineStarts.size

                val outerInset   = 3.5f.dp.toPx()
                val outerTopLeft = Offset(outerInset, outerInset)
                val outerSize    = Size(size.width - outerInset * 2, size.height - outerInset * 2)
                val outerColor   = gold.copy(alpha = 0.55f)
                val outerCorner  = CornerRadius(5.dp.toPx())
                val outerStroke  = Stroke(width = 1.5f.dp.toPx())

                val innerInset   = 6.5f.dp.toPx()
                val innerTopLeft = Offset(innerInset, innerInset)
                val innerSize    = Size(size.width - innerInset * 2, size.height - innerInset * 2)
                val innerColor   = gold.copy(alpha = 0.35f)
                val innerCorner  = CornerRadius(3.dp.toPx())
                val innerStroke  = Stroke(width = 0.8f.dp.toPx())

                val cx = size.width / 2f; val cy = size.height / 2f; val r = 7.dp.toPx()
                val diamond = Path().apply {
                    moveTo(cx, cy - r); lineTo(cx + r, cy)
                    lineTo(cx, cy + r); lineTo(cx - r, cy)
                    close()
                }
                val diamondColor  = gold.copy(alpha = 0.30f)
                val diamondStroke = Stroke(width = 0.9f.dp.toPx())

                onDrawWithContent {
                    drawContent()
                    repeat(lineCount) { i -> drawLine(lineColor, lineStarts[i], lineEnds[i], 0.8f) }
                    drawRoundRect(outerColor, outerTopLeft, outerSize, outerCorner, style = outerStroke)
                    drawRoundRect(innerColor, innerTopLeft, innerSize, innerCorner, style = innerStroke)
                    drawPath(diamond, diamondColor, style = diamondStroke)
                }
            }
    )
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
            .drawWithCache {
                val inset       = 2.5f.dp.toPx()
                val borderColor = Color.Black.copy(alpha = 0.07f)
                val topLeft     = Offset(inset, inset)
                val rectSize    = Size(size.width - inset * 2, size.height - inset * 2)
                val corner      = CornerRadius(5.dp.toPx())
                val stroke      = Stroke(width = 0.8f.dp.toPx())
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(borderColor, topLeft, rectSize, corner, style = stroke)
                }
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
            .drawWithCache {
                val inset       = 2.5f.dp.toPx()
                val borderColor = Color.Black.copy(alpha = 0.07f)
                val topLeft     = Offset(inset, inset)
                val rectSize    = Size(size.width - inset * 2, size.height - inset * 2)
                val corner      = CornerRadius(5.dp.toPx())
                val stroke      = Stroke(width = 0.8f.dp.toPx())
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(borderColor, topLeft, rectSize, corner, style = stroke)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.card_joker_emoji), fontSize = 22.sp)
            Text(
                text = stringResource(R.string.card_joker),
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
