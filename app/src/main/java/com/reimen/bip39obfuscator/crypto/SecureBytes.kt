package com.reimen.bip39obfuscator.crypto

import java.nio.CharBuffer

/**
 * Utilidades para el manejo de datos sensibles en memoria.
 *
 * Kotlin/Java no garantizan borrado seguro (a diferencia de C), pero
 * sobrescribir los buffers justo después de usarlos reduce la ventana de
 * exposición en dumps de memoria y ante GC.
 */

/** Sobrescribe el contenido del array con ceros. */
fun ByteArray.wipe() {
    fill(0)
}

/** Sobrescribe el contenido del array con caracteres nulos. */
fun CharArray.wipe() {
    fill('\u0000')
}

/** Convierte un CharArray a bytes UTF-8 sin crear un String persistente. */
fun CharArray.toUtf8Bytes(): ByteArray {
    val byteBuffer = Charsets.UTF_8.encode(CharBuffer.wrap(this))
    val bytes = ByteArray(byteBuffer.remaining())
    byteBuffer.get(bytes)
    return bytes
}

/** Representación hexadecimal (minúsculas). */
fun ByteArray.toHex(): String =
    joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

/** Convierte una cadena hexadecimal (longitud par) a bytes. Lanza si es inválida. */
fun String.hexToBytes(): ByteArray {
    require(length % 2 == 0) { "Longitud hexadecimal impar: $length" }
    val bytes = ByteArray(length / 2)
    for (i in bytes.indices) {
        val hi = Character.digit(this[i * 2], 16)
        val lo = Character.digit(this[i * 2 + 1], 16)
        require(hi != -1 && lo != -1) { "Carácter hex inválido en posición ${i * 2}" }
        bytes[i] = ((hi shl 4) or lo).toByte()
    }
    return bytes
}
