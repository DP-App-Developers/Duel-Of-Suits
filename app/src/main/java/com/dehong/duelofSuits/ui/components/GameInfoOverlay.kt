package com.dehong.duelofSuits.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.ui.theme.DangerRed
import com.dehong.duelofSuits.ui.theme.TextOnDark

private val BTN_SHAPE = RoundedCornerShape(14.dp)
private val BTN_MIN_WIDTH = 130.dp

@Composable
fun GameInfoOverlay(
    state: GameState,
    onPlaySelected: () -> Unit,
    onPass: () -> Unit,
    onTakeCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        when {
            state.phase == GamePhase.ATTACK_PHASE && state.isHumanAttacker -> {
                ActionButton(
                    label = "ATTACK",
                    icon = "▲",
                    enabled = state.selectedCards.isNotEmpty() && !state.animating,
                    topColor = Color(0xFFFFB300),
                    bottomColor = Color(0xFFE65100),
                    glowColor = Color(0xFFFF8F00),
                    onClick = onPlaySelected
                )
            }

            state.phase == GamePhase.THROW_IN_PHASE && state.isHumanTurn -> {
                if (state.selectedCards.isNotEmpty()) {
                    ActionButton(
                        label = "THROW IN",
                        icon = "+",
                        enabled = !state.animating,
                        topColor = Color(0xFF42A5F5),
                        bottomColor = Color(0xFF1565C0),
                        glowColor = Color(0xFF1976D2),
                        onClick = onPlaySelected
                    )
                }
                GhostButton(
                    label = "PASS",
                    enabled = !state.animating,
                    onClick = onPass
                )
            }

            state.phase == GamePhase.DEFENSE_PHASE && state.isHumanDefender -> {
                ActionButton(
                    label = "TAKE CARDS",
                    icon = "✕",
                    enabled = !state.animating,
                    topColor = Color(0xFFEF5350),
                    bottomColor = Color(0xFF880E4F),
                    glowColor = DangerRed,
                    onClick = onTakeCards
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: String,
    enabled: Boolean,
    topColor: Color,
    bottomColor: Color,
    glowColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pulse = rememberInfiniteTransition(label = "btnPulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.50f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.94f
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = tween(100),
        label = "scale"
    )

    val alpha = if (enabled) 1f else 0.38f
    val gradient = Brush.verticalGradient(
        colors = listOf(
            topColor.copy(alpha = alpha),
            bottomColor.copy(alpha = alpha)
        )
    )
    val effectiveGlow = if (enabled) glowColor.copy(alpha = glowAlpha) else Color.Transparent

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = if (enabled) 18.dp else 2.dp,
                shape = BTN_SHAPE,
                ambientColor = effectiveGlow,
                spotColor = effectiveGlow
            )
            .widthIn(min = BTN_MIN_WIDTH)
            .clip(BTN_SHAPE)
            .background(gradient)
            .drawWithContent {
                drawContent()
                // Top bevel — bright inner highlight gives a pressed-metal feel
                drawLine(
                    color = Color.White.copy(alpha = if (enabled) 0.40f else 0.10f),
                    start = Offset(14.dp.toPx(), 1.5.dp.toPx()),
                    end = Offset(size.width - 14.dp.toPx(), 1.5.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
                // Bottom shadow line for depth
                drawLine(
                    color = Color.Black.copy(alpha = 0.35f),
                    start = Offset(14.dp.toPx(), size.height - 1.5.dp.toPx()),
                    end = Offset(size.width - 14.dp.toPx(), size.height - 1.5.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                color = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.8.sp
            )
        }
    }
}

@Composable
private fun GhostButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "ghostScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .widthIn(min = BTN_MIN_WIDTH)
            .border(
                width = 1.dp,
                color = TextOnDark.copy(alpha = if (enabled) 0.28f else 0.10f),
                shape = BTN_SHAPE
            )
            .clip(BTN_SHAPE)
            .background(Color.Black.copy(alpha = 0.22f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextOnDark.copy(alpha = if (enabled) 0.58f else 0.22f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.2.sp
        )
    }
}
