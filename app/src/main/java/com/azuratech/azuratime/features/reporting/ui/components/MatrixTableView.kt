package com.azuratech.azuratime.features.reporting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.features.reporting.ui.matrix.AttendanceMatrixData
import java.time.format.DateTimeFormatter

@Composable
fun MatrixTableView(
    data: AttendanceMatrixData,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        // We use a scrollable row for the table content
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    MatrixHeaderCell(text = "Nama", width = 150.dp)
                    MatrixHeaderCell(text = "Kelas", width = 100.dp)

                    data.dateRange.forEach { date ->
                        val formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM"))
                        MatrixHeaderCell(text = formattedDate, width = 60.dp)
                    }

                    MatrixHeaderCell(text = "H", width = 50.dp)
                    MatrixHeaderCell(text = "S", width = 50.dp)
                    MatrixHeaderCell(text = "I", width = 50.dp)
                    MatrixHeaderCell(text = "A", width = 50.dp)
                }

                // Data Rows
                LazyColumn {
                    items(data.rows, key = { it.studentId }) { row ->
                        Row(
                            modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            MatrixDataCell(text = row.studentName, width = 150.dp, isBold = true)
                            MatrixDataCell(text = row.studentClass, width = 100.dp)

                            row.cells.forEach { cell ->
                                MatrixStatusCell(
                                    text = cell.text,
                                    textColor = cell.textColor,
                                    backgroundColor = cell.backgroundColor,
                                    width = 60.dp,
                                )
                            }

                            MatrixDataCell(text = row.summaryH, width = 50.dp)
                            MatrixDataCell(text = row.summaryT, width = 50.dp)
                            MatrixDataCell(text = row.summaryS, width = 50.dp)
                            MatrixDataCell(text = row.summaryI, width = 50.dp)
                            MatrixDataCell(text = row.summaryA, width = 50.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixHeaderCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MatrixDataCell(text: String, width: androidx.compose.ui.unit.Dp, isBold: Boolean = false) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MatrixStatusCell(text: String, textColor: Color, backgroundColor: Color, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(backgroundColor)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
