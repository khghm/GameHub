package com.gamehub.host.ui.screens

import android.annotation.SuppressLint
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamehub.host.ui.theme.*
import com.gamehub.host.viewmodel.AuthViewModel

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel.isLoggedIn.value) {
        if (viewModel.isLoggedIn.value) {
            onLoginSuccess()
        }
    }

    val deviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Section
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Primary, Secondary, Tertiary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🎮",
                    fontSize = 52.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "GameHub",
                style = MaterialTheme.typography.displayLarge,
                color = OnBackground,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Let the games begin!",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(40.dp))

            // Username TextField
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username", color = OnSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = OnSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    cursorColor = Primary,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Password TextField
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = OnSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = OnSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant,
                    cursorColor = Primary,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (username.isNotEmpty() && password.isNotEmpty()) {
                        viewModel.login(username, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                enabled = !isLoading && username.isNotEmpty() && password.isNotEmpty(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (isLoading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = OnPrimary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            "Connecting...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        "Login",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Guest Login Button
            OutlinedButton(
                onClick = { viewModel.guestLogin(deviceId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OnSurface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = SurfaceVariant
                )
            ) {
                Text(
                    "Quick Play (Guest)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Register Link
            TextButton(onClick = onNavigateToRegister) {
                Text(
                    "Don't have an account? ",
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Register",
                    color = Primary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Message Card
            val message by viewModel.message.collectAsState()
            AnimatedVisibility(
                visible = message.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isLoggedIn.value)
                            Success.copy(alpha = 0.12f)
                        else
                            Error.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (viewModel.isLoggedIn.value) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (viewModel.isLoggedIn.value) Success else Error
                        )
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (viewModel.isLoggedIn.value) Success else Error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
