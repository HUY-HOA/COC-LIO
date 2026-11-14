package com.huyhoa.coclio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun RemoteServiceNotices(
    content: RemoteContentState,
    onAction: (String) -> Unit
) {
    val announcement = content.announcement
    if (!content.maintenanceEnabled && announcement == null) return

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (content.maintenanceEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        content.maintenanceTitle.ifBlank { "THÔNG BÁO BẢO TRÌ" },
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        content.maintenanceMessage.ifBlank { "Một số chức năng có thể tạm thời không hoạt động." },
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 10.sp
                    )
                }
            }
        }

        announcement?.let { notice ->
            val container = when (notice.severity) {
                "error" -> MaterialTheme.colorScheme.errorContainer
                "warning" -> MaterialTheme.colorScheme.tertiaryContainer
                "success" -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = container),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (notice.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = notice.imageUrl,
                            contentDescription = notice.title,
                            modifier = Modifier.size(width = 76.dp, height = 64.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (notice.title.isNotBlank()) {
                            Text(notice.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        if (notice.message.isNotBlank()) {
                            Text(notice.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        if (notice.actionId.isNotBlank() && notice.actionLabel.isNotBlank()) {
                            Button(onClick = { onAction(notice.actionId) }) {
                                Text(notice.actionLabel, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteGuidesSection(
    guides: List<RemoteGuide>,
    links: List<RemoteContentLink>
) {
    if (guides.isEmpty() && links.isEmpty()) return
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 190.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("HƯỚNG DẪN VÀ LIÊN KẾT", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
        guides.forEach { guide ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(guide.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    if (guide.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = guide.imageUrl,
                            contentDescription = guide.title,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 110.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    Text(guide.body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
        links.forEach { link ->
            TextButton(onClick = { runCatching { uriHandler.openUri(link.url) } }) {
                Text(link.label, fontSize = 10.sp)
            }
        }
    }
}
