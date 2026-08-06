package com.reimen.bip39obfuscator.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Campo de texto seguro para el secreto.
 *
 * - Oculto con PasswordVisualTransformation + toggle de visibilidad.
 * - Sin autocorrección, sin mayúsculas automáticas.
 * - Autofill desactivado (semantics password + LocalAutofill = null) para que
 *   el teclado (Gboard/SwiftKey) no sincronice sugerencias a la nube.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    show: Boolean,
    onToggleShow: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalAutofill provides null) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            shape = FieldShape,
            colors = secureFieldColors(),
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None
            ),
            trailingIcon = {
                IconButton(onClick = onToggleShow) {
                    Text(if (show) "🙈" else "👁")
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .semantics { password() }
        )
    }
}
