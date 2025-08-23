import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myaiapp.R
import com.example.myaiapp.chat.presentation.ui_model.item.VerifyItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerifyItemDelegate(
    item: VerifyItem,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp
) {
    val statusColor = if (item.ok) Color(0xFF16A34A) else Color(0xFFDC2626) // зелёный/красный
    val faint = Color(0xFF9CA3AF) // серый для подписей
    val chipErrBg = Color(0xFFFFE4E6)
    val chipErrFg = Color(0xFFB91C1C)
    val chipWarnBg = Color(0xFFFEF3C7)
    val chipWarnFg = Color(0xFF92400E)

    Column(
        modifier = modifier
            .padding(dimensionResource(id = R.dimen.indent_10dp))
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(Color(0xFFF7F7F7))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor)
            )
            BasicText(
                text = if (item.ok) "Статус: PASSED" else "Статус: FAILED",
                style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.weight(1f))
            TinyLabel(text = "mode=${item.mode}", color = faint)
        }

        // Score
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val pct = (item.score.coerceIn(0.0, 1.0) * 100).toInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText("Оценка качества: $pct%")
                Spacer(Modifier.width(8.dp))
                TinyLabel(text = if (item.ok) "базовые проверки пройдены" else "есть проблемы", color = faint)
            }
            ProgressBarSimple(
                progress = item.score.coerceIn(0.0, 1.0).toFloat(),
                height = 8.dp,
                track = Color(0xFFE5E7EB),
                bar = statusColor
            )
        }

        // Missing required
        SectionTitle("Обязательные поля (A–E)")
        if (item.missingRequired.isNullOrEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TickDot(Color(0xFF16A34A))
                BasicText("Все обязательные поля на месте", style = androidx.compose.ui.text.TextStyle(color = faint))
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.missingRequired.forEach { tag ->
                    Chip(text = tag, bg = chipErrBg, fg = chipErrFg)
                }
            }
        }

        // Missing optional
        SectionTitle("Дополнительные пункты (из 12)")
        if (item.missingOptional.isNullOrEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TickDot(Color(0xFF16A34A))
                BasicText("Замечаний нет", style = androidx.compose.ui.text.TextStyle(color = faint))
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.missingOptional.forEach { tag ->
                    Chip(text = tag, bg = chipWarnBg, fg = chipWarnFg)
                }
            }
        }

        // Notes
        SectionTitle("Заметки ревьюера")
        BulletNotesSimple(item.notes)
    }
}

@Composable
private fun TinyLabel(text: String, color: Color) {
    BasicText(
        text = text,
        style = androidx.compose.ui.text.TextStyle(
            color = color,
            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text = text,
        style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun Chip(text: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
    ) {
        BasicText(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = androidx.compose.ui.text.TextStyle(color = fg),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProgressBarSimple(
    progress: Float,
    height: Dp,
    track: Color,
    bar: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(height / 2))
                .background(bar)
        )
    }
}

@Composable
private fun TickDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}

@Composable
private fun BulletNotesSimple(text: String) {
    val items = text
        .split('\n', ';')
        .map { it.trim().removePrefix("-").removePrefix("•").trim() }
        .filter { it.isNotEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (items.isEmpty()) {
            BasicText("Нет замечаний")
        } else {
            items.forEach { line ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TickDot(Color(0xFF9CA3AF))
                    BasicText(line)
                }
            }
        }
    }
}
