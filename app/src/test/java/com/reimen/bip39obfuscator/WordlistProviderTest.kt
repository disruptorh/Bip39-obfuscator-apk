package com.reimen.bip39obfuscator

import com.reimen.bip39obfuscator.crypto.WordlistProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WordlistProviderTest {

    @Test
    fun loadsExactly2048Words() {
        assertEquals(2048, TestWordlist.wordlist.words.size)
    }

    @Test
    fun wordToIdxIsBijective() {
        val wordlist = TestWordlist.wordlist
        assertEquals(2048, wordlist.wordToIdx.size)
        wordlist.words.forEachIndexed { index, word ->
            assertEquals(index, wordlist.wordToIdx[word])
        }
    }

    @Test
    fun firstAndLastWords() {
        assertEquals("abandon", TestWordlist.wordlist.words.first())
        assertEquals("zoo", TestWordlist.wordlist.words.last())
    }

    @Test
    fun expectedHashConstantMatchesAsset() {
        // TestWordlist.load() verifica el hash contra EXPECTED_SHA256;
        // si llegó aquí sin lanzar excepción, la constante es correcta.
        assertTrue(TestWordlist.wordlist.words.isNotEmpty())
    }

    @Test
    fun tamperedStreamIsRejected() {
        val fake = (0 until 2048).joinToString("\n") { "word$it" }
        val ex = assertThrows(IllegalStateException::class.java) {
            WordlistProvider.load(fake.byteInputStream())
        }
        assertTrue(ex.message!!.contains("Integridad de la wordlist comprometida"))
    }

    @Test
    fun wrongWordCountIsRejected() {
        // La verificación de hash corre primero; un input manipulado nunca llega
        // a la validación de conteo. Solo interesa que el cargador lo rechace.
        val tooFew = (0 until 100).joinToString("\n") { "word$it" }
        assertThrows(IllegalStateException::class.java) {
            WordlistProvider.load(tooFew.byteInputStream())
        }
    }
}
