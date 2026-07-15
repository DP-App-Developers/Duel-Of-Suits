package com.dehong.duelofSuits.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.model.GamePhase
import com.dehong.duelofSuits.model.GameState
import com.dehong.duelofSuits.ui.theme.ActionGreen
import com.dehong.duelofSuits.ui.theme.CounterBackground
import com.dehong.duelofSuits.ui.theme.DangerRed
import com.dehong.duelofSuits.ui.theme.TextOnDark

@Composable
fun GameInfoOverlay(
    state: GameState,
    onPlaySelected: () -> Unit,
    onPass: () -> Unit,
    onConfirmDefense: () -> Unit,
    onTakeCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(CounterBackground, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = state.message,
                color = TextOnDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                state.phase == GamePhase.ATTACK_PHASE && state.isHumanAttacker -> {
                    Button(
                        onClick = onPlaySelected,
                        enabled = state.selectedCards.isNotEmpty() && !state.animating,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen)
                    ) {
                        Text("Attack", fontSize = 12.sp)
                    }
                }

                state.phase == GamePhase.THROW_IN_PHASE && state.isHumanTurn -> {
                    AnimatedVisibility(
                        visible = state.selectedCards.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Button(
                            onClick = onPlaySelected,
                            enabled = !state.animating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
                        ) {
                            Text("Throw In", fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = onPass,
                        enabled = !state.animating
                    ) {
                        Text("Pass", fontSize = 12.sp, color = TextOnDark)
                    }
                }

                state.phase == GamePhase.DEFENSE_PHASE && state.isHumanDefender -> {
                    AnimatedVisibility(
                        visible = state.allSlotsDefended,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Button(
                            onClick = onConfirmDefense,
                            enabled = !state.animating,
                            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen)
                        ) {
                            Text("Confirm ✓", fontSize = 12.sp, color = Color.White)
                        }
                    }
                    OutlinedButton(
                        onClick = onTakeCards,
                        enabled = !state.animating,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Text("Take Cards", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
