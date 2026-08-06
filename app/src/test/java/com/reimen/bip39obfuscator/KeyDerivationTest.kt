package com.reimen.bip39obfuscator

import com.reimen.bip39obfuscator.crypto.KdfVersion
import com.reimen.bip39obfuscator.crypto.KeyDerivation
import com.reimen.bip39obfuscator.crypto.hexToBytes
import com.reimen.bip39obfuscator.crypto.toHex
import com.reimen.bip39obfuscator.crypto.wipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyDerivationTest {

    @Test
    fun deriveKeyV1MatchesPythonVectors() {
        for ((length, expectedHex) in TestVectors.deriveKeyVectors) {
            val secret = TestVectors.SECRET.toCharArray()
            val key = try {
                KeyDerivation.deriveKey(KdfVersion.V1_SHA256, secret, ByteArray(0), length)
            } finally {
                secret.wipe()
            }
            assertEquals(
                "deriveKey v1(length=$length) no coincide con el Python",
                expectedHex,
                key.toHex()
            )
        }
    }

    @Test
    fun deriveKeyV2ScryptMatchesPythonVectors() {
        val salt = TestVectors.scryptSaltHex.hexToBytes()
        try {
            for ((length, expectedHex) in TestVectors.scryptKeyVectors) {
                val secret = TestVectors.SECRET.toCharArray()
                val key = try {
                    KeyDerivation.deriveKey(KdfVersion.V2_SCRYPT, secret, salt, length)
                } finally {
                    secret.wipe()
                }
                assertEquals(
                    "scrypt(length=$length) no coincide con hashlib.scrypt (N=32768,r=8,p=1)",
                    expectedHex,
                    key.toHex()
                )
            }
        } finally {
            salt.wipe()
        }
    }

    @Test
    fun deriveKeyIsDeterministic() {
        val salt = TestVectors.scryptSaltHex.hexToBytes()
        try {
            val a = KeyDerivation.deriveKey(
                KdfVersion.V2_SCRYPT, TestVectors.SECRET.toCharArray(), salt, 32
            )
            val b = KeyDerivation.deriveKey(
                KdfVersion.V2_SCRYPT, TestVectors.SECRET.toCharArray(), salt, 32
            )
            assertEquals(a.toHex(), b.toHex())
        } finally {
            salt.wipe()
        }
    }

    @Test
    fun deriveKeyTruncatesToLength() {
        for (length in listOf(1, 2, 17, 31, 33, 64)) {
            val key = KeyDerivation.deriveKey(
                KdfVersion.V1_SHA256, TestVectors.SECRET.toCharArray(), ByteArray(0), length
            )
            assertEquals(length, key.size)
        }
    }

    @Test
    fun generateSaltProduces16RandomBytes() {
        val a = KeyDerivation.generateSalt()
        val b = KeyDerivation.generateSalt()
        try {
            assertEquals(16, a.size)
            assertEquals(16, b.size)
            assertEquals(32, a.toHex().length)
            assertNotEquals("Dos salts generados no deben ser iguales", a.toHex(), b.toHex())
        } finally {
            a.wipe()
            b.wipe()
        }
    }

    @Test
    fun scryptRequires16ByteSalt() {
        val secret = TestVectors.SECRET.toCharArray()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                KeyDerivation.deriveKey(KdfVersion.V2_SCRYPT, secret, ByteArray(0), 32)
            }
            assertThrows(IllegalArgumentException::class.java) {
                KeyDerivation.deriveKey(
                    KdfVersion.V2_SCRYPT, secret, "aabbcc".hexToBytes(), 32
                )
            }
        } finally {
            secret.wipe()
        }
    }
}
