package com.dehong.duelofSuits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.ui.theme.DangerRed
import com.dehong.duelofSuits.ui.theme.TextOnDark

private val BTN_SHAPE = RoundedCornerShape(6.dp)

@Composable
fun GameInfoOverlay(
    state: GameState,
    onPlaySelected: () -> Unit,
    onPass: () -> Unit,
    onTakeCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            state.phase == GamePhase.ATTACK_PHASE && state.isHumanAttacker -> {
                HudButton(
                    label = "ATTACK",
                    enabled = state.selectedCards.isNotEmpty() && !state.animating,
                    background = Color(0xFFB07A00),
                    textColor = Color.White,
                    onClick = onPlaySelected
                )
            }

            state.phase == GamePhase.THROW_IN_PHASE && state.isHumanTurn -> {
                if (state.selectedCards.isNotEmpty()) {
                    HudButton(
                        label = "THROW IN",
                        enabled = !state.animating,
                        background = Color(0xFF1565C0),
                        textColor = Color.White,
                        onClick = onPlaySelected
                    )
                }
                HudOutlineButton(
                    label = "PASS",
                    enabled = !state.animating,
                    borderColor = TextOnDark.copy(alpha = 0.35f),
                    textColor = TextOnDark.copy(alpha = 0.7f),
                    onClick = onPass
                )
            }

            state.phase == GamePhase.DEFENSE_PHASE && state.isHumanDefender -> {
                HudOutlineButton(
                    label = "TAKE",
                    enabled = !state.animating,
                    borderColor = DangerRed.copy(alpha = 0.7f),
                    textColor = DangerRed,
                    onClick = onTakeCards
                )
            }
        }
    }
}

@Composable
private fun HudButton(
    label: String,
    enabled: Boolean,
    background: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(BTN_SHAPE)
            .background(if (enabled) background else background.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun HudOutlineButton(
    label: String,
    enabled: Boolean,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(1.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.3f), BTN_SHAPE)
            .clip(BTN_SHAPE)
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) textColor else textColor.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}
