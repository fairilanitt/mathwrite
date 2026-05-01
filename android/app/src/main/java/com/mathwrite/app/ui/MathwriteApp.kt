package com.mathwrite.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mathwrite.app.bridge.CompanionBridgeClient
import com.mathwrite.app.format.LatexPasteMode
import com.mathwrite.app.ink.InkStroke
import com.mathwrite.app.ink.StrokeCanvas
import com.mathwrite.app.mathpix.MathpixClient
import com.mathwrite.app.mathpix.MathpixCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun MathwriteApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF8FAFC)) {
            MathwriteScreen()
        }
    }
}

@Composable
private fun MathwriteScreen() {
    var strokes by remember { mutableStateOf<List<InkStroke>>(emptyList()) }
    var preview by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Connect the tablet over USB, start the Windows companion, then write and send.") }
    var pasteMode by remember { mutableStateOf(MathwriteUiDefaults.DefaultPasteMode) }
    var sequenceId by remember { mutableLongStateOf(1L) }
    val bridgeSessionId = remember { UUID.randomUUID().toString() }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sendCurrentCanvas: () -> Unit = {
        if (strokes.isEmpty()) {
            status = "Write something before sending."
        } else {
            isSending = true
            status = "Recognizing with Mathpix v3/strokes..."

            scope.launch {
                val recognition = withContext(Dispatchers.IO) {
                    MathpixClient(MathpixCredentials.AppId, MathpixCredentials.AppKey).recognize(strokes)
                }

                val latex = recognition.latex
                if (latex.isNullOrBlank()) {
                    status = recognition.error ?: "No LaTeX returned."
                    isSending = false
                    return@launch
                }

                preview = latex
                status = "Sending LaTeX to Windows companion..."

                val currentSequence = sequenceId++
                val bridgeResult = withContext(Dispatchers.IO) {
                    CompanionBridgeClient().paste(currentSequence, latex, pasteMode, bridgeSessionId)
                }

                status = if (bridgeResult.ok) {
                    "Pasted to Windows."
                } else {
                    bridgeResult.message ?: "Windows companion paste failed."
                }
                isSending = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DrawingBoardToolbar(
            canUndo = strokes.isNotEmpty() && !isSending,
            canClear = strokes.isNotEmpty() && !isSending,
            canSend = !isSending,
            isSending = isSending,
            pasteMode = pasteMode,
            onPasteModeChange = { pasteMode = it },
            onUndo = { strokes = strokes.dropLast(1) },
            onClear = {
                strokes = emptyList()
                preview = ""
                status = "Canvas cleared."
            },
            onSend = sendCurrentCanvas,
        )

        StrokeCanvas(
            strokes = strokes,
            onStrokeFinished = { stroke -> strokes = strokes + stroke },
            onStylusButtonPressed = sendCurrentCanvas,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                .padding(8.dp),
        )

        Text("Preview: ${preview.ifBlank { "(nothing recognized yet)" }}")
        Text(status, color = Color(0xFF334155))

        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun DrawingBoardToolbar(
    canUndo: Boolean,
    canClear: Boolean,
    canSend: Boolean,
    isSending: Boolean,
    pasteMode: LatexPasteMode,
    onPasteModeChange: (LatexPasteMode) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        FlowRow(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Pen") },
            )
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("S Pen only") },
            )
            OutlinedButton(
                enabled = canUndo,
                onClick = onUndo,
            ) {
                Text("Undo")
            }
            OutlinedButton(
                enabled = canClear,
                onClick = onClear,
            ) {
                Text("Clear")
            }
            Button(
                enabled = canSend,
                onClick = onSend,
            ) {
                Text(if (isSending) "Sending" else "Send")
            }
            PasteModeSettingsMenu(
                pasteMode = pasteMode,
                onPasteModeChange = onPasteModeChange,
            )
        }
    }
}

@Composable
private fun PasteModeSettingsMenu(
    pasteMode: LatexPasteMode,
    onPasteModeChange: (LatexPasteMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text("Settings: ${pasteMode.label}")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LatexPasteMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text("Paste mode: ${mode.label}") },
                    onClick = {
                        onPasteModeChange(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}
