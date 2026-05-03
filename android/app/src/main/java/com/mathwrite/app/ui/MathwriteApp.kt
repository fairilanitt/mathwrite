package com.mathwrite.app.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.delay
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
    var pasteMode by remember { mutableStateOf(MathwriteUiDefaults.DefaultPasteMode) }
    var sequenceId by remember { mutableLongStateOf(1L) }
    var noticeSequence by remember { mutableLongStateOf(0L) }
    var notice by remember { mutableStateOf<TimedNotice?>(null) }
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

    fun showNotice(message: String) {
        noticeSequence += 1
        notice = TimedNotice(noticeSequence, message, System.currentTimeMillis())
    }

    LaunchedEffect(notice?.id) {
        val activeNoticeId = notice?.id ?: return@LaunchedEffect
        delay(TimedNotice.VisibleMillis)
        if (notice?.id == activeNoticeId) {
            notice = null
        }
    }

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
        showNotice("Connecting to ${endpoint.displayName}...")
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                CompanionBridgeClient(endpoint).announceTablet(bridgeSessionId, tabletName)
            }
            if (result.ok) {
                connectionActive = true
                showConnectionSetup = false
                showNotice("Connected to ${endpoint.displayName}.")
            } else {
                connectionActive = false
                showConnectionSetup = true
                showNotice(result.message ?: "Could not reach ${endpoint.displayName}.")
            }
        }
    }

    fun checkCurrentConnection() {
        val endpoint = currentEndpoint()
        if (endpoint == null) {
            selectedEndpoint = null
            connectionActive = false
            showConnectionSetup = true
            showNotice("Choose a laptop companion before checking.")
            return
        }

        selectedEndpoint = endpoint
        showNotice("Checking ${endpoint.displayName}...")
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                CompanionBridgeClient(endpoint).checkHealth()
            }
            connectionActive = result.ok
            showConnectionSetup = !result.ok
            if (result.ok) {
                showNotice("Connection active: ${endpoint.displayName}.")
            } else {
                showNotice(result.message ?: "Connection inactive: ${endpoint.displayName}.")
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
            endpoint == null -> showNotice("Choose a laptop companion before sending.")
            mathStrokes.isEmpty() -> showNotice("Write something before sending.")
            else -> {
                endpointPreferences.save(endpoint)
                isSending = true
                showNotice("Recognizing with Mathpix v3/strokes...")

                scope.launch {
                    val recognition = withContext(Dispatchers.IO) {
                        MathpixClient(MathpixCredentials.AppId, MathpixCredentials.AppKey).recognize(mathStrokes)
                    }

                    val latex = recognition.latex
                    if (latex.isNullOrBlank()) {
                        showNotice(recognition.error ?: "No LaTeX returned.")
                        isSending = false
                        return@launch
                    }

                    showNotice("Sending LaTeX to ${endpoint.displayName}...")

                    val currentSequence = sequenceId++
                    val bridgeResult = withContext(Dispatchers.IO) {
                        CompanionBridgeClient(endpoint).paste(currentSequence, latex, pasteMode, bridgeSessionId)
                    }

                    if (bridgeResult.ok) {
                        connectionActive = true
                        showConnectionSetup = false
                        showNotice("Pasted LaTeX through LAN.")
                    } else {
                        connectionActive = false
                        showConnectionSetup = true
                        showNotice(bridgeResult.message ?: "Windows companion paste failed.")
                    }
                    isSending = false
                }
            }
        }
    }

    val sendSketch: () -> Unit = {
        val endpoint = currentEndpoint()
        when {
            endpoint == null -> showNotice("Choose a laptop companion before sending.")
            sketchStrokes.isEmpty() -> showNotice("Draw a sketch before sending.")
            sketchCanvasSize.width <= 0 || sketchCanvasSize.height <= 0 -> showNotice("Sketch board is not ready yet.")
            else -> {
                endpointPreferences.save(endpoint)
                isSending = true
                showNotice("Rendering sketch screenshot...")
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

                    showNotice("Sending sketch to ${endpoint.displayName}...")
                    val bridgeResult = withContext(Dispatchers.IO) {
                        CompanionBridgeClient(endpoint).pasteSketch(currentSequence, pngBytes, bridgeSessionId, tabletName)
                    }

                    if (bridgeResult.ok) {
                        connectionActive = true
                        showConnectionSetup = false
                        showNotice("Pasted sketch screenshot through LAN.")
                    } else {
                        connectionActive = false
                        showConnectionSetup = true
                        showNotice(bridgeResult.message ?: "Windows companion image paste failed.")
                    }
                    isSending = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        ?: run { showNotice("Enter a valid laptop IP and port.") }
                },
                onScan = {
                    val port = endpointPortText.toIntOrNull() ?: 18765
                    isScanning = true
                    showNotice("Scanning local network...")
                    scope.launch {
                        val devices = withContext(Dispatchers.IO) {
                            CompanionDiscoveryClient(port).scan()
                        }
                        discoveredDevices = devices
                        if (devices.isEmpty()) {
                            showNotice("No companion found. Check Wi-Fi and firewall.")
                        } else {
                            showNotice("Found ${devices.size} companion device(s).")
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
                    showNotice("Math board cleared.")
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
                    showNotice("Sketch fill color changed.")
                },
                onUndo = { sketchStrokes = sketchStrokes.dropLast(1) },
                onClear = {
                    sketchStrokes = emptyList()
                    showNotice("Sketch board cleared.")
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

        }

        AnimatedVisibility(
            visible = notice != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        ) {
            NoticeBubble(message = notice?.message.orEmpty())
        }
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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.statusText,
                color = if (state.isActive) Color(0xFF166534) else Color(0xFF9A3412),
            )
            IconCircleButton(MathwriteIcon.CheckConnection, "Check connection", onClick = onCheck)
            IconCircleButton(MathwriteIcon.ChangeConnection, "Change connection", onClick = onChange)
        }
    }
}

