package com.reimen.bip39obfuscator

import com.reimen.bip39obfuscator.crypto.SeedValidator
import com.reimen.bip39obfuscator.crypto.toHex
import com.reimen.bip39obfuscator.crypto.wipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedValidatorTest {

    private val wordlist = TestWordlist.wordlist

    @Test
    fun valid12WordZeroSeed() {
        val data = SeedValidator.validateSeed(
            TestVectors.transformVectors[0].input,
            wordlist.wordToIdx
        )
        assertEquals(128, data.entBits)
        assertEquals(4, data.csBits)
        assertEquals("00000000000000000000000000000000", data.entropy.toHex())
        data.entropy.wipe()
    }

    @Test
    fun valid24WordZeroSeed() {
        val data = SeedValidator.validateSeed(
            TestVectors.transformVectors[5].input,
            wordlist.wordToIdx
        )
        assertEquals(256, data.entBits)
        assertEquals(8, data.csBits)
        data.entropy.wipe()
    }

    @Test
    fun invalidWordCountThrows() {
        val mnemonic = List(13) { "abandon" }.joinToString(" ")
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SeedValidator.validateSeed(mnemonic, wordlist.wordToIdx)
        }
        assertTrue(ex.message!!.contains("Cantidad de palabras inválida: 13"))
        assertTrue(ex.message!!.contains("Debe ser 12, 15, 18, 21 o 24."))
    }

    @Test
    fun unknownWordThrows() {
        val mnemonic = List(11) { "abandon" }.joinToString(" ") + " zzzz"
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SeedValidator.validateSeed(mnemonic, wordlist.wordToIdx)
        }
        assertTrue(
            ex.message!!.contains("Palabra #12 'zzzz' no se encuentra en la lista BIP-39.")
        )
    }

    @Test
    fun invalidChecksumThrows() {
        val mnemonic = List(12) { "abandon" }.joinToString(" ")
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SeedValidator.validateSeed(mnemonic, wordlist.wordToIdx)
        }
        assertTrue(ex.message!!.contains("Checksum incorrecto."))
        assertTrue(ex.message!!.contains("Esperado: 0011, obtenido: 0000."))
    }

    @Test
    fun allTransformInputsAreValid() {
        for (vector in TestVectors.transformVectors) {
            val data = SeedValidator.validateSeed(vector.input, wordlist.wordToIdx)
            data.entropy.wipe()
        }
    }
}
