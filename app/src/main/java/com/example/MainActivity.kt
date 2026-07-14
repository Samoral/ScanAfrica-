package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ScanNaijaRepository
import com.example.ui.ScanNaijaApp
import com.example.ui.ScanNaijaViewModel
import com.example.ui.ScanNaijaViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local SQLite DB and Repository setup
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScanNaijaRepository(
            manufacturerDao = database.manufacturerDao(),
            productDao = database.productDao(),
            scanHistoryDao = database.scanHistoryDao(),
            salesListDao = database.salesListDao(),
            piracyFlagDao = database.piracyFlagDao()
        )

        // Instantiate unified ViewModel
        val viewModel = ViewModelProvider(
            this,
            ScanNaijaViewModelFactory(application, repository)
        )[ScanNaijaViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ScanNaijaApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Box(modifier: Modifier = Modifier, content: @androidx.compose.runtime.Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        content()
    }
}

