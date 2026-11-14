package com.huyhoa.coclio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import coil.compose.AsyncImage
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
        "column" -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
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
        }
        "row" -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    block.children.forEach { child ->
                        Box(Modifier.weight(child.weight)) {
                            RemoteUiBlock(child, onAction)
                        }
                    }
                }
            }
        }
        "badge" -> {
            val accent = when (block.severity) {
                "success" -> Color(0xFF42C98A)
                "warning" -> Color(0xFFFFB74D)
                "error" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
            Text(
                block.title.ifBlank { block.message },
                modifier = Modifier
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(30.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        "metric" -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (block.title.isNotBlank()) {
                        Text(block.title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    if (block.message.isNotBlank()) {
                        Text(block.message, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        "list" -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (block.title.isNotBlank()) {
                    Text(block.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (block.message.isNotBlank()) {
                    Text(block.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                block.items.forEach { item ->
                    Text("• $item", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                }
            }
        }
        "image" -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (block.title.isNotBlank()) {
                    Text(block.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (block.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = block.imageUrl,
                        contentDescription = block.title,
                        modifier = Modifier.fillMaxWidth().heightIn(max = block.imageHeightDp.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                if (block.message.isNotBlank()) {
                    Text(block.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
        "link" -> {
            if (block.externalUrl.isNotBlank()) {
                val uriHandler = LocalUriHandler.current
                TextButton(onClick = { runCatching { uriHandler.openUri(block.externalUrl) } }) {
                    Text(
                        block.actionLabel.ifBlank { block.title.ifBlank { "Mở liên kết" } },
                        fontSize = 10.sp
                    )
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
