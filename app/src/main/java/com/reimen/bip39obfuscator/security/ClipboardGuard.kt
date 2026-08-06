package com.reimen.bip39obfuscator.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Copiado al portapapeles con marcas de contenido sensible y limpieza
 * automática.
 *
 * El portapapeles es la fuente #1 de fugas de seeds (otras apps con permiso
 * de lectura pueden leerlo), por lo que esta app NO ofrece copiar seeds por
 * defecto. Esta utilidad queda disponible para uso futuro.
 */
object ClipboardGuard {

    /** Copia [value] marcándolo como sensible (API 33+) y lo limpia tras [autoClearMs]. */
    fun copySensitive(
        context: Context,
        label: String,
        value: String,
        autoClearMs: Long = 30_000L,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ): Job {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)

        return scope.launch {
            delay(autoClearMs)
            val current = clipboard.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
            // Solo limpiamos si el portapapeles sigue conteniendo NUESTRA seed:
            // si otra app o el usuario copió otra cosa, no la destruimos.
            if (current == value) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
}
