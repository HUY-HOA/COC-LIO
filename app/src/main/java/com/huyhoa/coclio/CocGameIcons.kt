package com.huyhoa.coclio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
internal fun CocGameIcon(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier.size(18.dp),
    contentDescription: String? = null
) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        modifier = modifier
    )
}

@Composable
internal fun CocResourceIcon(
    resource: String,
    modifier: Modifier = Modifier.size(18.dp)
) {
    val iconRes = when (resource.trim().lowercase(Locale.ROOT)) {
        "gold", "builder gold" -> R.drawable.ic_coc_gold
        "elixir", "builder elixir" -> R.drawable.ic_coc_elixir
        "dark elixir" -> R.drawable.ic_coc_dark_elixir
        "capital gold" -> R.drawable.ic_coc_capital_gold
        "shiny ore", "quặng sáng" -> R.drawable.ic_coc_ore_shiny
        "glowy ore", "quặng phát sáng" -> R.drawable.ic_coc_ore_glowy
        "starry ore", "quặng sao" -> R.drawable.ic_coc_ore_starry
        "gold or elixir", "builder gold or builder elixir" -> R.drawable.ic_coc_resource
        else -> R.drawable.ic_coc_resource
    }
    val localizedDescription = when (resource.trim().lowercase(Locale.ROOT)) {
        "gold" -> "Vàng"
        "builder gold" -> "Vàng thợ xây"
        "elixir" -> "Tinh chất"
        "builder elixir" -> "Tinh chất thợ xây"
        "dark elixir" -> "Dầu đen"
        "capital gold" -> "Vàng thủ đô"
        "shiny ore", "quặng sáng" -> "Quặng sáng"
        "glowy ore", "quặng phát sáng" -> "Quặng phát sáng"
        "starry ore", "quặng sao" -> "Quặng sao"
        "gold or elixir" -> "Vàng hoặc Tinh chất"
        "builder gold or builder elixir" -> "Vàng thợ xây hoặc Tinh chất thợ xây"
        else -> if (resource.any { it in "ăâđêôơưáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ" }) {
            resource
        } else {
            "Tài nguyên"
        }
    }
    CocGameIcon(iconRes, modifier, localizedDescription)
}
