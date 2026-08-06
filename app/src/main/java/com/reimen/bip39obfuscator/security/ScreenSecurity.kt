package com.reimen.bip39obfuscator.security

import android.app.Activity
import android.view.WindowManager

/**
 * FLAG_SECURE bloquea:
 *  - capturas de pantalla y grabación de pantalla,
 *  - screen mirroring (Chromecast, etc.),
 *  - que el contenido aparezca en la lista de apps recientes.
 *
 * Debe activarse antes de setContent().
 */
fun Activity.enableScreenSecurity() {
    window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    )
}
