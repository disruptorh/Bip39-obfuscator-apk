package com.reimen.bip39obfuscator

import com.reimen.bip39obfuscator.crypto.WordlistProvider

/** Wordlist BIP-39 cargada desde test/resources (misma verificación de hash). */
object TestWordlist {
    val wordlist: WordlistProvider.Wordlist by lazy {
        val stream = checkNotNull(
            TestWordlist::class.java.getResourceAsStream("/bip39_english.txt")
        ) { "No se encontró bip39_english.txt en test resources" }
        WordlistProvider.load(stream)
    }
}
