package com.opencode.thin.ui.screens.connect

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opencode.thin.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    viewModel: ConnectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.opencode_logo),
            contentDescription = "OpenCode",
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(60.dp)
                .padding(bottom = 8.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect to your server",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = viewModel::updateBaseUrl,
            label = { Text("Server URL") },
            placeholder = { Text("http://192.168.1.100:4096") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("Password (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { viewModel.connect(onConnected) }
            ),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            },
        )

        val errorText = state.error
        if (errorText != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.connect(onConnected) },
            enabled = !state.isConnecting && state.baseUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.isConnecting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Connect")
        }
    }
}
