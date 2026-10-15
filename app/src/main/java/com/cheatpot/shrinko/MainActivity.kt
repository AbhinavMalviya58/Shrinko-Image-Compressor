package com.cheatpot.shrinko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cheatpot.shrinko.data.repository.ImageRepositoryImpl
import com.cheatpot.shrinko.navigation.AppNavigation

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = ImageRepositoryImpl(applicationContext)

        setContent {
            AppNavigation(imageRepository = repository)
        }
    }
}
