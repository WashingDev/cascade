package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.AppNavigation
import com.example.ui.theme.CadenceTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (FirebaseApp.getApps(this).isEmpty()) {
      FirebaseApp.initializeApp(this)
    }
    enableEdgeToEdge()
    setContent {
      CadenceTheme {
        AppNavigation()
      }
    }
  }
}
