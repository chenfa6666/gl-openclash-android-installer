package com.chenfa.openclashinstaller.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

// Liquid Glass 统一形状：连续大圆角
object GlassShapes {
    val card: Shape = RoundedCornerShape(24.dp)
    val button: Shape = RoundedCornerShape(20.dp)
    val dialog: Shape = RoundedCornerShape(28.dp)
    val logPanel: Shape = RoundedCornerShape(18.dp)
    val snack: Shape = RoundedCornerShape(18.dp)

    /** 顶栏：贴顶全宽，仅底部圆角，像一块悬浮玻璃 */
    val bar: Shape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 0.dp,
        bottomStart = 22.dp,
        bottomEnd = 22.dp,
    )
}
