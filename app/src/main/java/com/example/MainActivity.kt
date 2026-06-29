package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.TrooperDatabase
import com.example.data.TrooperRepository
import com.example.ui.TrooperApp
import com.example.ui.TrooperViewModel
import com.example.ui.TrooperViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Database, Repository and ViewModel
    val database = TrooperDatabase.getDatabase(this)
    val dao = database.trooperDao()
    val repository = TrooperRepository(dao)
    val factory = TrooperViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, factory)[TrooperViewModel::class.java]

    setContent {
      MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
        TrooperApp(viewModel = viewModel)
      }
    }
  }
}
