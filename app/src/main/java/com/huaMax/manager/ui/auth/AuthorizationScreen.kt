package com.huaMax.manager.ui.auth

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.huaMax.R
import com.huaMax.data.auth.AuthorizationManager
import com.huaMax.data.repository.PreferencesRepository
import com.huaMax.manager.ui.navigation.Screen

@Composable
fun AuthorizationScreen(
    navController: NavController,
    preferencesRepository: PreferencesRepository
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.auth_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.auth_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    message = null
                    isError = false
                },
                label = { Text(stringResource(R.string.auth_code_label)) },
                minLines = 3,
                maxLines = 5,
                isError = isError,
                modifier = Modifier.fillMaxWidth()
            )

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    when (val result = preferencesRepository.saveAuthorizationCode(code)) {
                        is AuthorizationManager.ValidationResult.Valid -> {
                            preferencesRepository.syncAuthorizationToRemote()
                            val expiresText = DateFormat.format(
                                "yyyy-MM-dd HH:mm",
                                result.expiresAtMillis
                            ).toString()
                            message = context.getString(R.string.auth_success, expiresText)
                            isError = false
                            navController.navigate(Screen.Permissions.route) {
                                popUpTo(Screen.Authorization.route) { inclusive = true }
                            }
                        }
                        is AuthorizationManager.ValidationResult.Invalid -> {
                            isError = true
                            message = AuthorizationManager.reasonMessage(result.reason)
                        }
                    }
                },
                enabled = code.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.auth_activate))
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { uriHandler.openUri("https://t.me/+w4ftZ0ZAmrRhOTZl") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.auth_join_group_for_code))
            }
        }
    }
}
