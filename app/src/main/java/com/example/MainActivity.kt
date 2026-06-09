package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.data.SpaceTimeDatabase
import com.example.data.SpaceTimeRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RoomScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SpaceTimeViewModel
import com.example.viewmodel.SpaceTimeViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var database: SpaceTimeDatabase
    private lateinit var repository: SpaceTimeRepository
    private lateinit var viewModel: SpaceTimeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = Room.databaseBuilder(
            applicationContext,
            SpaceTimeDatabase::class.java,
            "spacetime_db"
        )
        .fallbackToDestructiveMigration()
        .build()
        repository = SpaceTimeRepository(database.spaceTimeDao())
        viewModel = ViewModelProvider(
            this,
            SpaceTimeViewModelFactory(repository)
        )[SpaceTimeViewModel::class.java]

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToRoom = { roomId ->
                                    navController.navigate("room/$roomId")
                                }
                            )
                        }
                        composable("room/{roomId}") { backStackEntry ->
                            val roomIdStr = backStackEntry.arguments?.getString("roomId") ?: "0"
                            RoomScreen(
                                roomId = roomIdStr.toInt(),
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
