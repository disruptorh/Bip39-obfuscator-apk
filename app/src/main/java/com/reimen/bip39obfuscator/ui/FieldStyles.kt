package com.reimen.bip39obfuscator.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val FieldShape = RoundedCornerShape(16.dp)

val FieldContainerColor = Color(0xFF252536)
val FieldBorderColor = Color(0xFF3A3A50)
val FieldTextColor = Color(0xFFCDD6F4)
val FieldLabelColor = Color(0xFFA6ADC8)
val AccentColor = Color(0xFF89B4FA)

@Composable
fun secureFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentColor,
    unfocusedBorderColor = FieldBorderColor,
    focusedContainerColor = FieldContainerColor,
    unfocusedContainerColor = FieldContainerColor,
    cursorColor = AccentColor,
    focusedLabelColor = AccentColor,
    unfocusedLabelColor = FieldLabelColor,
    focusedTextColor = FieldTextColor,
    unfocusedTextColor = FieldTextColor,
    focusedPlaceholderColor = FieldLabelColor,
    unfocusedPlaceholderColor = FieldLabelColor
)