@Composable
private fun NoticeBubble(message: String) {
    Surface(
        modifier = Modifier.widthIn(min = 72.dp, max = 560.dp),
        color = Color.Black,
        contentColor = Color.White,
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 8.dp,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            color = Color.White,
        )
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
                horizontalArrangement = Arrangement.SpaceEvenly,
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
                IconCircleButton(MathwriteIcon.Connect, "Connect", onClick = onConnect)
                IconCircleButton(MathwriteIcon.Scan, "Scan network", enabled = !isScanning, onClick = onScan)
            }

            if (discoveredDevices.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    discoveredDevices.forEach { device ->
                        Surface(
                            modifier = Modifier.clickable { onDeviceSelected(device) },
                            color = if (device.host == endpointHost) Color.Black else Color.White,
                            contentColor = if (device.host == endpointHost) Color.White else Color.Black,
                            shape = RoundedCornerShape(999.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        ) {
                            Text(
                                text = device.displayName,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
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
    ToolbarStrip {
        IconCircleButton(
            icon = MathwriteIcon.MathMode,
            contentDescription = "Math mode",
            selected = boardMode == BoardMode.Math,
            onClick = { onBoardModeChange(BoardMode.Math) },
        )
        IconCircleButton(
            icon = MathwriteIcon.SketchMode,
            contentDescription = "Sketch mode",
            selected = boardMode == BoardMode.Sketch,
            onClick = { onBoardModeChange(BoardMode.Sketch) },
        )
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
    ToolbarStrip {
        IconCircleButton(MathwriteIcon.Pen, "Pen", selected = true, onClick = {})
        IconCircleButton(MathwriteIcon.StylusOnly, "S Pen only", selected = true, onClick = {})
        IconCircleButton(MathwriteIcon.Undo, "Undo", enabled = canUndo, onClick = onUndo)
        IconCircleButton(MathwriteIcon.Clear, "Clear", enabled = canClear, onClick = onClear)
        IconCircleButton(MathwriteIcon.SendLatex, if (isSending) "Sending LaTeX" else "Send LaTeX", enabled = canSend, onClick = onSend)
        PasteModeSettingsMenu(pasteMode = pasteMode, onPasteModeChange = onPasteModeChange)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircleButton(MathwriteIcon.Pen, "Pen", selected = tool == SketchTool.Pen, onClick = { onToolChange(SketchTool.Pen) })
                IconCircleButton(MathwriteIcon.Highlighter, "Highlighter", selected = tool == SketchTool.Highlighter, onClick = { onToolChange(SketchTool.Highlighter) })
                IconCircleButton(MathwriteIcon.Eraser, "Eraser", selected = tool == SketchTool.Eraser, onClick = { onToolChange(SketchTool.Eraser) })
                IconCircleButton(MathwriteIcon.Undo, "Undo", enabled = canUndo, onClick = onUndo)
                IconCircleButton(MathwriteIcon.Clear, "Clear", enabled = canClear, onClick = onClear)
                IconCircleButton(MathwriteIcon.SendSketch, if (isSending) "Sending sketch" else "Send sketch", enabled = canSend, onClick = onSend)
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
        MathwriteGlyph(
            icon = MathwriteIcon.Fill,
            tint = Color.Black,
            modifier = Modifier.size(24.dp),
        )
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
        IconCircleButton(MathwriteIcon.More, "More settings", onClick = { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Current: ${pasteMode.label}") },
                onClick = { expanded = false },
            )
            LatexPasteMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
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
