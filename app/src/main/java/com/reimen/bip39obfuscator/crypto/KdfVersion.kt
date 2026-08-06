package com.reimen.bip39obfuscator.crypto

/**
 * Versiones del KDF para transformar seeds.
 *
 * v1: SHA-256 encadenado con contador (paridad byte a byte con el script Python
 * original). Sin salt. No cambiar su lógica: las seeds ya ofuscadas con v1 solo
 * se revierten con v1.
 *
 * v2: scrypt (N=32768, r=8, p=1) con salt de 16 bytes obligatorio. El salt es
 * indispensable para revertir: sin él, la operación no es reversible.
 */
enum class KdfVersion(val id: Int, val label: String) {
    V1_SHA256(1, "v1 · SHA-256 (legado)"),
    V2_SCRYPT(2, "v2 · scrypt (recomendado)");

    companion object {
        fun fromId(id: Int): KdfVersion =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Versión KDF desconocida: $id")
    }
}
