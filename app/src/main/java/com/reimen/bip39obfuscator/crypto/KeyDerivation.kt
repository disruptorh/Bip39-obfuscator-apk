package com.reimen.bip39obfuscator.crypto

import org.bouncycastle.crypto.generators.SCrypt
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Derivación de clave.
 *
 * v1 (SHA-256, legado): 1:1 con derive_key del Python:
 *   key = SHA-256(secreto_utf8 || contador_4bytes_big_endian)
 *   concatenado hasta cubrir `length` bytes, truncado al final.
 *
 * v2 (scrypt): scrypt(secreto_utf8, salt, N=32768, r=8, p=1, dklen=length).
 *   El salt de 16 bytes es obligatorio y debe persistirse junto al resultado
 *   para poder revertir. Los parámetros N/r/p están fijados en esta versión:
 *   NO cambiar sin crear una versión nueva (v3), o las seeds v2 dejarán de
 *   ser reversibles.
 */
object KeyDerivation {

    /** Parámetros scrypt fijos de v2 — NO cambiar sin versionar de nuevo. */
    private const val SCRYPT_N = 32768 // 2^15
    private const val SCRYPT_R = 8
    private const val SCRYPT_P = 1
    const val SALT_BYTES = 16

    fun deriveKey(
        version: KdfVersion,
        secret: CharArray,
        salt: ByteArray,
        length: Int
    ): ByteArray = when (version) {
        KdfVersion.V1_SHA256 -> deriveKeySha256(secret, length)
        KdfVersion.V2_SCRYPT -> deriveKeyScrypt(secret, salt, length)
    }

    // ── v1: método original, sin salt (se mantiene intacto para compatibilidad) ──
    private fun deriveKeySha256(secret: CharArray, length: Int): ByteArray {
        val secretBytes = secret.toUtf8Bytes()
        val md = MessageDigest.getInstance("SHA-256")
        // Capacidad exacta: el resultado será exactamente `length` bytes.
        val out = ByteArrayOutputStream(length)
        var counter = 0

        while (out.size() < length) {
            md.reset()
            md.update(secretBytes)
            md.update(
                byteArrayOf(
                    (counter ushr 24).toByte(),
                    (counter ushr 16).toByte(),
                    (counter ushr 8).toByte(),
                    counter.toByte()
                )
            )
            val digest = md.digest()
            val remaining = length - out.size()
            out.write(digest, 0, minOf(digest.size, remaining))
            digest.wipe()
            counter++
        }

        secretBytes.wipe()
        val result = out.toByteArray()
        out.reset()
        return result
    }

    // ── v2: scrypt con salt obligatorio ──
    private fun deriveKeyScrypt(secret: CharArray, salt: ByteArray, length: Int): ByteArray {
        require(salt.size == SALT_BYTES) {
            "Salt de v2 debe tener $SALT_BYTES bytes, recibidos ${salt.size}."
        }
        val secretBytes = secret.toUtf8Bytes()
        val key = try {
            SCrypt.generate(
                secretBytes,
                salt,
                SCRYPT_N,
                SCRYPT_R,
                SCRYPT_P,
                length
            )
        } finally {
            secretBytes.wipe()
        }
        return key
    }

    /** Salt aleatorio de [SALT_BYTES] bytes para una nueva ofuscación en v2. */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }
}
