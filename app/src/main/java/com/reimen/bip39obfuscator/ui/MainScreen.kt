package com.reimen.bip39obfuscator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reimen.bip39obfuscator.crypto.KdfVersion

private val BackgroundStart = Color(0xFF0E0E1A)
private val BackgroundEnd = Color(0xFF1E1E2E)
private val CardColor = Color(0xFF1A1A2A)
private val SuccessColor = Color(0xFFA6E3A1)

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()

    val ready = state.wordlistStatus is WordlistStatus.Ready

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BackgroundStart, BackgroundEnd))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header()
            WordlistBadge(state.wordlistStatus)

            SectionLabel("Seed phrase")

            SeedInputField(
                value = state.seedInput,
                onValueChange = viewModel::onSeedInputChange
            )

            SectionLabel("Clave secreta")

            SecretField(
                value = state.secret,
                onValueChange = viewModel::onSecretChange,
                label = "Secreto",
                show = state.showSecret,
                onToggleShow = viewModel::toggleShowSecret
            )
            SecretField(
                value = state.confirmSecret,
                onValueChange = viewModel::onConfirmSecretChange,
                label = "Confirmar secreto",
                show = state.showConfirmSecret,
                onToggleShow = viewModel::toggleShowConfirmSecret
            )

            SectionLabel("Método de derivación")

            KdfVersion.entries.forEach { version ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.onKdfVersionSelected(version) }
                        .padding(vertical = 2.dp)
                ) {
                    RadioButton(
                        selected = state.kdfVersion == version,
                        onClick = { viewModel.onKdfVersionSelected(version) }
                    )
                    Text(
                        text = version.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FieldTextColor
                    )
                }
            }

            if (state.kdfVersion == KdfVersion.V2_SCRYPT) {
                OutlinedTextField(
                    value = state.saltInput,
                    onValueChange = viewModel::onSaltChange,
                    label = { Text("Salt (hex)") },
                    placeholder = {
                        Text(
                            "32 caracteres hexadecimales…",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    singleLine = true,
                    shape = FieldShape,
                    colors = secureFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        autoCorrect = false,
                        capitalization = KeyboardCapitalization.None
                    ),
                    supportingText = {
                        Text(
                            "Vacío = genera un salt nuevo. Para revertir, pega el salt guardado.",
                            style = MaterialTheme.typography.labelSmall,
                            color = FieldLabelColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = viewModel::transform,
                enabled = ready && !state.isTransforming,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = if (state.isTransforming) "Transformando…" else "⚡ Transformar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            AnimatedVisibility(visible = state.outputSeed.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Resultado")

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = CardColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.outputSeed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = FieldTextColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }

                    if (state.usedSaltHex.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF232A4A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Salt (guárdalo junto a la seed para revertir)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentColor
                                )
                                Text(
                                    text = state.usedSaltHex,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = FieldTextColor
                                )
                            }
                        }
                    }

                    Button(
                        onClick = viewModel::copyOutput,
                        enabled = !state.isTransforming,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("📋 Copiar seed")
                    }

                    if (state.usedSaltHex.isNotEmpty()) {
                        TextButton(
                            onClick = viewModel::copySalt,
                            enabled = !state.isTransforming,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Copiar salt", color = AccentColor)
                        }
                    }

                    if (state.clipboardStatus.isNotEmpty()) {
                        Text(
                            text = state.clipboardStatus,
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentColor,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            if (state.statusMessage.isNotEmpty()) {
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.statusIsError) MaterialTheme.colorScheme.error else SuccessColor
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "XOR es involutivo: aplicar de nuevo la misma transformación con la misma " +
                    "clave, versión y salt revierte la seed original.",
                style = MaterialTheme.typography.bodySmall,
                color = FieldLabelColor
            )
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "🔐 BIP-39 Obfuscator",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Ofusca y revierte seedphrases, 100 % offline.",
            style = MaterialTheme.typography.bodySmall,
            color = FieldLabelColor
        )
    }
}

@Composable
private fun WordlistBadge(status: WordlistStatus) {
    val (text, color, bg) = when (status) {
        WordlistStatus.Loading ->
            Triple("Cargando lista BIP-39…", FieldLabelColor, FieldContainerColor)

        WordlistStatus.Ready ->
            Triple("Wordlist BIP-39 verificada · SHA-256 ✓", AccentColor, Color(0xFF232A4A))

        is WordlistStatus.Error ->
            Triple(status.message, MaterialTheme.colorScheme.error, Color(0xFF3A1E2B))
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        color = FieldLabelColor
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SeedInputField(
    value: String,
    onValueChange: (String) -> Unit
) {
    CompositionLocalProvider(LocalAutofill provides null) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    "abandon abandon abandon …\no una palabra por línea",
                    fontFamily = FontFamily.Monospace
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                lineHeight = 22.sp
            ),
            minLines = 3,
            maxLines = 6,
            shape = FieldShape,
            colors = secureFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { password() }
        )
    }
}
