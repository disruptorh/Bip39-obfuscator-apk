package com.reimen.bip39obfuscator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reimen.bip39obfuscator.security.enableScreenSecurity
import com.reimen.bip39obfuscator.ui.MainScreen
import com.reimen.bip39obfuscator.ui.MainViewModel
import com.reimen.bip39obfuscator.ui.theme.SeedObfuscatorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FLAG_SECURE antes de setContent: bloquea screenshots/grabación/recientes.
        enableScreenSecurity()
        enableEdgeToEdge()

        setContent {
            SeedObfuscatorTheme {
                val viewModel: MainViewModel = viewModel()
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
