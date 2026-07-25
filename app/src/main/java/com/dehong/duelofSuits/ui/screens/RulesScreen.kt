package com.dehong.duelofSuits.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dehong.duelofSuits.R
import com.dehong.duelofSuits.ui.theme.Gold
import com.dehong.duelofSuits.ui.theme.GoldLight
import com.dehong.duelofSuits.ui.theme.TableGreen
import com.dehong.duelofSuits.ui.theme.TableGreenLight
import com.dehong.duelofSuits.ui.theme.TextOnDark

@Composable
fun RulesScreen(onBack: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val rulesText = when {
        locale.language == "zh" && locale.country in setOf("TW", "HK") -> RulesContent.traditionalChinese
        locale.language == "zh" -> RulesContent.simplifiedChinese
        else -> RulesContent.english
    }

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
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            LeftPanel(
                onBack = onBack,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(200.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .width(1.dp)
                    .align(Alignment.CenterVertically)
                    .background(Gold.copy(alpha = 0.2f))
            )

            RulesContentPanel(
                rulesText = rulesText,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun LeftPanel(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.rules_title),
            color = Gold,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.width(30.dp).height(1.dp).background(Gold.copy(alpha = 0.4f)))
            Text("♦", color = Gold.copy(alpha = 0.6f), fontSize = 11.sp)
            Box(Modifier.width(30.dp).height(1.dp).background(Gold.copy(alpha = 0.4f)))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.rules_subtitle),
            color = TextOnDark.copy(alpha = 0.4f),
            fontSize = 9.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.weight(1f))

        BackButton(onClick = onBack)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1A5235), Color(0xFF0A2418))),
                RoundedCornerShape(10.dp)
            )
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                val r = 10.dp.toPx()
                drawRoundRect(
                    color = Gold.copy(alpha = 0.55f),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(stroke)
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.rules_back),
            color = Gold.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun RulesContentPanel(rulesText: String, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val lines = remember(rulesText) { parseRules(rulesText) }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 28.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(lines) { line ->
            RuleLineItem(line)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ── Parsing ──────────────────────────────────────────────────────────────────

private sealed class RuleLine {
    data class H1(val text: String) : RuleLine()
    data class H2(val text: String) : RuleLine()
    data class H3(val text: String) : RuleLine()
    data class Body(val text: String) : RuleLine()
    data class Bullet(val text: String) : RuleLine()
    data class Numbered(val num: Int, val text: String) : RuleLine()
    object Divider : RuleLine()
    object Gap : RuleLine()
}

private fun parseRules(text: String): List<RuleLine> {
    val result = mutableListOf<RuleLine>()
    var numberedCounter = 0
    for (raw in text.lines()) {
        val line = raw.trim()
        when {
            line == "---"                       -> { result += RuleLine.Divider; numberedCounter = 0 }
            line.startsWith("# ")              -> { result += RuleLine.H1(line.removePrefix("# ")); numberedCounter = 0 }
            line.startsWith("## ")             -> { result += RuleLine.H2(line.removePrefix("## ")); numberedCounter = 0 }
            line.startsWith("### ")            -> { result += RuleLine.H3(line.removePrefix("### ")); numberedCounter = 0 }
            line.startsWith("- ")              -> result += RuleLine.Bullet(line.removePrefix("- "))
            line.matches(Regex("\\d+\\. .*")) -> {
                numberedCounter++
                result += RuleLine.Numbered(numberedCounter, line.substringAfter(". "))
            }
            line.isEmpty()                     -> { if (result.lastOrNull() !is RuleLine.Gap) result += RuleLine.Gap }
            else                               -> { result += RuleLine.Body(line); numberedCounter = 0 }
        }
    }
    return result
}

// ── Rendering ─────────────────────────────────────────────────────────────────

@Composable
private fun RuleLineItem(line: RuleLine) {
    when (line) {
        is RuleLine.H1 -> {
            Spacer(Modifier.height(16.dp))
            Text(
                text = line.text,
                color = Gold,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Box(Modifier.width(48.dp).height(1.5.dp).background(Gold.copy(alpha = 0.5f)))
            Spacer(Modifier.height(8.dp))
        }
        is RuleLine.H2 -> {
            Spacer(Modifier.height(10.dp))
            Text(
                text = line.text,
                color = GoldLight.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
        }
        is RuleLine.H3 -> {
            Spacer(Modifier.height(8.dp))
            Text(
                text = line.text,
                color = TextOnDark.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(3.dp))
        }
        is RuleLine.Body -> {
            Text(
                text = styledText(line.text),
                color = TextOnDark.copy(alpha = 0.82f),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(3.dp))
        }
        is RuleLine.Bullet -> {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "•",
                    color = Gold.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 1.dp, end = 6.dp)
                )
                Text(
                    text = styledText(line.text),
                    color = TextOnDark.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            Spacer(Modifier.height(2.dp))
        }
        is RuleLine.Numbered -> {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "${line.num}.",
                    color = Gold.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(22.dp).padding(top = 1.dp)
                )
                Text(
                    text = styledText(line.text),
                    color = TextOnDark.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
            Spacer(Modifier.height(2.dp))
        }
        RuleLine.Divider -> {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Gold.copy(alpha = 0.15f), thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
        }
        RuleLine.Gap -> Spacer(Modifier.height(6.dp))
    }
}

private fun styledText(raw: String) = buildAnnotatedString {
    val boldStyle = SpanStyle(fontWeight = FontWeight.Bold, color = Gold.copy(alpha = 0.95f))
    var i = 0
    while (i < raw.length) {
        val start = raw.indexOf("**", i)
        if (start == -1) { append(raw.substring(i)); break }
        append(raw.substring(i, start))
        val end = raw.indexOf("**", start + 2)
        if (end == -1) { append(raw.substring(start)); break }
        withStyle(boldStyle) { append(raw.substring(start + 2, end)) }
        i = end + 2
    }
}
