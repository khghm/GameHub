package com.gamehub.host.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.graphics.Brush
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
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val isLoading by viewModel.isLoading.collectAsState()
    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword

    LaunchedEffect(viewModel.isLoggedIn.value) {
        if (viewModel.isLoggedIn.value) {
            onRegisterSuccess()
        }
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
                "Create Account",
                style = MaterialTheme.typography.displayLarge,
                color = OnBackground,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Join the fun today!",
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

            // Display Name TextField
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name", color = OnSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Star,
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

            Spacer(modifier = Modifier.height(18.dp))

            // Confirm Password TextField
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password", color = OnSurfaceVariant) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = OnSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading,
                isError = !passwordsMatch,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    focusedBorderColor = if (passwordsMatch) Primary else Error,
                    unfocusedBorderColor = if (passwordsMatch) SurfaceVariant else Error.copy(alpha = 0.5f),
                    cursorColor = if (passwordsMatch) Primary else Error,
                    focusedContainerColor = Surface,
                    unfocusedContainerColor = Surface.copy(alpha = 0.7f)
                )
            )
            AnimatedVisibility(
                visible = !passwordsMatch,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Passwords do not match",
                        color = Error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Register Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (username.isNotEmpty() && password.isNotEmpty() && passwordsMatch) {
                        viewModel.register(username, password, displayName.ifEmpty { username })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                enabled = !isLoading && username.isNotEmpty() && password.isNotEmpty() && passwordsMatch,
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
                            "Creating Account...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        "Register",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Login Link
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    "Already have an account? ",
                    color = OnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Login",
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
