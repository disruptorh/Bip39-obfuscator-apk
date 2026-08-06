package com.reimen.bip39obfuscator.crypto

import java.security.MessageDigest

/**
 * Transformación de la entropía 1:1 con transform_seed del Python (v1) y
 * extendido con scrypt (v2).
 *
 * XOR es involutivo: ofuscar y revertir usan la misma operación con el mismo
 * secreto, versión y salt.
 *
 * El secreto y los buffers de entropía/clave se limpian al finalizar
 * (mejor esfuerzo en la JVM). El salt es del llamador y no se limpia aquí.
 */
object SeedTransformer {

    fun transformSeed(
        entropy: ByteArray,
        entBits: Int,
        csBits: Int,
        secret: CharArray,
        salt: ByteArray,
        kdfVersion: KdfVersion,
        idxToWord: List<String>
    ): String {
        val key = KeyDerivation.deriveKey(kdfVersion, secret, salt, entBits / 8)

        // nueva_entropía = entropía XOR clave (byte a byte)
        val newEntropy = ByteArray(entropy.size) { i ->
            (entropy[i].toInt() xor key[i].toInt()).toByte()
        }
        key.wipe()

        // Recalcular checksum SHA-256 sobre la nueva entropía
        val sha256 = MessageDigest.getInstance("SHA-256").digest(newEntropy)
        val hashBits = sha256.toBitString()
        val newChecksum = hashBits.substring(0, csBits)

        // Reconstruir flujo binario: entropía + checksum
        val entropyBits = newEntropy.toBitString()
        val fullBits = entropyBits + newChecksum

        // Dividir en grupos de 11 bits -> palabras
        val wordCount = (entBits + csBits) / 11
        val words = (0 until wordCount).map { i ->
            val chunk = fullBits.substring(i * 11, i * 11 + 11)
            idxToWord[chunk.toInt(2)]
        }

        newEntropy.wipe()
        return words.joinToString(" ")
    }

    private fun ByteArray.toBitString(): String =
        joinToString("") { b -> (b.toInt() and 0xFF).toString(2).padStart(8, '0') }
}
