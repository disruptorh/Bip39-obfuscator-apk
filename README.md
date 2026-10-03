# BIP-39 Obfuscator (Android)

App Android que **ofusca y revierte** frases semilla BIP-39 con una clave
secreta. Kotlin + Jetpack Compose (Material 3), MVVM con `StateFlow`,
`minSdk 26` / `targetSdk 35`, y **sin permiso `INTERNET`**: ninguna librería ni
componente puede sacar datos del dispositivo.

<p align="center">
  <a href="https://github.com/disruptorh/Bip39-obfuscator-apk/releases/latest/download/bip39-obfuscator-V2.1.apk">
    <img alt="Descargar" src="https://img.shields.io/badge/%E2%AC%87%20Download-latest%20release-2f6feb?style=for-the-badge&logo=github&logoColor=white">
  </a>
  <a href="https://github.com/disruptorh/Bip39-obfuscator-apk/releases/latest">
    <img alt="Versiones" src="https://img.shields.io/github/v/release/disruptorh/Bip39-obfuscator-apk?label=release&style=flat&logo=github&logoColor=white">
  </a>
  <a href="./LICENSE">
    <img alt="Licencia" src="https://img.shields.io/badge/licencia-Apache--2.0-blue?style=flat">
  </a>
</p>

## 📥 Descarga rápida

El botón de arriba descarga el APK **ya firmado** de la última release
(`bip39-obfuscator-V2.1.apk`). Necesitas Android 8.0 (API 26) o superior.

Para instalarlo a mano: abre el APK descargado y, si Android lo pide, activa
**Ajustes → Apps → Acceso especial → Instalar apps desconocidas** para la app
que lo abre (navegador o gestor de archivos).

Si lo descargas desde un ordenador con el móvil por cable o ADB Wi-Fi:

```bash
# 1. Descargar la última release publicada
curl -L -o bip39-obfuscator.apk https://github.com/disruptorh/Bip39-obfuscator-apk/releases/latest/download/bip39-obfuscator-V2.1.apk

# 2. Instalar (o reinstalar) en el dispositivo conectado
adb install -r bip39-obfuscator.apk
```

## 🚀 Uso rápido

La transformación es un **XOR de la entropía con una clave derivada del
secreto**, seguido de recalcular el checksum BIP-39. El XOR es involutivo:
ofuscar y revertir son la misma operación, siempre que coincidan secreto,
versión de KDF y salt.

1. Pega la seedphrase (12, 15, 18, 21 o 24 palabras). También acepta el
   contenido pegado de un fichero entero: la app detecta si es una seed por
   línea o una palabra por línea.
2. Escribe la clave secreta y su confirmación.
3. Elige el KDF:
   - **v1 · SHA-256 (legado)** — `SHA-256(secreto || contador_BE_4B)` concatenado
     hasta cubrir la entropía, sin salt. Paridad exacta con el script Python
     original. Las seeds ofuscadas con v1 **solo** se revierten con v1.
   - **v2 · scrypt (recomendado)** — `scrypt(secreto, salt, N=32768, r=8, p=1)`.
     El salt de 16 bytes (32 caracteres hex) es **obligatorio e indispensable**
     para revertir: si dejas el campo vacío se genera uno nuevo y la app te lo
     muestra para que lo guardes junto a la seed.
4. Pulsa **⚡ Transformar** y copia el resultado (o el salt) si lo necesitas.

> Los parámetros N/r/p de v2 están fijos a propósito. Cambiarlos rompería la
> reversibilidad de las seeds v2 ya ofuscadas: si hay que cambiarlos, crear una
> v3.

## 🧬 Algoritmo y arquitectura

La app es una sola pantalla, sin navegación ni red, con la lógica separada de
Android para poder testearla en la JVM:

