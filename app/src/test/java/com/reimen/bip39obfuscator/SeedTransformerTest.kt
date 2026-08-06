package com.reimen.bip39obfuscator

import com.reimen.bip39obfuscator.crypto.KdfVersion
import com.reimen.bip39obfuscator.crypto.SeedTransformer
import com.reimen.bip39obfuscator.crypto.SeedValidator
import com.reimen.bip39obfuscator.crypto.hexToBytes
import com.reimen.bip39obfuscator.crypto.wipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeedTransformerTest {

    private val wordlist = TestWordlist.wordlist

    @Test
    fun transformV1MatchesPythonVectors() {
        for (vector in TestVectors.transformVectors) {
            val secret = TestVectors.SECRET.toCharArray()
            try {
                val seedData = SeedValidator.validateSeed(vector.input, wordlist.wordToIdx)
                val output = try {
                    SeedTransformer.transformSeed(
                        entropy = seedData.entropy,
                        entBits = seedData.entBits,
                        csBits = seedData.csBits,
                        secret = secret,
                        salt = ByteArray(0),
                        kdfVersion = KdfVersion.V1_SHA256,
                        idxToWord = wordlist.words
                    )
                } finally {
                    seedData.entropy.wipe()
                }
                assertEquals("Falla transform v1 para: ${vector.input}", vector.output, output)
            } finally {
                secret.wipe()
            }
        }
    }

    @Test
    fun transformV2MatchesPythonVectors() {
        for (vector in TestVectors.transformVectorsV2) {
            val secret = TestVectors.SECRET.toCharArray()
            val salt = TestVectors.scryptSaltHex.hexToBytes()
            try {
                val seedData = SeedValidator.validateSeed(vector.input, wordlist.wordToIdx)
                val output = try {
                    SeedTransformer.transformSeed(
                        entropy = seedData.entropy,
                        entBits = seedData.entBits,
                        csBits = seedData.csBits,
                        secret = secret,
                        salt = salt,
                        kdfVersion = KdfVersion.V2_SCRYPT,
                        idxToWord = wordlist.words
                    )
                } finally {
                    seedData.entropy.wipe()
                }
                assertEquals("Falla transform v2 para: ${vector.input}", vector.output, output)
            } finally {
                secret.wipe()
                salt.wipe()
            }
        }
    }

    @Test
    fun xorIsInvolutiveRoundTripV1() {
        for (vector in TestVectors.transformVectors) {
            val secret = TestVectors.SECRET.toCharArray()
            try {
                val first = SeedValidator.validateSeed(vector.input, wordlist.wordToIdx)
                val once = SeedTransformer.transformSeed(
                    entropy = first.entropy,
                    entBits = first.entBits,
                    csBits = first.csBits,
                    secret = secret,
                    salt = ByteArray(0),
                    kdfVersion = KdfVersion.V1_SHA256,
                    idxToWord = wordlist.words
                )
                first.entropy.wipe()

                val second = SeedValidator.validateSeed(once, wordlist.wordToIdx)
                val twice = SeedTransformer.transformSeed(
                    entropy = second.entropy,
                    entBits = second.entBits,
                    csBits = second.csBits,
                    secret = secret,
                    salt = ByteArray(0),
                    kdfVersion = KdfVersion.V1_SHA256,
                    idxToWord = wordlist.words
                )
                second.entropy.wipe()

                assertEquals("Round-trip v1 falló para: ${vector.input}", vector.input, twice)
            } finally {
                secret.wipe()
            }
        }
    }

    @Test
    fun xorIsInvolutiveRoundTripV2() {
        for (vector in TestVectors.transformVectorsV2) {
            val secret = TestVectors.SECRET.toCharArray()
            val salt = TestVectors.scryptSaltHex.hexToBytes()
            try {
                val first = SeedValidator.validateSeed(vector.input, wordlist.wordToIdx)
                val once = SeedTransformer.transformSeed(
                    entropy = first.entropy,
                    entBits = first.entBits,
                    csBits = first.csBits,
                    secret = secret,
                    salt = salt,
                    kdfVersion = KdfVersion.V2_SCRYPT,
                    idxToWord = wordlist.words
                )
                first.entropy.wipe()

                val second = SeedValidator.validateSeed(once, wordlist.wordToIdx)
                val twice = SeedTransformer.transformSeed(
                    entropy = second.entropy,
                    entBits = second.entBits,
                    csBits = second.csBits,
                    secret = secret,
                    salt = salt,
                    kdfVersion = KdfVersion.V2_SCRYPT,
                    idxToWord = wordlist.words
                )
                second.entropy.wipe()

                assertEquals("Round-trip v2 falló para: ${vector.input}", vector.input, twice)
            } finally {
                secret.wipe()
                salt.wipe()
            }
        }
    }

    @Test
    fun differentSecretProducesDifferentResult() {
        val vector = TestVectors.transformVectors.first()
        val secretA = TestVectors.SECRET.toCharArray()
        val secretB = "otro-secreto-diferente".toCharArray()
        val a = try {
            val seedData = SeedValidator.validateSeed(vector.input, wordlist.wordToIdx)
            try {
                SeedTransformer.transformSeed(
                    entropy = seedData.entropy,
                    entBits = seedData.entBits,
                    csBits = seedData.csBits,
                    secret = secretA,
                    salt = ByteArray(0),
                    kdfVersion = KdfVersion.V1_SHA256,
                    idxToWord = wordlist.words
                )
            } finally {
                seedData.entropy.wipe()
            }
        } finally {
            secretA.wipe()
        }
        val b = try {
            val seedData = SeedValidator.validateSeed(vector.input, wordlist.wordToIdx)
            try {
                SeedTransformer.transformSeed(
                    entropy = seedData.entropy,
                    entBits = seedData.entBits,
                    csBits = seedData.csBits,
                    secret = secretB,
                    salt = ByteArray(0),
                    kdfVersion = KdfVersion.V1_SHA256,
                    idxToWord = wordlist.words
                )
            } finally {
                seedData.entropy.wipe()
            }
        } finally {
            secretB.wipe()
        }
        assertNotEquals(a, b)
    }

    @Test
    fun differentKdfProducesDifferentResult() {
        val vector = TestVectors.transformVectors.first()
        val secret = TestVectors.SECRET.toCharArray()
        val salt = TestVectors.scryptSaltHex.hexToBytes()
        try {
            val seedV1 = SeedValidator.validateSeed(vector.input, wordlist.wordToIdx)
            val outV1 = try {
                SeedTransformer.transformSeed(
                    entropy = seedV1.entropy,
                    entBits = seedV1.entBits,
                    csBits = seedV1.csBits,
                    secret = secret,
                    salt = ByteArray(0),
                    kdfVersion = KdfVersion.V1_SHA256,
                    idxToWord = wordlist.words
                )
            } finally {
                seedV1.entropy.wipe()
            }

            val seedV2 = SeedValidator.validateSeed(vector.input, wordlist.wordToIdx)
            val outV2 = try {
                SeedTransformer.transformSeed(
                    entropy = seedV2.entropy,
                    entBits = seedV2.entBits,
                    csBits = seedV2.csBits,
                    secret = secret,
                    salt = salt,
                    kdfVersion = KdfVersion.V2_SCRYPT,
                    idxToWord = wordlist.words
                )
            } finally {
                seedV2.entropy.wipe()
            }

            assertNotEquals("v1 y v2 no deben producir lo mismo", outV1, outV2)
        } finally {
            secret.wipe()
            salt.wipe()
        }
    }
}
