package com.ivanopulo.planner.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanopulo.planner.R
import com.ivanopulo.planner.data.SyncError
import com.ivanopulo.planner.sync.SyncStatus
import com.ivanopulo.planner.ui.components.ruledLine
import com.ivanopulo.planner.ui.week.currentLocale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SyncSection(viewModel: SyncViewModel) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val signingIn by viewModel.signingIn.collectAsStateWithLifecycle()
    val signInError by viewModel.signInError.collectAsStateWithLifecycle()

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> viewModel.onConsentResult(result.resultCode, result.data) }
    LaunchedEffect(Unit) {
        viewModel.consentRequests.collect { pendingIntent ->
            consentLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        }
    }

    // Until the first value arrives the section stays empty instead of flashing the signed-out state.
    val current = status ?: return
    if (current.account == null) {
        SignedOut(signingIn, signInError, onSignIn = viewModel::signIn)
    } else {
        SignedIn(current, onSyncNow = viewModel::syncNow, onSignOut = viewModel::signOut)
    }
}

@Composable
private fun SignedOut(signingIn: Boolean, error: SignInError?, onSignIn: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ActionRow(
        text = stringResource(if (signingIn) R.string.sync_signing_in else R.string.sync_sign_in),
        color = colors.primary,
        enabled = !signingIn,
        onClick = onSignIn,
    )
    Text(
        text = stringResource(R.string.sync_sign_in_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    if (error != null) {
        Text(
            text = stringResource(
                when (error) {
                    SignInError.CONFIGURATION -> R.string.sign_in_error_configuration
                    SignInError.FAILED -> R.string.sign_in_error_failed
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.error,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SignedIn(status: SyncStatus, onSyncNow: () -> Unit, onSignOut: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    ValueRow(label = stringResource(R.string.sync_account), value = status.account.orEmpty())
    Column(Modifier.ruledLine().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.sync_last),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    status.running -> stringResource(R.string.sync_running)
                    status.lastSyncAt != null -> formatTimestamp(status.lastSyncAt)
                    else -> stringResource(R.string.sync_never)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
        if (status.lastError != null && !status.running) {
            Text(
                text = stringResource(status.lastError.message()),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
    ActionRow(
        text = stringResource(R.string.sync_now),
        color = colors.primary,
        enabled = !status.running,
        onClick = onSyncNow,
    )
    ActionRow(text = stringResource(R.string.sync_sign_out), color = colors.error, onClick = onSignOut)
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(Modifier.ruledLine(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
        )
    }
}

@Composable
private fun ActionRow(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier.ruledLine().clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun formatTimestamp(millis: Long): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(currentLocale())
    return formatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

private fun SyncError.message() = when (this) {
    SyncError.AUTH -> R.string.sync_error_auth
    SyncError.NETWORK -> R.string.sync_error_network
    SyncError.FORMAT -> R.string.sync_error_format
    SyncError.OTHER -> R.string.sync_error_other
}
