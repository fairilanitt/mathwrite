package com.mathwrite.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mathwrite.app.R

enum class MathwriteIcon(@param:DrawableRes val drawableResId: Int) {
    CheckConnection(R.drawable.btn_check_connection),
    ChangeConnection(R.drawable.btn_change_connection),
    Connect(R.drawable.btn_connect),
    Scan(R.drawable.btn_scan),
    MathMode(R.drawable.btn_math_mode),
    SketchMode(R.drawable.btn_sketch_mode),
    Pen(R.drawable.btn_pen),
    StylusOnly(R.drawable.btn_stylus_only),
    Undo(R.drawable.btn_undo),
    Clear(R.drawable.btn_clear),
    SendLatex(R.drawable.btn_send_latex),
    SendSketch(R.drawable.btn_send_sketch),
    More(R.drawable.btn_more),
    Highlighter(R.drawable.btn_highlighter),
    Eraser(R.drawable.btn_eraser),
    Fill(R.drawable.btn_fill),
}

@Composable
fun ToolbarStrip(content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = androidx.compose.ui.graphics.Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun ToolbarGroup(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun GeneratedIconButton(
    icon: MathwriteIcon,
    contentDescription: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 48.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
                if (!enabled) disabled()
            }
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        color = androidx.compose.ui.graphics.Color(0xFF111827),
                        shape = RoundedCornerShape(14.dp),
                    )
                } else {
                    Modifier
                },
            )
            .padding(2.dp)
            .alpha(if (enabled) 1f else 0.42f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MathwriteIconImage(
            icon = icon,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun MathwriteIconImage(
    icon: MathwriteIcon,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(icon.drawableResId),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Composable
fun PaletteLabelIcon(icon: MathwriteIcon) {
    MathwriteIconImage(
        icon = icon,
        modifier = Modifier
            .width(48.dp)
            .height(30.dp),
    )
}
