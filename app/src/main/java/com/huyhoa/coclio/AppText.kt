package com.huyhoa.coclio

import android.content.Context
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

object AppText {
    @Suppress("UNUSED_PARAMETER")
    fun translate(context: Context, source: String): String = source
}

fun clearLegacyLanguagePreference(context: Context) {
    context.getSharedPreferences("coc_lio_language", Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

fun appErrorMessage(message: String?, fallback: String): String {
    val value = message?.trim().orEmpty()
    if (value.isBlank()) return fallback
    if (Regex("[À-ỹ]").containsMatchIn(value)) return value

    val normalized = value.lowercase(java.util.Locale.ROOT)
    return when {
        normalized.contains("permission_denied") ||
            normalized.contains("missing or insufficient permissions") ->
            "Tài khoản hiện không có quyền truy cập dữ liệu này. Vui lòng đăng nhập lại hoặc liên hệ quản lý ứng dụng."
        normalized.contains("network") ||
            normalized.contains("unavailable") ||
            normalized.contains("unable to resolve host") ||
            normalized.contains("timed out") ||
            normalized.contains("timeout") ->
            "Không thể kết nối mạng. Vui lòng kiểm tra kết nối rồi thử lại."
        normalized.contains("too many requests") ->
            "Bạn thao tác quá nhanh. Vui lòng chờ một lúc rồi thử lại."
        normalized.contains("invalid-credential") ||
            normalized.contains("credential is malformed") ||
            normalized.contains("invalid id token") ->
            "Thông tin đăng nhập Google không hợp lệ. Vui lòng đăng nhập lại."
        normalized.contains("email address is already in use") ->
            "Địa chỉ email này đã được sử dụng cho một tài khoản khác."
        else -> fallback
    }
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

suspend fun SnackbarHostState.showAppSnackbar(
    @Suppress("UNUSED_PARAMETER") context: Context,
    message: String
): SnackbarResult = showSnackbar(message)

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}
