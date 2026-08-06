package com.reimen.bip39obfuscator.crypto

import java.security.MessageDigest

/** Entropía (bytes) y parámetros de bits de una seed válida. */
data class SeedData(
    val entropy: ByteArray,
    val entBits: Int,
    val csBits: Int
)

/**
 * Validación completa de una seedphrase BIP-39 (1:1 con validate_seed del Python):
 *
 * 1. Verifica que el número de palabras sea 12/15/18/21/24.
 * 2. Verifica que cada palabra exista en la lista BIP-39.
 * 3. Reconstruye el flujo binario (11 bits por índice).
 * 4. Separa entropía (primeros ENT bits) del checksum (últimos CS bits).
 * 5. Recalcula SHA-256(entropía) y compara los primeros CS bits.
 *
 * Lanza [IllegalArgumentException] con un mensaje descriptivo ante cualquier error.
 */
object SeedValidator {

    fun validateSeed(
        mnemonic: String,
        wordToIdx: Map<String, Int>
    ): SeedData {
        val words = mnemonic.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val wordCount = words.size

        // -- Paso 1: número de palabras válido --
        val (entBits, csBits) = Bip39Params.forWordCount(wordCount)
            ?: throw IllegalArgumentException(
                "Cantidad de palabras inválida: $wordCount. Debe ser 12, 15, 18, 21 o 24."
            )

        // -- Pasos 2 y 3: verificar palabras y obtener índices --
        val indices = words.mapIndexed { index, word ->
            wordToIdx[word]
                ?: throw IllegalArgumentException(
                    "Palabra #${index + 1} '$word' no se encuentra en la lista BIP-39."
                )
        }

        // -- Paso 4: reconstruir el flujo binario (11 bits por índice) --
        val bitString = buildString {
            for (idx in indices) {
                append(idx.toString(2).padStart(11, '0'))
            }
        }

        // -- Paso 5: separar entropía y checksum --
        val entropyBitsStr = bitString.substring(0, entBits)
        val checksumBitsStr = bitString.substring(entBits)

        // Convertir entropía de bits a bytes (big-endian, respetando ceros iniciales)
        val entropy = ByteArray(entBits / 8) { i ->
            entropyBitsStr.substring(i * 8, i * 8 + 8).toInt(2).toByte()
        }

        // -- Paso 6: recalcular checksum con SHA-256 --
        val sha256 = MessageDigest.getInstance("SHA-256").digest(entropy)
        val hashBits = sha256.toBitString()
        val expectedChecksum = hashBits.substring(0, csBits)

        // -- Paso 7: comparar checksums --
        if (checksumBitsStr != expectedChecksum) {
            throw IllegalArgumentException(
                "Checksum incorrecto. Esperado: $expectedChecksum, obtenido: $checksumBitsStr."
            )
        }

        return SeedData(entropy = entropy, entBits = entBits, csBits = csBits)
    }

    private fun ByteArray.toBitString(): String =
        joinToString("") { b -> (b.toInt() and 0xFF).toString(2).padStart(8, '0') }
}
