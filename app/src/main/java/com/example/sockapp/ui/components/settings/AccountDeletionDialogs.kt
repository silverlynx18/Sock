package com.example.sockapp.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sockapp.ui.theme.SockAppTheme

@Composable
fun AccountDeletionConfirmationDialogStep1(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account?") },
        text = { Text("This is a permanent action and cannot be undone. All your data associated with this account will be removed, including your profile, groups you own (if not transferred), and other contributions.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Continue Deletion")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AccountDeletionConfirmationDialogStep2(
    isLoading: Boolean,
    onConfirmDeletion: (confirmationText: String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null, // To display errors from ViewModel (e.g., password incorrect if that was a step)
    confirmationChallengeText: String = "DELETE" // The text user must type
) {
    var typedConfirmation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Final Confirmation Required") },
        text = {
            Column {
                Text("To confirm permanent deletion of your account, please type '${confirmationChallengeText}' in the box below.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = typedConfirmation,
                    onValueChange = { typedConfirmation = it },
                    label = { Text("Type '${confirmationChallengeText}'") },
                    singleLine = true,
                    isError = typedConfirmation.isNotEmpty() && typedConfirmation != confirmationChallengeText
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
                }
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDeletion(typedConfirmation) },
                enabled = !isLoading && typedConfirmation == confirmationChallengeText,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete My Account Permanently")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
fun AccountDeletionConfirmationDialogStep1Preview() {
    SockAppTheme {
        AccountDeletionConfirmationDialogStep1(onConfirm = {}, onDismiss = {})
    }
}

@Preview
@Composable
fun AccountDeletionConfirmationDialogStep2Preview_Initial() {
    SockAppTheme {
        AccountDeletionConfirmationDialogStep2(
            isLoading = false,
            onConfirmDeletion = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun AccountDeletionConfirmationDialogStep2Preview_Typing() {
    SockAppTheme {
        // In a real preview, you'd need to manage the state of typedConfirmation
        AccountDeletionConfirmationDialogStep2(
            isLoading = false,
            onConfirmDeletion = {},
            onDismiss = {}
            // Simulate typing by pre-filling or using @PreviewParameter
        )
    }
}

@Preview
@Composable
fun AccountDeletionConfirmationDialogStep2Preview_Loading() {
    SockAppTheme {
        AccountDeletionConfirmationDialogStep2(
            isLoading = true,
            onConfirmDeletion = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun AccountDeletionConfirmationDialogStep2Preview_Error() {
    SockAppTheme {
        AccountDeletionConfirmationDialogStep2(
            isLoading = false,
            onConfirmDeletion = {},
            onDismiss = {},
            errorMessage = "Account deletion failed. Please try again."
        )
    }
}
