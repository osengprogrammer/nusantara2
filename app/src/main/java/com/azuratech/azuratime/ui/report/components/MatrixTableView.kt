package com.azuratech.azuratime.ui.report.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.report.MatrixCellModel
import com.azuratech.azuratime.ui.report.MatrixRowModel
import java.time.LocalDate

private val NameColumnWidth = 150.dp
private val DateCellWidth = 75.dp
private val SummaryCellWidth = 60.dp
private val TotalHoursCellWidth = 85.dp
private val SalaryCellWidth = 110.dp

@Composable
fun MatrixTableView(
    rows: List<MatrixRowModel>,
    dateRange: List<LocalDate>,
    onCellClick: (String, String, LocalDate) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // --- HEADER ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            TableHeaderCell(text = "NAMA SISWA", width = NameColumnWidth)
            LazyRow {
                items(dateRange) { date ->
                    TableHeaderCell(text = "${date.dayOfMonth}/${date.monthValue}", width = DateCellWidth)
                }
                item { TableHeaderCell(text = "∑ JAM", width = TotalHoursCellWidth) }
                item { TableHeaderCell(text = "∑ H", width = SummaryCellWidth) }
                item { TableHeaderCell(text = "∑ S", width = SummaryCellWidth) }
                item { TableHeaderCell(text = "∑ I", width = SummaryCellWidth) }
                item { TableHeaderCell(text = "∑ A", width = SummaryCellWidth) }
                item { TableHeaderCell(text = "GAJI", width = SalaryCellWidth) }
            }
        }

        // --- DATA ROWS ---
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(rows, key = { it.studentId }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sticky Student Name Cell
                    TableContentCell(
                        text = row.studentName,
                        width = NameColumnWidth,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Bold,
                        textColor = MaterialTheme.colorScheme.primary
                    )

                    // Scrollable Data Cells
                    LazyRow {
                        itemsIndexed(row.cells) { index, cell ->
                            val date = dateRange[index]
                            TableContentCell(
                                text = cell.text,
                                width = DateCellWidth,
                                modifier = Modifier.clickable { onCellClick(row.studentId, row.studentName, date) },
                                textColor = cell.textColor,
                                backgroundColor = cell.bgColor,
                                fontWeight = if (cell.isBold) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        // Summary Cells
                        item { TableContentCell(text = row.totalHours, width = TotalHoursCellWidth, fontWeight = FontWeight.Bold) }
                        item { TableContentCell(text = row.summaryH, width = SummaryCellWidth, fontWeight = FontWeight.Bold) }
                        item { TableContentCell(text = row.summaryS, width = SummaryCellWidth) }
                        item { TableContentCell(text = row.summaryI, width = SummaryCellWidth) }
                        item { TableContentCell(text = row.summaryA, width = SummaryCellWidth, textColor = if (row.summaryA != "0") MaterialTheme.colorScheme.error else Color.Unspecified) }
                        item { TableContentCell(text = row.estimatedSalary, width = SalaryCellWidth, fontWeight = FontWeight.ExtraBold, textColor = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: Dp) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.width(width),
        border = BorderStroke(0.1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun TableContentCell(
    text: String,
    width: Dp,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    fontWeight: FontWeight = FontWeight.Normal,
    textColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.Transparent
) {
    Surface(
        color = backgroundColor,
        modifier = modifier.width(width).fillMaxHeight(),
        border = BorderStroke(0.1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = fontWeight,
                textAlign = textAlign,
                color = textColor
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun MatrixTableViewPreview() {
    val sampleDateRange = List(10) { LocalDate.now().minusDays((9 - it).toLong()) }
    val sampleRows = listOf(
        MatrixRowModel(
            studentId = "1",
            studentName = "Ahmad Rizki",
            studentClass = "Kelas 10A",
            cells = sampleDateRange.mapIndexed { index, _ ->
                when (index % 5) {
                    0 -> MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), isBold = true)
                    1 -> MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), isBold = true)
                    2 -> MatrixCellModel("S", Color(0xFFF9A825), Color(0xFFFFF9C4), isBold = false)
                    3 -> MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), isBold = true)
                    else -> MatrixCellModel("A", Color(0xFFC62828), Color(0xFFFFEBEE), isBold = false)
                }
            },
            totalHours = "12j 30m",
            summaryH = "7",
            summaryS = "1",
            summaryI = "0",
            summaryA = "2",
            estimatedSalary = "Rp 0"
        ),
        MatrixRowModel(
            studentId = "2",
            studentName = "Siti Nurhaliza",
            studentClass = "Kelas 10B",
            cells = sampleDateRange.mapIndexed { index, _ ->
                when (index % 4) {
                    0 -> MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), isBold = true)
                    1 -> MatrixCellModel("I", Color(0xFF1565C0), Color(0xFFE3F2FD), isBold = false)
                    2 -> MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), isBold = true)
                    else -> MatrixCellModel("H", Color(0xFF2E7D32), Color(0xFFE8F5E9), isBold = true)
                }
            },
            totalHours = "15j 00m",
            summaryH = "8",
            summaryS = "0",
            summaryI = "1",
            summaryA = "1",
            estimatedSalary = "Rp 0"
        )
    )

    MaterialTheme {
        MatrixTableView(
            rows = sampleRows,
            dateRange = sampleDateRange,
            onCellClick = { _, _, _ -> }
        )
    }
}
