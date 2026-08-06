package com.reimen.bip39obfuscator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reimen.bip39obfuscator.crypto.KdfVersion
import com.reimen.bip39obfuscator.crypto.KeyDerivation
import com.reimen.bip39obfuscator.crypto.SeedTransformer
import com.reimen.bip39obfuscator.crypto.SeedValidator
import com.reimen.bip39obfuscator.crypto.WordlistProvider
import com.reimen.bip39obfuscator.crypto.hexToBytes
import com.reimen.bip39obfuscator.crypto.toHex
import com.reimen.bip39obfuscator.crypto.wipe
import com.reimen.bip39obfuscator.io.SeedFileParser
import com.reimen.bip39obfuscator.security.ClipboardGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface WordlistStatus {
    data object Loading : WordlistStatus
    data object Ready : WordlistStatus
    data class Error(val message: String) : WordlistStatus
}

data class UiState(
    val wordlistStatus: WordlistStatus = WordlistStatus.Loading,
    val seedInput: String = "",
    val secret: String = "",
    val confirmSecret: String = "",
    val showSecret: Boolean = false,
    val showConfirmSecret: Boolean = false,
    val kdfVersion: KdfVersion = KdfVersion.V2_SCRYPT,
    val saltInput: String = "",
    val isTransforming: Boolean = false,
    val outputSeed: String = "",
    val usedSaltHex: String = "",
    val statusMessage: String = "",
    val statusIsError: Boolean = false,
    val clipboardStatus: String = ""
)

