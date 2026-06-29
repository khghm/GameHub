package com.gamehub.host.ui.navigation

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gamehub.host.ui.screens.*
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.AuthViewModel
import com.gamehub.host.viewmodel.ChatViewModel
import com.gamehub.host.viewmodel.GameViewModel
import com.gamehub.host.viewmodel.PartyViewModel
import com.gamehub.host.viewmodel.TournamentViewModel

data class BottomNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val activity = LocalContext.current as Activity
    val authViewModel: AuthViewModel = viewModel(viewModelStoreOwner = activity as ViewModelStoreOwner)
    val currentUser by authViewModel.currentUser.collectAsState()
    val token by authViewModel.token.collectAsState()

    LaunchedEffect(token) {
        com.gamehub.host.network.GlobalAuth.token = token
    }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("lobby") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                viewModel = authViewModel
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("lobby") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        composable("lobby") {
            MainScreen(
                navController = navController,
                startTab = "lobby",
                authViewModel = authViewModel,
                activity = activity
            )
        }

        composable("friends") {
            MainScreen(
                navController = navController,
                startTab = "friends",
                authViewModel = authViewModel,
                activity = activity
            )
        }

        composable("party") {
            MainScreen(
                navController = navController,
                startTab = "party",
                authViewModel = authViewModel,
                activity = activity
            )
        }

        composable("chat") {
            MainScreen(
                navController = navController,
                startTab = "chat",
                authViewModel = authViewModel,
                activity = activity
            )
        }

        composable("profile") {
            MainScreen(
                navController = navController,
                startTab = "profile",
                authViewModel = authViewModel,
                activity = activity
            )
        }

        composable(
            route = "game/{gameId}/{playerId}?sessionId={sessionId}&instanceKey={instanceKey}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.StringType },
                navArgument("playerId") { type = NavType.StringType; defaultValue = "player1" },
                navArgument("sessionId") { type = NavType.StringType; defaultValue = "" },
                navArgument("instanceKey") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            val playerId = backStackEntry.arguments?.getString("playerId") ?: "player1"
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val instanceKey = backStackEntry.arguments?.getString("instanceKey") ?: ""

            val gameViewModel: GameViewModel = viewModel(key = instanceKey)

            GameScreen(
                gameId = gameId,
                playerId = playerId,
                sessionId = sessionId,
                viewModel = gameViewModel,
                onPlayAgain = {
                    navController.navigate("lobby") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("matchhistory/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            MatchHistoryScreen(
                username = username,
                onBack = { navController.popBackStack() },
                onViewReplay = { gameSessionId ->
                    navController.navigate("replay/$gameSessionId")
                }
            )
        }

        composable(
            route = "replay/{gameSessionId}",
            arguments = listOf(
                navArgument("gameSessionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameSessionId = backStackEntry.arguments?.getString("gameSessionId") ?: ""
            ReplayScreen(
                gameSessionId = gameSessionId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("leaderboard") {
            val currentUsername = currentUser?.username ?: ""
            LeaderboardScreen(
                currentUsername = currentUsername,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "queue/{gameType}/{playerName}",
            arguments = listOf(
                navArgument("gameType") { type = NavType.StringType },
                navArgument("playerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val gameType = backStackEntry.arguments?.getString("gameType") ?: "tictactoe"
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "player1"
            QueueScreen(
                gameType = gameType,
                playerName = playerName,
                onMatchFound = { sessionId, opponent, instanceKey ->
                    navController.navigate("game/$gameType/$playerName?sessionId=$sessionId&instanceKey=$instanceKey") {
                        popUpTo("lobby") { inclusive = false }
                    }
                },
                onCancel = {
                    navController.navigate("lobby") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("tournaments") {
            val tournamentViewModel: TournamentViewModel = viewModel()
            val currentUsername = currentUser?.username ?: "player1"
            LaunchedEffect(Unit) {
                tournamentViewModel.connect()
                tournamentViewModel.listTournaments()
            }
            TournamentListScreen(
                viewModel = tournamentViewModel,
                onBack = { navController.popBackStack() },
                onTournamentClick = { tournamentId ->
                    navController.navigate("tournamentLobby/$tournamentId")
                },
                currentUsername = currentUsername
            )
        }

        composable(
            route = "tournamentLobby/{tournamentId}",
            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: return@composable
            val playerName = currentUser?.username ?: "player1"
            val tournamentViewModel: TournamentViewModel = viewModel(viewModelStoreOwner = activity as ViewModelStoreOwner)

            LaunchedEffect(tournamentId) {
                tournamentViewModel.playerName = playerName
                tournamentViewModel.connect()
                tournamentViewModel.subscribe(tournamentId)
            }

            TournamentLobbyScreen(
                tournamentId = tournamentId,
                viewModel = tournamentViewModel,
                onBack = { navController.popBackStack() },
                onStartBracket = { tourneyId -> navController.navigate("tournamentBracket/$tourneyId") }
            )
        }

        composable(
            route = "tournamentBracket/{tournamentId}",
            arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: return@composable
            val tournamentViewModel: TournamentViewModel = viewModel(viewModelStoreOwner = activity as ViewModelStoreOwner)

            LaunchedEffect(tournamentId) {
                tournamentViewModel.subscribe(tournamentId)
            }

            TournamentBracketScreen(
                tournamentId = tournamentId,
                viewModel = tournamentViewModel,
                onBack = { navController.popBackStack() },
                onPlayMatch = { matchId, gameType, playerName ->
                    navController.navigate("game/$gameType/$playerName?sessionId=$matchId")
                }
            )
        }

        composable("clan") {
            val authToken = com.gamehub.host.network.GlobalAuth.token ?: ""
            ClanScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { channelType, channelId ->
                    navController.navigate("chatGroup/$channelType/$channelId")
                }
            )
        }

        composable("society") {
            val authToken = com.gamehub.host.network.GlobalAuth.token ?: ""
            SocietyScreen(
                onBack = { navController.popBackStack() },
                onOpenChat = { channelType, channelId ->
                    navController.navigate("chatGroup/$channelType/$channelId")
                }
            )
        }

        composable("chatGroup/{channelType}/{channelId}") { backStackEntry ->
            val channelType = backStackEntry.arguments?.getString("channelType") ?: "clan"
            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
        }


    }
}

@Composable
fun MainScreen(
    navController: NavHostController,
    startTab: String,
    authViewModel: AuthViewModel,
    activity: Activity
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUsername = currentUser?.username ?: "player1"

    val bottomNavItems = listOf(
        BottomNavItem("لابی", Icons.Default.Home, "lobby"),
        BottomNavItem("دوستان", Icons.Default.Face, "friends"),
        BottomNavItem("پارتی", Icons.Default.Star, "party"),
        BottomNavItem("چت", Icons.Default.Email, "chat"),
        BottomNavItem("پروفایل", Icons.Default.Person, "profile"),
    )

    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route ?: startTab

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo("lobby") { inclusive = false }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) Primary else OnSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                color = if (isSelected) Primary else OnSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (currentRoute) {
            "lobby" -> {
                LaunchedEffect(Unit) {
                    authViewModel.loadProfile()
                }
                LobbyScreen(
                    modifier = contentModifier,
                    onStartGame = { gameId ->
                        navController.navigate("game/$gameId/$currentUsername")
                    },
                    onProfileClick = { navController.navigate("profile") },
                    onFriendsClick = { navController.navigate("friends") },
                    onChatClick = { navController.navigate("chat") },
                    onPlayNowClick = { gameType ->
                        navController.navigate("queue/$gameType/$currentUsername")
                    },
                    currentUsername = currentUsername,
                    onTournamentsClick = { navController.navigate("tournaments") },
                    onLeaderboardClick = { navController.navigate("leaderboard") },
                    onPartyCreateClick = { navController.navigate("party") },
                    onClanClick = { navController.navigate("clan") },
                    onSocietyClick = { navController.navigate("society") },
                )
            }
            "friends" -> FriendsScreen(viewModel = authViewModel, modifier = contentModifier)
            "party" -> {
                val partyViewModel: PartyViewModel = viewModel()
                val token = com.gamehub.host.network.GlobalAuth.token ?: ""
                val currentUsernameP = authViewModel.currentUser.collectAsState().value?.username ?: "player1"

                LaunchedEffect(token) {
                    if (token.isNotEmpty()) {
                        partyViewModel.authToken = token
                        partyViewModel.currentUserId = currentUsernameP
                        partyViewModel.connectHub()
                    }
                }

                PartyScreen(
                    viewModel = partyViewModel,
                    onBack = { navController.popBackStack() },
                    onStartGame = { /* TODO */ },
                    modifier = contentModifier
                )
            }
            "chat" -> {
                val playerName = currentUser?.username ?: "player1"
                val chatViewModel: ChatViewModel = viewModel()
                val serverIp = com.gamehub.host.BuildConfig.SERVER_IP
                LaunchedEffect(playerName) {
                    chatViewModel.connect("ws://$serverIp:8080/chat", playerName)
                }

                ChatScreen(
                    localPlayerName = playerName,
                    messages = chatViewModel.messages.collectAsState().value,
                    onlineUsers = chatViewModel.onlineUsers.collectAsState().value,
                    onSendMessage = { chatViewModel.sendMessage(it) },
                    onClose = { navController.popBackStack() },
                    modifier = contentModifier
                )
            }
            "profile" -> ProfileScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = authViewModel,
                modifier = contentModifier,
                onViewMatchHistory = {
                    val username = authViewModel.currentUser.value?.username ?: ""
                    navController.navigate("matchhistory/$username")
                }
            )
        }
    }
}
