package com.reimen.bip39obfuscator

/**
 * Vectores de prueba generados ejecutando el script Python original
 * (bip39_obfuscator.py) con la misma wordlist y el mismo secreto.
 * Los resultados deben coincidir bit a bit.
 */
object TestVectors {

    const val SECRET = "mi-clave-secreta-123"

    /** derive_key(SECRET, length) → hex, del Python. */
    val deriveKeyVectors: Map<Int, String> = mapOf(
        16 to "74ec52f1ce55bd38f1c3f54399fe4376",
        20 to "74ec52f1ce55bd38f1c3f54399fe43762650645a",
        24 to "74ec52f1ce55bd38f1c3f54399fe43762650645a10307aae",
        32 to "74ec52f1ce55bd38f1c3f54399fe43762650645a10307aae9df205b2f245d1e8"
    )

    /**
     * derive_key v2 (scrypt N=32768, r=8, p=1) → hex, del Python:
     *   hashlib.scrypt(SECRET, salt=salt, n=32768, r=8, p=1, dklen=length)
     */
    val scryptSaltHex = "0102030405060708090a0b0c0d0e0f10"
    val scryptKeyVectors: Map<Int, String> = mapOf(
        16 to "65cc911d2254a7a83774b3d4f9e03b20",
        20 to "65cc911d2254a7a83774b3d4f9e03b20da7de132",
        24 to "65cc911d2254a7a83774b3d4f9e03b20da7de132bb47455e",
        32 to "65cc911d2254a7a83774b3d4f9e03b20da7de132bb47455ec50db2a63dbbfa51"
    )

    data class TransformVector(val input: String, val output: String)

    /** transform_seed(input, SECRET) → output, del Python. */
    val transformVectors: List<TransformVector> = listOf(
        TransformVector(
            input = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            output = "insane glad round original forward organ shove leaf drum soup move unit"
        ),
        TransformVector(
            input = "legal winner thank year wave sausage worth useful legal winner thank yellow",
            output = "argue offer glove girl promote cat depend mule diagram cross excuse bargain"
        ),
        TransformVector(
            input = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon address",
            output = "insane glad round original forward organ shove leaf drum soup move unable govern cram hair"
        ),
        TransformVector(
            input = "absurd avoid scissors anxiety gather lottery category door army half long cage bachelor another expect people blade setup",
            output = "invest income latin note desert blur tobacco divert exhibit pole basic small injury desert day pill horror response"
        ),
        TransformVector(
            input = "zoo ivory industry jar praise service talk skirt during october lounge acid year humble cream inspire office dry sunset pride energy",
            output = "mention behave soon typical venture flame box reduce amateur finger behind unit orbit fresh follow infant tobacco dance pink peanut goose"
        ),
        TransformVector(
            input = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon art",
            output = "insane glad round original forward organ shove leaf drum soup move unable govern cram hair alcohol kingdom insane lady arena connect carry element better"
        ),
        TransformVector(
            input = "letter advice cage absurd amount doctor acoustic avoid letter advice cage above",
            output = "virtual grief ordinary oval fatigue switch shallow huge seek slogan rail under"
        ),
        TransformVector(
            input = "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong",
            output = "mention output drum globe picnic glory decline life round copy hundred basket"
        )
    )

    /**
     * transform_seed v2 (scrypt con scryptSaltHex) → output, del Python.
     * Mismo secreto SECRET, salt fijo scryptSaltHex.
     */
    val transformVectorsV2: List<TransformVector> = listOf(
        TransformVector(
            input = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
            output = "grape goose elder dwarf engine staff tape nose stay someone attract domain"
        )
    )
}
