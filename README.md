# BIP-39 Obfuscator (Android)

App Android que **ofusca y revierte** frases semilla BIP-39 con una clave
secreta. Kotlin + Jetpack Compose (Material 3), MVVM con `StateFlow`,
`minSdk 26` / `targetSdk 35`, sin dependencias de red.

La transformación es un **XOR de la entropía con una clave derivada del
secreto**, seguido de recalcular el checksum BIP-39. El XOR es involutivo:
ofuscar y revertir son la misma operación, siempre que coincidan secreto,
versión de KDF y salt.

- **Sin permiso `INTERNET`** declarado en el manifiesto: ninguna librería ni
  componente puede sacar datos del dispositivo. Es la mitigación más fuerte
  contra la exfiltración.
- `FLAG_SECURE`: sin capturas, sin grabación de pantalla, sin mirroring y sin
  aparecer en la lista de apps recientes.
- Portapapeles marcado como sensible (`IS_SENSITIVE`, API 33+) y auto-borrado a
  los 30 s.
- Buffers de clave/entropía sobrescritos con ceros tras cada uso (mejor
  esfuerzo: la JVM no garantiza borrado seguro).
- Wordlist empaquetada en assets y verificada por SHA-256 contra una constante
  hardcodeada, además de exigir exactamente 2048 entradas.
- Backup en la nube y traspaso entre dispositivos excluidos por completo
  (`data_extraction_rules.xml`).

## Requisitos

- JDK 17 (Android Gradle Plugin requiere 17; el bytecode targets Java 11).
- Android SDK con `compileSdk 35` y build-tools.
- Gradle Wrapper 8.11.1 incluido: usar `./gradlew`, no un Gradle del sistema.
- BouncyCastle `bcprov-jdk18on:1.78.1` (única dependencia no-Android, para scrypt).

## Build y tests

```sh
./gradlew test                 # 29 tests unitarios (JVM, sin emulador)
./gradlew assembleDebug        # APK de debug
./gradlew assembleRelease      # APK release (R8 + shrink, requiere keystore)
./gradlew installDebug         # instalar en un dispositivo conectado
```

La configuración release lee `keystore.properties` en la raíz del proyecto (no
versionado). Si el fichero no existe, Gradle compila el release sin firmar:

```properties
storeFile=/ruta/al/keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

### Tests

| Suite | Qué cubre |
|---|---|
| `KeyDerivationTest` | Paridad byte a byte con los vectores de Python para v1 y v2, determinismo, truncado, salt obligatorio de 16 bytes |
| `SeedTransformerTest` | Ida y vuelta (round-trip) en v1 y v2, y vectores de `transform_seed` del script original |
| `SeedValidatorTest` | Seeds de 12 y 24 palabras, checksum incorrecto, palabras desconocidas, número de palabras inválido |
| `SeedFileParserTest` | Formato horizontal, vertical, líneas en blanco como separador, archivo vacío |
| `WordlistProviderTest` | Digest correcto, asset manipulado rechazado, recuento de palabras |

## Uso

1. Pega la seedphrase (12, 15, 18, 21 o 24 palabras).
2. Escribe la clave secreta y su confirmación.
3. Elige el KDF:
   - **v1 · SHA-256 (legado)** — `SHA-256(secreto || contador_BE_4B)` concatenado
     hasta cubrir la entropía, sin salt. Paridad exacta con el script Python
     original. Las seeds ofuscadas con v1 **solo** se revierten con v1.
   - **v2 · scrypt (recomendado)** — `scrypt(secreto, salt, N=32768, r=8, p=1)`.
     El salt de 16 bytes es **obligatorio e indispensable** para revertir: si
     dejas el campo vacío se genera uno nuevo y la app te lo muestra para que lo
     guardes junto a la seed.
4. Pulsa **Transformar** y copia el resultado (o el salt) si lo necesitas.

> Los parámetros N/r/p de v2 están fijos a propósito. Cambiarlos rompería la
> reversibilidad de las seeds v2 ya ofuscadas: si hay que cambiarlos, crear una
> v3.

## Estructura

```
app/src/main/java/com/reimen/bip39obfuscator/
  MainActivity.kt            # Activity; FLAG_SECURE antes de setContent
  crypto/
    Bip39Params.kt           # tabla de bits por número de palabras
    WordlistProvider.kt      # carga de assets + verificación SHA-256
    SeedValidator.kt         # validación completa (palabras, checksum)
    SeedTransformer.kt       # XOR de entropía + recomputación de checksum
    KeyDerivation.kt         # v1 SHA-256 contador / v2 scrypt
    SecureBytes.kt           # wipe, hex↔bytes, CharArray→UTF-8 sin String
  io/
    SeedFileParser.kt        # horizontal vs. vertical
  security/
    ScreenSecurity.kt        # FLAG_SECURE
    ClipboardGuard.kt        # IS_SENSITIVE + auto-clear
  ui/
    MainScreen.kt            # Composables
    MainViewModel.kt         # estado UiState + orquestación
    SecretField.kt, FieldStyles.kt, theme/Theme.kt
app/src/main/assets/bip39_english.txt   # 2048 palabras
app/src/test/                # tests JVM + vectores de Python
```

## Modelo de seguridad

- **Ofuscación, no cifrado.** Es una capa reversible con clave, no un cifrado
  autenticado: quien tenga la seed ofuscada y el secreto recupera la seed.
  Sirve para que una seed no quede legible a simple vista (un `.txt` compartido,
  una captura de pantalla), no para proteger frente a alguien que tenga acceso
  a ambos. Para eso está la app **Encrypt**.
- **El v2 depende del salt.** Sin el salt de 16 bytes, la transformación con v2
  no es invertible. La app lo muestra explícitamente tras transformar; guárdalo.
- **Sin red.** La ausencia del permiso `INTERNET` es la garantía principal: no
  depende de la disciplina del código.
- **Fuga #1: el portapapeles.** Por eso la copia es siempre marcada como
  sensible, se limpia a los 30 s, y la app no ofrece copiar la seed de forma
  automática.
- **Sin persistencia.** La app no escribe las seeds ni los secretos a disco; los
  buffers se ponen a cero tras usarlos y el backup está excluido.

## Compatibilidad

El esquema es interoperable con
[Bip39-Obfuscator-C++](../../Bip39-Obfuscator-C++) y con el export por lotes de
[Bip39-Generator-C++](../../Bip39-Generator-C++), que portan la misma lógica a
partir del script Python original (los tests comparan contra los vectores
generados por ese script).

## Licencia

Apache-2.0 (ver `LICENSE`).
