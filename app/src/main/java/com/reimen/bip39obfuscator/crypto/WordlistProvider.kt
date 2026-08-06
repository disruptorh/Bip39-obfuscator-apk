package com.reimen.bip39obfuscator.crypto

import java.io.InputStream
import java.security.MessageDigest

/**
 * Carga la wordlist BIP-39 empaquetada en assets y verifica su integridad.
 *
 * El hash SHA-256 se compara contra una constante hardcodeada para detectar
 * manipulación del APK (repackaging) o corrupción del asset.
 */
object WordlistProvider {

    const val ASSET_NAME = "bip39_english.txt"

    /** SHA-256 de app/src/main/assets/bip39_english.txt (2048 palabras, inglés estándar). */
    const val EXPECTED_SHA256 = "2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda"

    data class Wordlist(
        val words: List<String>,
        val wordToIdx: Map<String, Int>
    )

    fun load(input: InputStream): Wordlist {
        val bytes = input.use { it.readBytes() }

        val actualSha = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
        if (actualSha != EXPECTED_SHA256) {
            throw IllegalStateException(
                "Integridad de la wordlist comprometida: hash SHA-256 no coincide " +
                    "(esperado $EXPECTED_SHA256, obtenido $actualSha)."
            )
        }

        val text = String(bytes, Charsets.UTF_8)
        val words = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (words.size != 2048) {
            throw IllegalStateException(
                "El archivo de palabras debe contener exactamente 2048 entradas, " +
                    "pero tiene ${words.size}."
            )
        }

        val wordToIdx = HashMap<String, Int>(words.size * 2)
        words.forEachIndexed { index, word -> wordToIdx[word] = index }

        return Wordlist(words = words, wordToIdx = wordToIdx)
    }
}
