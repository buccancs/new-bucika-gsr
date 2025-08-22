package com.multisensor.recording.ui.firebase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * Firebase Authentication screen for researcher login and registration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseAuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: FirebaseAuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Handle successful authentication
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // App logo/title
        Text(
            text = "Multi-Sensor GSR Research Platform",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Research Authentication",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (uiState.isRegisterMode) {
            RegisterForm(
                uiState = uiState,
                onRegister = { email, password, displayName, researcherType ->
                    scope.launch {
                        viewModel.register(email, password, displayName, researcherType)
                    }
                },
                onSwitchToLogin = { viewModel.switchToLoginMode() },
                onUpdateDisplayName = viewModel::updateDisplayName,
                onUpdateResearcherType = viewModel::updateResearcherType
            )
        } else {
            LoginForm(
                uiState = uiState,
                onLogin = { email, password ->
                    scope.launch {
                        viewModel.login(email, password)
                    }
                },
                onSwitchToRegister = { viewModel.switchToRegisterMode() },
                onForgotPassword = { email ->
                    scope.launch {
                        viewModel.sendPasswordReset(email)
                    }
                }
            )
        }

        // Error message
        uiState.errorMessage?.let { errorMsg ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Success message
        uiState.successMessage?.let { successMsg ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = successMsg,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    uiState: FirebaseAuthUiState,
    onLogin: (String, String) -> Unit,
    onSwitchToRegister: () -> Unit,
    onForgotPassword: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Researcher Sign In",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            Button(
                onClick = { onLogin(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign In")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { onForgotPassword(email) },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && email.isNotBlank()
                ) {
                    Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
                }

                TextButton(
                    onClick = onSwitchToRegister,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("Create Account", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterForm(
    uiState: FirebaseAuthUiState,
    onRegister: (String, String, String, com.multisensor.recording.firebase.ResearcherType) -> Unit,
    onSwitchToLogin: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateResearcherType: (com.multisensor.recording.firebase.ResearcherType) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val passwordsMatch = password == confirmPassword
    val isFormValid = email.isNotBlank() &&
                     password.length >= 6 &&
                     passwordsMatch &&
                     uiState.displayName.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create Researcher Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = onUpdateDisplayName,
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            // Researcher type dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded && !uiState.isLoading }
            ) {
                OutlinedTextField(
                    value = uiState.selectedResearcherType.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Researcher Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    com.multisensor.recording.firebase.ResearcherType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                onUpdateResearcherType(type)
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                supportingText = {
                    Text("Minimum 6 characters", style = MaterialTheme.typography.bodySmall)
                }
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = confirmPassword.isNotBlank() && !passwordsMatch,
                supportingText = {
                    if (confirmPassword.isNotBlank() && !passwordsMatch) {
                        Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Button(
                onClick = { onRegister(email, password, uiState.displayName, uiState.selectedResearcherType) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && isFormValid
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Account")
                }
            }

            TextButton(
                onClick = onSwitchToLogin,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Text("Already have an account? Sign In", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