| Pieza | Qué hace |
|---|---|
| `crypto/WordlistProvider` | Carga la wordlist de `assets/` y verifica su SHA-256 contra una constante hardcodeada; exige exactamente 2048 entradas |
| `crypto/SeedValidator` | Divide la seed en entropía + checksum, valida palabras, recuento y checksum BIP-39 |
| `crypto/KeyDerivation` | v1: SHA-256 con contador de 4 bytes big-endian. v2: scrypt (N=32768, r=8, p=1) con salt de 16 bytes obligatorio |
| `crypto/SeedTransformer` | XOR byte a byte de la entropía con la clave, recalcula el checksum SHA-256 y vuelve a agrupar en palabras de 11 bits |
| `crypto/SecureBytes` | `wipe()` de buffers, hex↔bytes, `CharArray`→UTF-8 sin pasar por `String` |
| `io/SeedFileParser` | Formato horizontal (una seed por línea) o vertical (una palabra por línea, bloques separados por líneas vacías) |
| `security/ScreenSecurity` | `FLAG_SECURE` |
| `security/ClipboardGuard` | Copia marcada como sensible (API 33+) y auto-borrado a los 30 s |
| `ui/MainViewModel` | `UiState` con `StateFlow` y orquestación; la transformación va a `Dispatchers.Default` |

Dependencias: AndroidX, Compose BOM `2024.06.00` y una única librería no-Android,
`org.bouncycastle:bcprov-jdk18on:1.78.1`, para scrypt.

## 📦 Compilar desde código

La raíz del repositorio **es** el proyecto Gradle: no hay subdirectorio
`android/`. Todos los comandos de esta sección se ejecutan desde ahí.

### Requisitos

- **JDK 17 o superior** (Android Gradle Plugin 8.6 no arranca con menos).
  El bytecode que se genera apunta a Java 11.
- **Android SDK** con la plataforma `android-35` instalada (o Android Studio,
  que la gestiona por ti).
- Nada más: el **Gradle Wrapper 8.11.1** va incluido, así que no hace falta
  instalar Gradle. Usa siempre `./gradlew`, no un Gradle del sistema.

### Clonar

```bash
# 1. Clonar el repositorio
git clone https://github.com/disruptorh/Bip39-obfuscator-apk.git
cd Bip39-obfuscator-apk
```

### Dependencias

Gradle necesita saber dónde está tu SDK de Android. Eso se guarda en
`local.properties`, en la raíz del repo, con una única línea `sdk.dir=`.

> **Aviso:** `local.properties` está en `.gitignore`, pero si copiaste el repo
> desde otra máquina puede venir con la ruta del SDK de esa otra máquina, que en
> tu equipo no existe. Si te aparece un error tipo "SDK location not found",
> sobrescribe el fichero con el bloque de abajo.

```bash
# 2. Crear local.properties apuntando a tu SDK de Android
printf 'sdk.dir=%s\n' "$HOME/Android/Sdk" > local.properties
```

Si tu SDK está en otro sitio, cambia la ruta: en macOS suele ser
`$HOME/Library/Android/sdk` y en Windows
`C:\Users\<tu-usuario>\AppData\Local\Android\Sdk`. Comprueba cuál es con
`ls $HOME/Android/Sdk` o, si usas Android Studio, con **Settings → Languages &
frameworks → Android SDK → SDK location**.

### Compilar

```bash
# 3. Compilar el APK de depilación (va firmado con el keystore de debug que crea el SDK)
./gradlew :app:assembleDebug
```

Sale en `app/build/outputs/apk/debug/app-debug.apk`. Se instala en un
dispositivo conectado, sin configuración adicional:

```bash
# 4. Instalar el APK de depuración en el dispositivo conectado
./gradlew :app:installDebug
```

O a mano, con la ruta exacta del fichero:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