/**
 * ViewModel principal.
 *
 * App 100 % airgapped: no lee ni escribe archivos, no toca el almacenamiento
 * externo ni requiere permisos. Una sola seed se transforma en memoria con
 * la clave secreta y el resultado se muestra en pantalla.
 *
 * Ningún valor sensible (secreto, seed, entropía) se loguea jamás.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var wordlist: WordlistProvider.Wordlist? = null
    private var copyJob: Job? = null

    init {
        loadWordlist()
    }

    private fun loadWordlist() {
        _uiState.update { it.copy(wordlistStatus = WordlistStatus.Loading) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val input = getApplication<Application>()
                        .assets.open(WordlistProvider.ASSET_NAME)
                    WordlistProvider.load(input)
                }
            }
            result.onSuccess { loaded ->
                wordlist = loaded
                _uiState.update { it.copy(wordlistStatus = WordlistStatus.Ready) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        wordlistStatus = WordlistStatus.Error(
                            "No se pudo cargar la lista BIP-39: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    fun onSeedInputChange(value: String) =
        _uiState.update { it.copy(seedInput = value, statusMessage = "") }

    fun onSecretChange(value: String) = _uiState.update { it.copy(secret = value) }

    fun onConfirmSecretChange(value: String) = _uiState.update { it.copy(confirmSecret = value) }

    fun toggleShowSecret() = _uiState.update { it.copy(showSecret = !it.showSecret) }

    fun toggleShowConfirmSecret() =
        _uiState.update { it.copy(showConfirmSecret = !it.showConfirmSecret) }

    fun onKdfVersionSelected(version: KdfVersion) =
        _uiState.update { it.copy(kdfVersion = version) }

    fun onSaltChange(value: String) = _uiState.update { it.copy(saltInput = value) }

    /**
     * Valida la seed, la transforma con la clave (XOR) y muestra el resultado.
     * XOR es involutivo: aplicar de nuevo la misma transformación con el mismo
     * secreto, versión y salt revierte la seed original.
     *
     * v2 (scrypt): si el campo de salt está vacío se genera un salt aleatorio
     * nuevo por transformación (modo ofuscar) y se muestra junto al resultado;
     * si el usuario pega un salt guardado, se usa ese (modo revertir).
     * v1 (SHA-256): ignora el salt, comportamiento byte a byte original.
     */
    fun transform() {
        val state = _uiState.value
        val wl = wordlist

        if (state.isTransforming) return
        if (wl == null) {
            setError("La lista BIP-39 aún no está cargada.")
            return
        }
        if (state.seedInput.isBlank()) {
            setError("Ingresa una seedphrase BIP-39.")
            return
        }
        if (state.secret.isEmpty()) {
            setError("Ingresa una clave secreta.")
            return
        }
        if (state.secret != state.confirmSecret) {
            setError("La clave secreta y la confirmación no coinciden.")
            return
        }

        val kdf = state.kdfVersion
        val saltHex = state.saltInput.trim().lowercase()

        // Validar el salt (v2) antes de lanzar la corrutina
        if (kdf == KdfVersion.V2_SCRYPT && saltHex.isNotEmpty()) {
            val salt = runCatching { saltHex.hexToBytes() }.getOrElse {
                setError("Salt inválido: debe ser hexadecimal válido.")
                return
            }
            if (salt.size != KeyDerivation.SALT_BYTES) {
                setError("Salt inválido: debe tener ${KeyDerivation.SALT_BYTES} bytes (32 caracteres hexadecimales).")
                return
            }
            salt.wipe()
        }

        val parsed = SeedFileParser.parseSeedsFromFile(state.seedInput)
        if (parsed.size != 1) {
            setError(
                if (parsed.isEmpty()) {
                    "No se encontró una seedphrase en el texto ingresado."
                } else {
                    "Ingresa exactamente una sola seedphrase (12, 15, 18, 21 o 24 palabras)."
                }
            )
            return
        }

        _uiState.update {
            it.copy(
                isTransforming = true,
                outputSeed = "",
                usedSaltHex = "",
                clipboardStatus = "",
                statusMessage = ""
            )
        }

        viewModelScope.launch {
            val secretArray = state.secret.toCharArray()
            try {
                val result = withContext(Dispatchers.Default) {
                    runCatching {
                        val seedData = SeedValidator.validateSeed(parsed.first(), wl.wordToIdx)
                        try {
                            val saltBytes: ByteArray = when (kdf) {
                                KdfVersion.V1_SHA256 -> ByteArray(0)
                                KdfVersion.V2_SCRYPT ->
                                    if (saltHex.isEmpty()) KeyDerivation.generateSalt()
                                    else saltHex.hexToBytes()
                            }
                            val usedSalt = if (kdf == KdfVersion.V2_SCRYPT) saltBytes.toHex() else ""
                            try {
                                val output = SeedTransformer.transformSeed(
                                    entropy = seedData.entropy,
                                    entBits = seedData.entBits,
                                    csBits = seedData.csBits,
                                    secret = secretArray,
                                    salt = saltBytes,
                                    kdfVersion = kdf,
                                    idxToWord = wl.words
                                )
                                output to usedSalt
                            } finally {
                                saltBytes.wipe()
                            }
                        } finally {
                            seedData.entropy.wipe()
                        }
                    }
                }
                result.onSuccess { (output, usedSalt) ->
                    val message = when {
                        kdf == KdfVersion.V2_SCRYPT && usedSalt.isNotEmpty() && saltHex.isEmpty() ->
                            "Transformada con v2 · scrypt. Salt: $usedSalt — guárdalo, sin él no podrás revertir."
                        kdf == KdfVersion.V2_SCRYPT ->
                            "Transformada con v2 · scrypt."
                        else ->
                            "Transformada con v1 · SHA-256."
                    }
                    _uiState.update {
                        it.copy(
                            outputSeed = output,
                            usedSaltHex = usedSalt,
                            statusMessage = message,
                            statusIsError = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            statusMessage = e.message ?: "Error desconocido.",
                            statusIsError = true
                        )
                    }
                }
            } finally {
                secretArray.wipe()
                _uiState.update { it.copy(isTransforming = false) }
            }
        }
    }

    /** Copia la seed resultante al portapapeles marcada como sensible y con auto-borrado. */
    fun copyOutput() {
        val output = _uiState.value.outputSeed
        if (output.isEmpty()) return

        copyJob?.cancel()
        val context = getApplication<Application>()

        val job = ClipboardGuard.copySensitive(
            context = context,
            label = "seed",
            value = output,
            autoClearMs = CLIPBOARD_CLEAR_MS,
            scope = viewModelScope
        )
        copyJob = job

        _uiState.update {
            it.copy(clipboardStatus = "Copiado — se limpiará del portapapeles en 30 s")
        }

        viewModelScope.launch {
            job.join()
            if (copyJob === job) {
                _uiState.update { it.copy(clipboardStatus = "Portapapeles limpiado.") }
            }
        }
    }

    /** Copia el salt de v2 usado en la última transformación (mismo borrado seguro). */
    fun copySalt() {
        val salt = _uiState.value.usedSaltHex
        if (salt.isEmpty()) return

        copyJob?.cancel()
        val context = getApplication<Application>()

        val job = ClipboardGuard.copySensitive(
            context = context,
            label = "salt",
            value = salt,
            autoClearMs = CLIPBOARD_CLEAR_MS,
            scope = viewModelScope
        )
        copyJob = job

        _uiState.update {
            it.copy(clipboardStatus = "Salt copiado — se limpiará del portapapeles en 30 s")
        }

        viewModelScope.launch {
            job.join()
            if (copyJob === job) {
                _uiState.update { it.copy(clipboardStatus = "Portapapeles limpiado.") }
            }
        }
    }

    private fun setError(message: String) =
        _uiState.update { it.copy(statusMessage = message, statusIsError = true) }

    private companion object {
        const val CLIPBOARD_CLEAR_MS = 30_000L
    }
}
