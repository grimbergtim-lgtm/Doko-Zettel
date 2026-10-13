package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AppDatabase
import com.example.data.repository.DoppelkopfRepository
import com.example.data.settings.SettingsRepository
import com.example.ui.navigation.DoppelkopfNavGraph
import com.example.ui.theme.DoppelkopfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = DoppelkopfRepository(database)
        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            DoppelkopfTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    DoppelkopfNavGraph(
                        navController = navController,
                        database = database,
                        repository = repository,
                        settingsRepository = settingsRepository
                    )
                }
            }
        }
    }
}
