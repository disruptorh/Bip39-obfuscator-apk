package com.reimen.bip39obfuscator.crypto

/**
 * Parámetros BIP-39 por número de palabras.
 * word_count -> (entropy_bits, checksum_bits)
 */
object Bip39Params {

    private val params = mapOf(
        12 to (128 to 4),
        15 to (160 to 5),
        18 to (192 to 6),
        21 to (224 to 7),
        24 to (256 to 8)
    )

    /** Devuelve (entropy_bits, checksum_bits) para un conteo válido, o null. */
    fun forWordCount(wordCount: Int): Pair<Int, Int>? = params[wordCount]
}
