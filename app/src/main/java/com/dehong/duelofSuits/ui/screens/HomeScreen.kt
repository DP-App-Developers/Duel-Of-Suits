package com.dehong.duelofSuits.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.R
import com.dehong.duelofSuits.model.Difficulty
import com.dehong.duelofSuits.ui.theme.Gold
import com.dehong.duelofSuits.ui.theme.TableGreen
import com.dehong.duelofSuits.ui.theme.TableGreenLight
import com.dehong.duelofSuits.ui.theme.TextOnDark

private val difficultyLabels = mapOf(
    Difficulty.NORMAL to R.string.home_difficulty_normal,
    Difficulty.HARD to R.string.home_difficulty_hard,
)

@Composable
fun HomeScreen(onStartGame: (Int, Difficulty) -> Unit) {
    var difficulty by remember { mutableStateOf(Difficulty.NORMAL) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(TableGreenLight, TableGreen)))
            .drawBehind {
                val lineColor = Color.Black.copy(alpha = 0.05f)
                val spacing = 5.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(lineColor, Offset(x, 0f), Offset(x + size.height, size.height), 0.7f)
                    x += spacing
                }
                x = 0f
                while (x < size.width + size.height) {
                    drawLine(lineColor, Offset(x, 0f), Offset(x - size.height, size.height), 0.7f)
                    x += spacing
                }
            },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth > maxHeight) {
                LandscapeLayout(
                    difficulty = difficulty,
                    onDifficultyChange = { difficulty = it },
                    onStartGame = { onStartGame(it, difficulty) }
                )
            } else {
                PortraitLayout(
                    difficulty = difficulty,
                    onDifficultyChange = { difficulty = it },
                    onStartGame = { onStartGame(it, difficulty) }
                )
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    difficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
    onStartGame: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TitleBlock()
        Spacer(modifier = Modifier.height(32.dp))
        DifficultySection(difficulty, onDifficultyChange)
        Spacer(modifier = Modifier.height(24.dp))
        PlayerCountSection(onStartGame)
    }
}

@Composable
private fun LandscapeLayout(
    difficulty: Difficulty,
    onDifficultyChange: (Difficulty) -> Unit,
    onStartGame: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TitleBlock()
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(0.65f)
                .background(Gold.copy(alpha = 0.25f))
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            DifficultySection(difficulty, onDifficultyChange)
            PlayerCountSection(onStartGame)
        }
    }
}

@Composable
private fun TitleBlock() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.home_title),
            color = Gold,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.width(60.dp).height(1.dp).background(Gold.copy(alpha = 0.45f)))
            Text(stringResource(R.string.home_ornament_diamond), color = Gold.copy(alpha = 0.7f), fontSize = 13.sp)
            Box(Modifier.width(60.dp).height(1.dp).background(Gold.copy(alpha = 0.45f)))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.home_subtitle),
            color = TextOnDark.copy(alpha = 0.45f),
            fontSize = 10.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun DifficultySection(difficulty: Difficulty, onDifficultyChange: (Difficulty) -> Unit) {
    val segmentedColors = SegmentedButtonDefaults.colors(
        activeContainerColor = Color(0xFF1A5235),
        activeContentColor = Gold,
        activeBorderColor = Gold.copy(alpha = 0.85f),
        inactiveContainerColor = Color(0xFF061510),
        inactiveContentColor = Gold.copy(alpha = 0.4f),
        inactiveBorderColor = Gold.copy(alpha = 0.3f),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.home_choose_difficulty),
            color = TextOnDark.copy(alpha = 0.65f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Difficulty.entries.forEachIndexed { index, diff ->
                SegmentedButton(
                    selected = difficulty == diff,
                    onClick = { onDifficultyChange(diff) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = Difficulty.entries.size),
                    colors = segmentedColors,
                    label = {
                        Text(
                            text = stringResource(difficultyLabels.getValue(diff)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PlayerCountSection(onStartGame: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.home_choose_players),
            color = TextOnDark.copy(alpha = 0.65f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PlayerCountButton(count = 2, modifier = Modifier.weight(1f), onStartGame = onStartGame)
            PlayerCountButton(count = 3, modifier = Modifier.weight(1f), onStartGame = onStartGame)
            PlayerCountButton(count = 4, modifier = Modifier.weight(1f), onStartGame = onStartGame)
        }
    }
}

@Composable
private fun PlayerCountButton(count: Int, modifier: Modifier = Modifier, onStartGame: (Int) -> Unit) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1A5235), Color(0xFF0A2418))),
                RoundedCornerShape(12.dp)
            )
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                val r = 12.dp.toPx()
                drawRoundRect(
                    color = Gold.copy(alpha = 0.7f),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(stroke)
                )
            }
            .clickable { onStartGame(count) }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "$count",
                color = Gold,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(32.dp).height(1.dp).background(Gold.copy(alpha = 0.35f)))
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (count == 1) stringResource(R.string.home_player_singular) else stringResource(R.string.home_player_plural),
                color = TextOnDark.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}
