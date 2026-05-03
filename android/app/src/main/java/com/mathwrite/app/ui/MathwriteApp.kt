package com.mathwrite.app.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mathwrite.app.bridge.CompanionBridgeClient
import com.mathwrite.app.bridge.CompanionDevice
import com.mathwrite.app.bridge.CompanionDiscoveryClient
import com.mathwrite.app.bridge.CompanionEndpoint
import com.mathwrite.app.bridge.ConnectionUiState
import com.mathwrite.app.bridge.EndpointPreferences
import com.mathwrite.app.format.LatexPasteMode
import com.mathwrite.app.ink.InkStroke
import com.mathwrite.app.ink.StrokeCanvas
import com.mathwrite.app.mathpix.MathpixClient
import com.mathwrite.app.mathpix.MathpixCredentials
import com.mathwrite.app.sketch.SketchBitmapRenderer
import com.mathwrite.app.sketch.SketchCanvas
import com.mathwrite.app.sketch.SketchStroke
import com.mathwrite.app.sketch.SketchStyle
import com.mathwrite.app.sketch.SketchTool
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
    val context = LocalContext.current
    val endpointPreferences = remember { EndpointPreferences(context) }
    val savedEndpoint = remember { endpointPreferences.load() }
    val tabletName = remember {
        listOf(Build.MANUFACTURER, Build.MODEL)
            .joinToString(" ")
            .replaceFirstChar { it.uppercaseChar() }
    }

    var mathStrokes by remember { mutableStateOf<List<InkStroke>>(emptyList()) }
    var sketchStrokes by remember { mutableStateOf<List<SketchStroke>>(emptyList()) }
    var preview by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Choose the laptop companion on the same network.") }
    var pasteMode by remember { mutableStateOf(MathwriteUiDefaults.DefaultPasteMode) }
    var sequenceId by remember { mutableLongStateOf(1L) }
    val bridgeSessionId = remember { UUID.randomUUID().toString() }
    var isSending by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var boardMode by remember { mutableStateOf(BoardMode.Math) }
    var selectedEndpoint by remember { mutableStateOf(savedEndpoint) }
    var endpointHost by remember { mutableStateOf(savedEndpoint?.host.orEmpty()) }
    var endpointPortText by remember { mutableStateOf((savedEndpoint?.port ?: 18765).toString()) }
    var connectionActive by remember { mutableStateOf(false) }
    var showConnectionSetup by remember { mutableStateOf(savedEndpoint == null) }
    var discoveredDevices by remember { mutableStateOf<List<CompanionDevice>>(emptyList()) }
    var selectedColorArgb by remember { mutableStateOf(0xFF111827.toInt()) }
    var backgroundArgb by remember { mutableStateOf(0xFFFFFFFF.toInt()) }
    var sketchTool by remember { mutableStateOf(SketchTool.Pen) }
    var sketchWidth by remember { mutableStateOf(8f) }
    var sketchCanvasSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val connectionState = ConnectionUiState(selectedEndpoint, connectionActive, showConnectionSetup)

    fun currentEndpoint(): CompanionEndpoint? {
        val port = endpointPortText.toIntOrNull()
        val host = endpointHost.trim()
        return selectedEndpoint ?: if (host.isBlank() || port == null || port !in 1..65535) null else CompanionEndpoint(host, port)
    }

    fun saveAndAnnounceEndpoint(endpoint: CompanionEndpoint) {
        selectedEndpoint = endpoint
        endpointHost = endpoint.host
        endpointPortText = endpoint.port.toString()
        endpointPreferences.save(endpoint)
        status = "Connecting to ${endpoint.displayName}..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                CompanionBridgeClient(endpoint).announceTablet(bridgeSessionId, tabletName)
            }
            status = if (result.ok) {
                connectionActive = true
                showConnectionSetup = false
                "Connected to ${endpoint.displayName}."
            } else {
                connectionActive = false
                showConnectionSetup = true
                result.message ?: "Could not reach ${endpoint.displayName}."
            }
        }
    }

    fun checkCurrentConnection() {
        val endpoint = currentEndpoint()
        if (endpoint == null) {
            selectedEndpoint = null
            connectionActive = false
            showConnectionSetup = true
            status = "Choose a laptop companion before checking."
            return
        }

        selectedEndpoint = endpoint
        status = "Checking ${endpoint.displayName}..."
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                CompanionBridgeClient(endpoint).checkHealth()
            }
            connectionActive = result.ok
            showConnectionSetup = !result.ok
            status = if (result.ok) {
                "Connection active: ${endpoint.displayName}."
            } else {
                result.message ?: "Connection inactive: ${endpoint.displayName}."
            }
        }
    }

    LaunchedEffect(savedEndpoint) {
        if (savedEndpoint != null) {
            checkCurrentConnection()
        }
    }

    val sendMath: () -> Unit = {
        val endpoint = currentEndpoint()
        when {
            endpoint == null -> status = "Choose a laptop companion before sending."
            mathStrokes.isEmpty() -> status = "Write something before sending."
            else -> {
                endpointPreferences.save(endpoint)
                isSending = true
                status = "Recognizing with Mathpix v3/strokes..."

                scope.launch {
                    val recognition = withContext(Dispatchers.IO) {
                        MathpixClient(MathpixCredentials.AppId, MathpixCredentials.AppKey).recognize(mathStrokes)
                    }

                    val latex = recognition.latex
                    if (latex.isNullOrBlank()) {
                        status = recognition.error ?: "No LaTeX returned."
                        isSending = false
                        return@launch
                    }

                    preview = latex
                    status = "Sending LaTeX to ${endpoint.displayName}..."

                    val currentSequence = sequenceId++
                    val bridgeResult = withContext(Dispatchers.IO) {
                        CompanionBridgeClient(endpoint).paste(currentSequence, latex, pasteMode, bridgeSessionId)
                    }

                    status = if (bridgeResult.ok) {
                        connectionActive = true
                        showConnectionSetup = false
                        "Pasted LaTeX through LAN."
                    } else {
                        connectionActive = false
                        showConnectionSetup = true
                        bridgeResult.message ?: "Windows companion paste failed."
                    }
                    isSending = false
                }
            }
        }
    }

    val sendSketch: () -> Unit = {
        val endpoint = currentEndpoint()
        when {
            endpoint == null -> status = "Choose a laptop companion before sending."
            sketchStrokes.isEmpty() -> status = "Draw a sketch before sending."
            sketchCanvasSize.width <= 0 || sketchCanvasSize.height <= 0 -> status = "Sketch board is not ready yet."
            else -> {
                endpointPreferences.save(endpoint)
                isSending = true
                status = "Rendering sketch screenshot..."
                val currentSequence = sequenceId++

                scope.launch {
                    val pngBytes = withContext(Dispatchers.Default) {
                        SketchBitmapRenderer.renderPng(
                            strokes = sketchStrokes,
                            width = sketchCanvasSize.width,
                            height = sketchCanvasSize.height,
                            backgroundArgb = backgroundArgb,
                        )
                    }

                    status = "Sending sketch to ${endpoint.displayName}..."
                    val bridgeResult = withContext(Dispatchers.IO) {
                        CompanionBridgeClient(endpoint).pasteSketch(currentSequence, pngBytes, bridgeSessionId, tabletName)
                    }

                    status = if (bridgeResult.ok) {
                        connectionActive = true
                        showConnectionSetup = false
                        "Pasted sketch screenshot through LAN."
                    } else {
                        connectionActive = false
                        showConnectionSetup = true
                        bridgeResult.message ?: "Windows companion image paste failed."
                    }
                    isSending = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ConnectionStatusBar(
            state = connectionState,
            onCheck = ::checkCurrentConnection,
            onChange = { showConnectionSetup = true },
        )

        if (connectionState.shouldShowSetup) {
            ConnectionPanel(
                endpointHost = endpointHost,
                endpointPortText = endpointPortText,
                discoveredDevices = discoveredDevices,
                isScanning = isScanning,
                onHostChange = {
                    endpointHost = it
                    selectedEndpoint = null
                    connectionActive = false
                },
                onPortChange = {
                    endpointPortText = it.filter(Char::isDigit).take(5)
                    selectedEndpoint = null
                    connectionActive = false
                },
                onConnect = {
                    currentEndpoint()?.let(::saveAndAnnounceEndpoint)
                        ?: run { status = "Enter a valid laptop IP and port." }
                },
                onScan = {
                    val port = endpointPortText.toIntOrNull() ?: 18765
                    isScanning = true
                    status = "Scanning local network..."
                    scope.launch {
                        val devices = withContext(Dispatchers.IO) {
                            CompanionDiscoveryClient(port).scan()
                        }
                        discoveredDevices = devices
                        status = if (devices.isEmpty()) {
                            "No companion found. Check that both devices are on the same Wi-Fi and Windows Firewall allows the app."
                        } else {
                            "Found ${devices.size} companion device(s)."
                        }
                        isScanning = false
                    }
                },
                onDeviceSelected = { device -> saveAndAnnounceEndpoint(device.endpoint) },
            )
        }

        BoardModeToolbar(
            boardMode = boardMode,
            onBoardModeChange = { boardMode = it },
        )

        if (boardMode == BoardMode.Math) {
            MathToolbar(
                canUndo = mathStrokes.isNotEmpty() && !isSending,
                canClear = mathStrokes.isNotEmpty() && !isSending,
                canSend = !isSending,
                isSending = isSending,
                pasteMode = pasteMode,
                onPasteModeChange = { pasteMode = it },
                onUndo = { mathStrokes = mathStrokes.dropLast(1) },
                onClear = {
                    mathStrokes = emptyList()
                    preview = ""
                    status = "Math board cleared."
                },
                onSend = sendMath,
            )
        } else {
            SketchRibbon(
                tool = sketchTool,
                colorArgb = selectedColorArgb,
                width = sketchWidth,
                backgroundArgb = backgroundArgb,
                canUndo = sketchStrokes.isNotEmpty() && !isSending,
                canClear = sketchStrokes.isNotEmpty() && !isSending,
                canSend = !isSending,
                isSending = isSending,
                onToolChange = { sketchTool = it },
                onColorChange = { selectedColorArgb = it },
                onWidthChange = { sketchWidth = it },
                onBackgroundChange = {
                    backgroundArgb = it
                    status = "Sketch fill color changed."
                },
                onUndo = { sketchStrokes = sketchStrokes.dropLast(1) },
                onClear = {
                    sketchStrokes = emptyList()
                    status = "Sketch board cleared."
                },
                onSend = sendSketch,
            )
        }

        val boardModifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
            .padding(8.dp)

        if (boardMode == BoardMode.Math) {
            StrokeCanvas(
                strokes = mathStrokes,
                onStrokeFinished = { stroke -> mathStrokes = mathStrokes + stroke },
                onStylusButtonPressed = sendMath,
                modifier = boardModifier,
            )
        } else {
            SketchCanvas(
                strokes = sketchStrokes,
                style = SketchStyle(selectedColorArgb, sketchWidth, sketchTool),
                backgroundArgb = backgroundArgb,
                onStrokeFinished = { stroke -> sketchStrokes = sketchStrokes + stroke },
                onSizeChanged = { sketchCanvasSize = it },
                modifier = boardModifier,
            )
        }

        if (boardMode == BoardMode.Math) {
            Text("Preview: ${preview.ifBlank { "(nothing recognized yet)" }}")
        }
        Text(status, color = Color(0xFF334155))

        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun ConnectionStatusBar(
    state: ConnectionUiState,
    onCheck: () -> Unit,
    onChange: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (state.isActive) Color(0xFFEFFDF5) else Color(0xFFFFF7ED),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        FlowRow(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.statusText,
                color = if (state.isActive) Color(0xFF166534) else Color(0xFF9A3412),
            )
            OutlinedButton(onClick = onCheck) {
                Text("Check")
            }
            OutlinedButton(onClick = onChange) {
                Text("Change")
            }
        }
    }
}

@Composable
private fun ConnectionPanel(
    endpointHost: String,
    endpointPortText: String,
    discoveredDevices: List<CompanionDevice>,
    isScanning: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    onScan: () -> Unit,
    onDeviceSelected: (CompanionDevice) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = endpointHost,
                    onValueChange = onHostChange,
                    label = { Text("Laptop IP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.52f),
                )
                OutlinedTextField(
                    value = endpointPortText,
                    onValueChange = onPortChange,
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(0.24f),
                )
                Button(onClick = onConnect) {
                    Text("Connect")
                }
                OutlinedButton(enabled = !isScanning, onClick = onScan) {
                    Text(if (isScanning) "Scanning" else "Scan")
                }
            }

            if (discoveredDevices.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    discoveredDevices.forEach { device ->
                        FilterChip(
                            selected = device.host == endpointHost,
                            onClick = { onDeviceSelected(device) },
                            label = { Text(device.displayName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardModeToolbar(
    boardMode: BoardMode,
    onBoardModeChange: (BoardMode) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoardMode.entries.forEach { mode ->
            FilterChip(
                selected = boardMode == mode,
                onClick = { onBoardModeChange(mode) },
                label = { Text(mode.label) },
            )
        }
    }
}

@Composable
private fun MathToolbar(
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
            FilterChip(selected = true, onClick = {}, label = { Text("Pen") })
            FilterChip(selected = true, onClick = {}, label = { Text("S Pen only") })
            OutlinedButton(enabled = canUndo, onClick = onUndo) { Text("Undo") }
            OutlinedButton(enabled = canClear, onClick = onClear) { Text("Clear") }
            Button(enabled = canSend, onClick = onSend) { Text(if (isSending) "Sending" else "Send LaTeX") }
            PasteModeSettingsMenu(pasteMode = pasteMode, onPasteModeChange = onPasteModeChange)
        }
    }
}

@Composable
private fun SketchRibbon(
    tool: SketchTool,
    colorArgb: Int,
    width: Float,
    backgroundArgb: Int,
    canUndo: Boolean,
    canClear: Boolean,
    canSend: Boolean,
    isSending: Boolean,
    onToolChange: (SketchTool) -> Unit,
    onColorChange: (Int) -> Unit,
    onWidthChange: (Float) -> Unit,
    onBackgroundChange: (Int) -> Unit,
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
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SketchTool.entries.forEach { option ->
                    FilterChip(
                        selected = tool == option,
                        onClick = { onToolChange(option) },
                        label = { Text(option.label) },
                    )
                }
                OutlinedButton(enabled = canUndo, onClick = onUndo) { Text("Undo") }
                OutlinedButton(enabled = canClear, onClick = onClear) { Text("Clear") }
                Button(enabled = canSend, onClick = onSend) { Text(if (isSending) "Sending" else "Send Sketch") }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColorPalette(
                    selectedArgb = colorArgb,
                    colors = SketchColors,
                    onColorSelected = onColorChange,
                )
                FillPalette(
                    selectedArgb = backgroundArgb,
                    colors = FillColors,
                    onColorSelected = onBackgroundChange,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Thickness ${width.toInt()}")
                Slider(
                    value = width,
                    onValueChange = onWidthChange,
                    valueRange = 2f..34f,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ColorPalette(
    selectedArgb: Int,
    colors: List<Int>,
    onColorSelected: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        colors.forEach { color ->
            ColorSwatch(
                colorArgb = color,
                selected = selectedArgb == color,
                onClick = { onColorSelected(color) },
            )
        }
    }
}

@Composable
private fun FillPalette(
    selectedArgb: Int,
    colors: List<Int>,
    onColorSelected: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Fill")
        colors.forEach { color ->
            ColorSwatch(
                colorArgb = color,
                selected = selectedArgb == color,
                onClick = { onColorSelected(color) },
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    colorArgb: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(if (selected) 32.dp else 28.dp)
            .background(Color(colorArgb), CircleShape)
            .border(if (selected) 3.dp else 1.dp, Color(0xFF334155), CircleShape)
            .clickable(onClick = onClick),
    )
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

private enum class BoardMode(val label: String) {
    Math("Math"),
    Sketch("Sketch"),
}

private val SketchColors = listOf(
    0xFF111827.toInt(),
    0xFFDC2626.toInt(),
    0xFFF97316.toInt(),
    0xFFFACC15.toInt(),
    0xFF16A34A.toInt(),
    0xFF2563EB.toInt(),
    0xFF7C3AED.toInt(),
    0xFFFFFFFF.toInt(),
)

private val FillColors = listOf(
    0xFFFFFFFF.toInt(),
    0xFFF8FAFC.toInt(),
    0xFFFEF3C7.toInt(),
    0xFFE0F2FE.toInt(),
    0xFFEDE9FE.toInt(),
    0xFF111827.toInt(),
)
