package com.huyhoa.coclio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemoteDashboardBlocks(
    blocks: List<RemoteDashboardBlock>,
    onAction: (String) -> Unit
) {
    if (blocks.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        blocks.forEach { block ->
            RemoteUiBlock(block = block, onAction = onAction)
        }
    }
}

@Composable
private fun RemoteUiBlock(
    block: RemoteDashboardBlock,
    onAction: (String) -> Unit
) {
    val spacing = block.spacingDp.dp
    when (block.type) {
        "divider" -> Divider(color = MaterialTheme.colorScheme.outlineVariant)
        "spacer" -> Spacer(Modifier.height(spacing.coerceAtLeast(4.dp)))
        "text" -> {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (block.title.isNotBlank()) {
                    Text(
                        block.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (block.message.isNotBlank()) {
                    Text(
                        block.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
        "button" -> {
            if (block.actionId.isNotBlank() && block.actionLabel.isNotBlank()) {
                Button(
                    onClick = { onAction(block.actionId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(block.actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        "column", "row" -> {
            val content: @Composable () -> Unit = {
                if (block.title.isNotBlank()) {
                    Text(
                        block.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (block.message.isNotBlank()) {
                    Text(
                        block.message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                block.children.forEach { child ->
                    RemoteUiBlock(child, onAction)
                }
            }
            if (block.type == "row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    content()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    content()
                }
            }
        }
        "notice", "card" -> {
            val accent = when (block.severity) {
                "success" -> Color(0xFF42C98A)
                "warning" -> Color(0xFFFFB74D)
                "error" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accent.copy(alpha = 0.07f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    if (block.title.isNotBlank()) {
                        Text(
                            block.title,
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (block.message.isNotBlank()) {
                        Text(
                            block.message,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                    block.children.forEach { child ->
                        RemoteUiBlock(child, onAction)
                    }
                    if (block.actionId.isNotBlank() && block.actionLabel.isNotBlank()) {
                        Button(onClick = { onAction(block.actionId) }) {
                            Text(block.actionLabel, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
