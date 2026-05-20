package com.azuratech.azuratime.features.reporting.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.azuratech.azuratime.features.reporting.ui.matrix.AttendanceMatrixData
import java.time.format.DateTimeFormatter

@Composable
fun MatrixTableView(
    data: AttendanceMatrixData,
    modifier: Modifier = Modifier,
    onCellClick: ((studentId: String, studentName: String, date: java.time.LocalDate) -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    // Fixed widths for columns
    val nameColumnWidth = 150.dp
    val classColumnWidth = 100.dp
    val statusColumnWidth = 60.dp
    val summaryColumnWidth = 50.dp

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Scrollable Content
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                Column {
                    // Header Row
                    Row(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        // Placeholder for fixed columns in scrollable row to maintain alignment
                        Spacer(modifier = Modifier.width(nameColumnWidth + classColumnWidth))

                        data.dateRange.forEach { date ->
                            val formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM"))
                            MatrixHeaderCell(text = formattedDate, width = statusColumnWidth)
                        }

                        MatrixHeaderCell(text = "H", width = summaryColumnWidth)
                        MatrixHeaderCell(text = "T", width = summaryColumnWidth)
                        MatrixHeaderCell(text = "S", width = summaryColumnWidth)
                        MatrixHeaderCell(text = "I", width = summaryColumnWidth)
                        MatrixHeaderCell(text = "A", width = summaryColumnWidth)
                    }

                    // Data Rows
                    LazyColumn(modifier = Modifier.fillMaxHeight()) {
                        items(data.rows, key = { it.studentId }) { row ->
                            Row(
                                modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                // Placeholder for fixed columns
                                Spacer(modifier = Modifier.width(nameColumnWidth + classColumnWidth))

                                data.dateRange.forEachIndexed { index, date ->
                                    val cell = row.cells.getOrNull(index)
                                    MatrixStatusCell(
                                        text = cell?.text ?: "-",
                                        textColor = cell?.textColor ?: Color.Gray,
                                        backgroundColor = cell?.backgroundColor ?: Color.Transparent,
                                        width = statusColumnWidth,
                                        onClick = { onCellClick?.invoke(row.studentId, row.studentName, date) },
                                    )
                                }

                                MatrixDataCell(text = row.summaryH, width = summaryColumnWidth)
                                MatrixDataCell(text = row.summaryT, width = summaryColumnWidth)
                                MatrixDataCell(text = row.summaryS, width = summaryColumnWidth)
                                MatrixDataCell(text = row.summaryI, width = summaryColumnWidth)
                                MatrixDataCell(text = row.summaryA, width = summaryColumnWidth)
                            }
                        }
                    }
                }
            }

            // Fixed Columns (Sticky Name and Class)
            Column(
                modifier = Modifier
                    .width(nameColumnWidth + classColumnWidth)
                    .zIndex(1f), // Ensure it stays on top
            ) {
                // Fixed Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row {
                        MatrixHeaderCell(text = "Nama", width = nameColumnWidth)
                        MatrixHeaderCell(text = "Kelas", width = classColumnWidth)
                    }
                }

                // Fixed Data Rows
                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    items(data.rows, key = { it.studentId }) { row ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row {
                                MatrixDataCell(text = row.studentName, width = nameColumnWidth, isBold = true)
                                MatrixDataCell(text = row.studentClass, width = classColumnWidth)
                            }
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
            .height(40.dp) // Fixed height for alignment
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
            .height(40.dp) // Fixed height for alignment
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
private fun MatrixStatusCell(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    width: androidx.compose.ui.unit.Dp,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(40.dp) // Fixed height for alignment
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(backgroundColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
