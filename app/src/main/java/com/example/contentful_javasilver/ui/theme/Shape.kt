package com.example.contentful_javasilver.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 のデフォルト Shapes を設定
// 必要に応じて角丸のサイズなどを調整してください
val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp), // M3ではSmallと同じ場合も多いが、用途に応じて変更可
    large = RoundedCornerShape(0.dp)   // 例: BottomSheetなどは角丸なし
    /* 他の形状 (extraSmall, extraLarge) も必要に応じて定義できます
       https://m3.material.io/styles/shape/shape-scale-tokens
    */
) 