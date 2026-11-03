package io.github.giova.metroceara.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.giova.metroceara.data.Direction
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import io.github.giova.metroceara.R
import io.github.giova.metroceara.data.DirectionVia

/**
 * Um sentido da linha: "sentido" pequeno em cima e o nome da ponta embaixo,
 * para não parecer o nome de uma estação de destino. Assim o nome não se
 * parte com a letra grande.
 */
@Composable
fun DirectionLabel(
    headsign: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            text = stringResource(R.string.direction_label),
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.75f),
        )
        Text(text = headsign, style = style)
    }
}

/** "Passa por **A, B, C, D**… até X · N estações", com os nomes do caminho em negrito. */
@Composable
fun directionViaText(via: DirectionVia): AnnotatedString {
    // Só falta a última: "Próxima estação: X, a última".
    if (via.shown.isEmpty()) {
        return buildAnnotatedString {
            append(stringResource(R.string.direction_via_next))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(via.end) }
            append(stringResource(R.string.direction_via_last))
        }
    }
    val count = stringResource(R.string.direction_via_count, via.count)
    val and = stringResource(R.string.direction_via_and)
    val names = if (via.more || via.shown.size == 1) via.shown.joinToString(", ")
    else via.shown.dropLast(1).joinToString(", ") + and + via.shown.last()
    return buildAnnotatedString {
        append(stringResource(R.string.direction_via_prefix))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(names) }
        if (via.more) append("…")
        append(stringResource(R.string.direction_via_until, via.end))
        append(" · ")
        append(count)
    }
}

/**
 * Um metrô pequeno com a seta para o lado em que ele vai: o sentido 0 da linha
 * vai para a esquerda e o outro para a direita, igual em todas as estações.
 */
@Composable
fun DirectionTrain(
    toLeft: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    width: Dp = 36.dp,
) {
    Canvas(modifier.size(width = width, height = width * 0.55f)) {
        // Desenhado indo para a direita; para a esquerda, espelhado.
        withTransform({ if (toLeft) scale(-1f, 1f, pivot = center) }) {
            val u = size.width / 40f
            val stroke = 2.4f * u
            // O metrô e, na frente dele, a seta.
            drawRoundRect(color, Offset(1 * u, 3 * u), Size(22 * u, 13 * u), CornerRadius(4 * u))
            listOf(4f, 11f, 18f).forEach { x ->
                drawRoundRect(Color.White, Offset(x * u, 6 * u), Size(4 * u, 4 * u), CornerRadius(1 * u))
            }
            drawCircle(color, 1.9f * u, Offset(6 * u, 18.6f * u))
            drawCircle(color, 1.9f * u, Offset(18 * u, 18.6f * u))
            drawLine(color, Offset(27 * u, 9.5f * u), Offset(38 * u, 9.5f * u), stroke, StrokeCap.Round)
            val arrow = Path().apply {
                moveTo(34 * u, 5.5f * u); lineTo(38 * u, 9.5f * u); lineTo(34 * u, 13.5f * u)
            }
            drawPath(arrow, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * As abas dos sentidos como um trilho: a estação é a bolinha no meio, e cada
 * aba tem um metrô indo para a sua ponta. Num terminal, só uma aba, e a
 * bolinha fica na ponta oposta. Os sentidos vêm ordenados pelo número.
 */
@Composable
fun DirectionTrack(
    directions: List<Direction>,
    selected: Int,
    onSelect: (Int) -> Unit,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val indicator = MaterialTheme.colorScheme.primary
    val divider = MaterialTheme.colorScheme.outlineVariant
    val single = directions.singleOrNull()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .selectableGroup()
            // O trilho por trás das abas; a bolinha (a estação) por cima de tudo.
            .drawWithContent {
                val y = size.height / 2
                drawLine(lineColor.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
                drawLine(divider, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                drawContent()
                val dot = 7.dp.toPx()
                val x = when {
                    single == null -> size.width / 2
                    single.id == 0 -> size.width - 16.dp.toPx()
                    else -> 16.dp.toPx()
                }
                drawCircle(surface, dot, Offset(x, y))
                drawCircle(lineColor, dot, Offset(x, y), style = Stroke(3.dp.toPx()))
            },
    ) {
        directions.forEachIndexed { index, direction ->
            val isSelected = index == selected
            val toLeft = direction.id == 0
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                    .drawBehind {
                        if (isSelected) {
                            val h = 3.dp.toPx()
                            drawRect(indicator, Offset(0f, size.height - h), Size(size.width, h))
                        }
                    }
                    // Folga do lado da bolinha, para o nome não passar por cima dela.
                    .padding(
                        start = if (toLeft || single != null) 6.dp else 18.dp,
                        end = if (!toLeft || single != null) 6.dp else 18.dp,
                        top = 12.dp,
                        bottom = 12.dp,
                    ),
                horizontalArrangement = if (toLeft) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val train = @Composable {
                    DirectionTrain(
                        toLeft = toLeft,
                        color = if (isSelected) lineColor else lineColor.copy(alpha = 0.5f),
                        modifier = Modifier.background(surface).padding(horizontal = 2.dp),
                    )
                }
                if (toLeft) train()
                DirectionLabel(
                    headsign = direction.headsign,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .background(surface)
                        .padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                        textAlign = if (toLeft) TextAlign.Start else TextAlign.End,
                    ),
                    horizontalAlignment = if (toLeft) Alignment.Start else Alignment.End,
                )
                if (!toLeft) train()
            }
        }
    }
}

/**
 * Para o cartão do início e as integrações: uma seta curva ("vira para lá") e
 * o metrô de frente. O sentido 0 vira para a esquerda, como no trilho das abas.
 */
@Composable
fun DirectionTurn(
    toLeft: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    width: Dp = 40.dp,
) {
    Canvas(modifier.size(width = width, height = width * 0.5f)) {
        // Desenhado virando para a esquerda; para a direita, espelhado.
        withTransform({ if (!toLeft) scale(-1f, 1f, pivot = center) }) {
            val u = size.width / 44f
            val curve = Path().apply {
                moveTo(19 * u, 20 * u); lineTo(19 * u, 12 * u)
                quadraticTo(19 * u, 7 * u, 14 * u, 7 * u); lineTo(6 * u, 7 * u)
            }
            drawPath(curve, color, style = Stroke(3.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val head = Path().apply {
                moveTo(0.5f * u, 7 * u); lineTo(8 * u, 1 * u); lineTo(8 * u, 13 * u); close()
            }
            drawPath(head, color)
            drawRoundRect(color, Offset(25 * u, 1.5f * u), Size(15 * u, 15.5f * u), CornerRadius(4 * u))
            drawRoundRect(Color.White, Offset(27.5f * u, 4 * u), Size(10 * u, 5 * u), CornerRadius(1.5f * u))
            drawCircle(Color.White, 1.4f * u, Offset(28.8f * u, 13 * u))
            drawCircle(Color.White, 1.4f * u, Offset(36.2f * u, 13 * u))
            drawLine(color, Offset(27.5f * u, 17 * u), Offset(25.5f * u, 21 * u), 2 * u, StrokeCap.Round)
            drawLine(color, Offset(37.5f * u, 17 * u), Offset(39.5f * u, 21 * u), 2 * u, StrokeCap.Round)
        }
    }
}