El APK de **release** (minificado con R8 y con los recursos reducidos) necesita
un keystore propio: mira [Firma del APK](#firma-del-apk).

```bash
# 5. Compilar el APK de release (sin firmar si no has configurado un keystore)
./gradlew :app:assembleRelease
```

### Ejecutar los tests

29 tests unitarios JVM, sin emulador. Cubren la paridad byte a byte con los
vectores del script Python original, el round-trip y la validación de entradas:

```bash
./gradlew :app:testDebugUnitTest
```

| Suite | Qué cubre |
|---|---|
| `KeyDerivationTest` | Paridad con los vectores de Python en v1 y v2, determinismo, truncado, salt aleatorio de 16 bytes, scrypt rechaza un salt que no sea de 16 bytes |
| `SeedTransformerTest` | Vectores de `transform_seed` en v1 y v2, round-trip (XOR involutivo) en ambas versiones, y que cambiar secreto o KDF cambia el resultado |
| `SeedValidatorTest` | Seeds válidas de 12 y 24 palabras, checksum incorrecto, palabra desconocida, número de palabras inválido |
| `SeedFileParserTest` | Formato horizontal, vertical, líneas en blanco como separador, agrupación en una sola seed, fichero vacío, salto de línea final |
| `WordlistProviderTest` | Exactamente 2048 palabras, correspondencia biyectiva palabra↔índice, primera y última palabra, digest esperado del asset, asset manipulado y recuento incorrecto rechazados |

Los informes XML quedan en `app/build/test-results/testDebugUnitTest/`.

### Ejecutar la aplicación

Con Android Studio: abre la carpeta del repo y pulsa **Run** sobre la
configuración `app`. Sin Android Studio, `installDebug` (o el `adb install` de
arriba) la deja instalada y lista para lanzar con un toque en el icono.

## 🧰 Comandos útiles

| Tarea | Comando | Qué hace |
|---|---|---|
| Compilar debug | `./gradlew :app:assembleDebug` | APK de depuración en `app/build/outputs/apk/debug/app-debug.apk` |
| Instalar debug | `./gradlew :app:installDebug` | Instala en el dispositivo conectado |
| Compilar release | `./gradlew :app:assembleRelease` | APK de release (R8 + shrink de recursos) |
| Tests | `./gradlew :app:testDebugUnitTest` | Los 29 tests unitarios JVM |
| Lint | `./gradlew :app:lintDebug` | Análisis estático de Android |
| Compilar todo | `./gradlew :app:assemble` | Debug y release de una vez |
| Limpiar | `./gradlew clean` | Borra los ficheros generados |
| Ver tareas | `./gradlew :app:tasks --all` | Lista todas las tareas disponibles |

## 🔐 Seguridad

- **Sin permiso `INTERNET`.** No está declarado en `AndroidManifest.xml`. Es la
  mitigación más fuerte contra la exfiltración: no depende de la disciplina
  del código, la ausencia del permiso es la garantía.
- `FLAG_SECURE`: sin capturas, sin grabación de pantalla, sin mirroring y sin
  aparecer en la lista de apps recientes. Se activa antes de `setContent()`.
- Portapapeles marcado como sensible (`IS_SENSITIVE`, API 33+) y auto-borrado a
  los 30 s. El borrado solo se hace si el portapapeles sigue conteniendo lo que
  copió esta app: no destruye lo que hayas copiado después.
- Buffers de clave/entropía sobrescritos con ceros tras cada uso (mejor
  esfuerzo: la JVM no garantiza borrado seguro).
- Wordlist empaquetada en assets y verificada por SHA-256 contra una constante
  hardcodeada, además de exigir exactamente 2048 entradas. Detecta
  repackaging del APK o corrupción del asset.
- Backup en la nube y traspaso entre dispositivos excluidos por completo
  (`allowBackup="false"` y `data_extraction_rules.xml`).
- Sin persistencia: la app no escribe seeds ni secretos a disco.

**Modelo de amenazas.** Esto es **ofuscación, no cifrado**: es una capa
reversible con clave, no un cifrado autenticado. Quien tenga la seed ofuscada
*y* el secreto recupera la seed. Sirve para que una seed no quede legible a
simple vista (un `.txt` compartido, una captura de pantalla), no para
proteger frente a alguien que tenga acceso a ambos. La fuga #1 es el
portapapeles, y la app nunca copia la seed de forma automática.

El **v2 depende del salt**: sin el salt de 16 bytes la transformación no es
invertible. La app lo muestra explícitamente tras transformar; guárdalo.

El esquema es interoperable con
[Bip39-Obfuscator-C++](https://github.com/disruptorh/Bip39-Obfuscator-C-) y con
el export por lotes de [Bip39-Generator-C++](https://github.com/disruptorh/Bip39-Generator-C-),
que portan la misma lógica a partir del script Python original. Los tests
comparan contra los vectores generados por ese script.

### Firma del APK

`app/build.gradle.kts` lee un fichero `keystore.properties` **en la raíz del
repo** y solo aplica la configuración de firma si ese fichero existe.

**Qué pasa si no tienes keystore propio:** `./gradlew :app:assembleRelease`
termina sin errores, pero produce `app-release-unsigned.apk`. Android no lo
puede instalar: un APK sin firma se rechaza. Para instalar y probar, compila
`assembleDebug`, que va firmado con el keystore de depuración que genera el
propio SDK de Android.

Para firmar el release tienes que aportar tu propio keystore. Estos dos
bloques usan valores **de prueba** (`clave-local-de-pruebas` / `mi-alias`) que
funcionan tal cual al pegar; cámbialos por los tuyos si prefieres.

```bash
# 1. Crear un keystore local de pruebas (el del release publicado no está en el repo)
rm -f release.keystore
keytool -genkeypair -v -keystore release.keystore -alias mi-alias -keyalg RSA -keysize 2048 -validity 10000 -storepass 'clave-local-de-pruebas' -keypass 'clave-local-de-pruebas' -dname "CN=Pruebas locales, C=ES"

# 2. Crear keystore.properties en la raíz del repo con esos mismos valores
cat > keystore.properties <<'EOF'
storeFile=release.keystore
storePassword=clave-local-de-pruebas
keyAlias=mi-alias
keyPassword=clave-local-de-pruebas
EOF

# 3. Compilar el release ya firmado
./gradlew :app:assembleRelease
```

Qué significa cada propiedad:

- `storeFile` — ruta del keystore, **relativa a la raíz del repo**. Con el
  bloque de arriba queda en `release.keystore`, en el mismo directorio.
- `storePassword` — contraseña del almacén del keystore.
- `keyAlias` — alias de la clave dentro del keystore (el que le diste a
  `keytool -alias`).
- `keyPassword` — contraseña de esa clave.

`keystore.properties`, `*.keystore` y `*.jks` están en `.gitignore`: **no los
subas nunca a Git**. Son las cuatro líneas que guardan tu clave de firma.

Con tu keystore, el resultado es `app/build/outputs/apk/release/app-release.apk`
(firmado). Ojo con esto: tu firma es distinta de la del APK publicado en
releases, así que Android no te deja instalarlo encima. Desinstala la versión
anterior primero:

```bash
adb uninstall com.reimen.bip39obfuscator
adb install -r app/build/outputs/apk/release/app-release.apk
```

## 🗂️ Estructura del proyecto

```text
.
├── app/
│   ├── build.gradle.kts              # SDK, plugins, firma release, dependencias
│   ├── proguard-rules.pro            # reglas R8 del release
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml    # sin INTERNET, allowBackup=false
│       │   ├── assets/
│       │   │   └── bip39_english.txt  # las 2048 palabras, verificadas por SHA-256
│       │   ├── java/com/reimen/bip39obfuscator/
│       │   │   ├── MainActivity.kt    # Activity; FLAG_SECURE antes de setContent
│       │   │   ├── crypto/            # Bip39Params, WordlistProvider, SeedValidator,
│       │   │   │                      # SeedTransformer, KeyDerivation, KdfVersion, SecureBytes
│       │   │   ├── io/                # SeedFileParser: horizontal vs. vertical
│       │   │   ├── security/          # ScreenSecurity (FLAG_SECURE), ClipboardGuard
│       │   │   └── ui/                # MainScreen, MainViewModel, SecretField,
│       │   │                          # FieldStyles, theme/Theme
│       │   ├── res/                   # drawable, mipmap, values, xml/data_extraction_rules
│       │   └── test/                  # 29 tests JVM + vectores de Python
│       │       └── resources/bip39_english.txt
├── build.gradle.kts                  # versiones de AGP 8.6.0 y Kotlin 2.0.20
├── settings.gradle.kts               # repositorios google() + mavenCentral(); módulo :app
├── gradle.properties                 # AndroidX, 2 GB de heap para Gradle
├── gradle/wrapper/                   # Gradle Wrapper 8.11.1 (incluido)
├── gradlew                           # siempre ./gradlew
├── local.properties                  # NO se versiona: la ruta de tu SDK (sdk.dir=)
├── keystore.properties               # NO se versiona: credenciales de firma (tú lo creas)
├── LICENSE                           # Apache-2.0
└── README.md
```

## 📄 Licencia

Apache-2.0 — ver [`LICENSE`](./LICENSE).