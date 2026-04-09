package com.poppost.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poppost.PopPostApplication
import com.poppost.navigation.NavRoutes
import com.poppost.ui.archived.ArchivedScreen
import com.poppost.ui.create.CreatePostScreen
import com.poppost.ui.main.MainScreen
import com.poppost.viewmodel.PostViewModel
import com.poppost.viewmodel.PostViewModelFactory

/**
 * Ponto de entrada da UI.
 * Cria o NavController e instancia o [PostViewModel] compartilhado entre todas as telas.
 */
@Composable
fun PopPostApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // DI manual: obtém o repositório via Application e cria o ViewModel com a factory
    val app = LocalContext.current.applicationContext as PopPostApplication
    val viewModel: PostViewModel = viewModel(factory = PostViewModelFactory(app.repository))

    NavHost(
        navController = navController,
        startDestination = NavRoutes.MAIN,
        modifier = modifier,
    ) {
        composable(NavRoutes.MAIN) {
            MainScreen(
                viewModel = viewModel,
                onNavigateToCreate = { navController.navigate(NavRoutes.CREATE) },
                onNavigateToArchived = { navController.navigate(NavRoutes.ARCHIVED) },
            )
        }
        composable(NavRoutes.CREATE) {
            CreatePostScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavRoutes.ARCHIVED) {
            ArchivedScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
