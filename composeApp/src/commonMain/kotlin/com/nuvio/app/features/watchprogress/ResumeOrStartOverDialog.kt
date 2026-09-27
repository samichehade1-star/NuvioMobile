package com.nuvio.app.features.watchprogress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.resume_or_start_over_body_percent
import nuvio.composeapp.generated.resources.resume_or_start_over_body_time
import nuvio.composeapp.generated.resources.resume_or_start_over_resume
import nuvio.composeapp.generated.resources.resume_or_start_over_start_over
import nuvio.composeapp.generated.resources.resume_or_start_over_title
import org.jetbrains.compose.resources.stringResource

/** Minimum resume position before it's worth interrupting playback start with a choice. */
internal const val ResumeOrStartOverMinPositionMs = 15_000L

private fun Long.formatAsClock(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ResumeOrStartOverDialog(
    resumePositionMs: Long,
    resumeProgressFraction: Float?,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    onDismiss: () -> Unit,
) {
    val body = if (resumeProgressFraction != null && resumeProgressFraction > 0f) {
        stringResource(Res.string.resume_or_start_over_body_percent, (resumeProgressFraction * 100f).roundToInt())
    } else {
        stringResource(Res.string.resume_or_start_over_body_time, resumePositionMs.formatAsClock())
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(Res.string.resume_or_start_over_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onStartOver,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        Text(stringResource(Res.string.resume_or_start_over_start_over))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = onResume,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(Res.string.resume_or_start_over_resume))
                    }
                }
            }
        }
    }
}
