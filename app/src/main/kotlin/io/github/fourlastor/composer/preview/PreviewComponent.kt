package io.github.fourlastor.composer.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.soywiz.klock.TimeSpan
import io.github.fourlastor.composer.CompleteConversion
import io.github.fourlastor.composer.Component
import io.github.fourlastor.composer.extensions.toBitmap
import io.github.fourlastor.composer.swap
import io.github.fourlastor.composer.ui.HorizontalSeparator
import io.github.fourlastor.composer.ui.PickFolderDialog
import io.github.fourlastor.composer.ui.VerticalSeparator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.jetbrains.skiko.MainUIDispatcher
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PreviewComponent(
    private val context: ComponentContext,
    conversion: CompleteConversion,
    private val goToPickFiles: () -> Unit,
) : Component, ComponentContext by context {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    init {
        lifecycle.doOnDestroy { scope.cancel() }
    }

    private val data = MutableStateFlow(Data(conversion, false, "", ""))
    private val showFileSelect = MutableStateFlow(false)
    private val stateFlow =
        data.combine(showFileSelect) { it, showSelect -> if (showSelect) PreviewState.PickSaveFile else it.toState() }

    @OptIn(ExperimentalSerializationApi::class)
    private fun save(location: File) {
        val current = data.value
        scope.launch {
            val palette = if (current.swapPalette) current.conversion.palette.swap() else current.conversion.palette
            withContext(Dispatchers.IO) {
                File(location, "${current.name}.zip")
                    .outputStream().buffered()
                    .let { ZipOutputStream(it) }.use { zip ->
                        zip.putNextEntry(ZipEntry("front.png"))
                        current.conversion.front[0].inputStream().buffered().copyTo(zip)
                        zip.putNextEntry(ZipEntry("back.png"))
                        current.conversion.back.inputStream().buffered().copyTo(zip)
                        zip.putNextEntry(ZipEntry("data.json"))
                        Json.encodeToStream(
                            JsonFile(
                                animationFrameDurations = current.conversion.durations.map { it.millisecondsLong },
                                credits = current.credits,
                                palette = JsonPalette(
                                    color1 = palette.first.first.hexStringNoAlpha to palette.first.second.hexStringNoAlpha,
                                    color2 = palette.second.first.hexStringNoAlpha to palette.second.second.hexStringNoAlpha,
                                )
                            ), zip
                        )
                        current.conversion.front.forEachIndexed { index, file ->
                            zip.putNextEntry(ZipEntry("animation/$index.png"))
                            file.inputStream().buffered().copyTo(zip)
                        }
                    }
            }
            withContext(MainUIDispatcher) {
                goToPickFiles()
            }
        }

    }

    private suspend fun Data.toState(): PreviewState {
        val palette = if (swapPalette) conversion.palette.swap() else conversion.palette
        return PreviewState.Ready(
            front = listOf(
                conversion.front[0],
                if (swapPalette) conversion.frontInverted[0] else conversion.frontShiny[0]
            ).toBitmaps(),
            back = listOf(
                conversion.back,
                if (swapPalette) conversion.backInverted else conversion.backShiny
            ).toBitmaps(),
            animation = conversion.front.toBitmaps(),
            durations = conversion.durations,
            color1 = palette.first.second.let { Color(it.r, it.g, it.b) },
            color2 = palette.second.second.let { Color(it.r, it.g, it.b) },
        )
    }

    private suspend fun List<File>.toBitmaps() = withContext(Dispatchers.IO) {
        map { it.toBitmap() }
    }

    @Composable
    override fun render() {
        val current by stateFlow.collectAsState(PreviewState.Loading)
        when (val state = current) {
            is PreviewState.Ready -> Preview(
                modifier = Modifier.fillMaxSize(),
                onSwapPalette = { data.update { it.copy(swapPalette = !it.swapPalette) } },
                state = state,
                onUpdateName = { name -> data.update { it.copy(name = name) } },
                onUpdateCredits = { credits -> data.update { it.copy(credits = credits) } },
                onSaveRequest = { showFileSelect.update { true } }
            )

            PreviewState.Loading -> {}
            PreviewState.PickSaveFile -> PickSaveFile(
                onSave = ::save,
                onSaveAbort = { showFileSelect.update { false } }
            )
        }
    }
}

@Composable
private fun PickSaveFile(onSave: (File) -> Unit, onSaveAbort: () -> Unit) {
    PickFolderDialog(
        onCloseRequest = {
            if (it != null) {
                onSave(it)
            } else {
                onSaveAbort()
            }
        }
    )
}

@Composable
private fun Preview(
    modifier: Modifier,
    onSwapPalette: () -> Unit,
    state: PreviewState.Ready,
    onUpdateCredits: (String) -> Unit,
    onUpdateName: (String) -> Unit,
    onSaveRequest: () -> Unit,
) {
    Column(modifier) {
        PreviewRow(
            modifier = Modifier.weight(5f).fillMaxWidth(),
            front = state.front,
            back = state.back,
            animation = state.animation,
            durations = state.durations,
        )
        HorizontalSeparator()
        ControlsRow(
            modifier = Modifier.weight(3f).fillMaxWidth(),
            color1 = state.color1,
            color2 = state.color2,
            onSwapPalette = onSwapPalette,
            onUpdateCredits = onUpdateCredits,
            onUpdateName = onUpdateName,
            onSave = onSaveRequest,
        )
    }
}

@Composable
private fun PreviewRow(
    modifier: Modifier,
    front: List<ImageBitmap>,
    back: List<ImageBitmap>,
    animation: List<ImageBitmap>,
    durations: List<TimeSpan>,
) {
    val durationBgColor = remember { Color(0f, 0f, 0f, 0.4f) }
    Row(modifier = modifier) {
        PreviewImage(text = "Front", images = front, modifier = Modifier.weight(1f).fillMaxHeight())
        VerticalSeparator()
        PreviewImage(text = "Back", images = back, modifier = Modifier.weight(1f).fillMaxHeight())
        VerticalSeparator()
        PreviewImage(
            text = "Animation",
            images = animation,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            extra = {
                Text(
                    text = "${durations[it]}",
                    modifier = Modifier.align(Alignment.BottomStart).background(durationBgColor),
                    color = Color.White,
                )
            }
        )
    }
}

@Composable
private fun ControlsRow(
    modifier: Modifier,
    color1: Color,
    color2: Color,
    onSwapPalette: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateCredits: (String) -> Unit,
    onSave: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val saveEnabled by remember(name) { derivedStateOf { name.isNotEmpty() } }
    var credits by remember { mutableStateOf("") }
    Row(modifier = modifier) {
        SwapPaletteControl(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            onSwap = onSwapPalette,
            color1 = color1,
            color2 = color2,
        )
        VerticalSeparator()
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            TextField(value = name, onValueChange = {
                name = it
                onUpdateName(it)
            }, label = { Text("Name") }, singleLine = true)
            TextField(value = credits, onValueChange = {
                credits = it
                onUpdateCredits(it)
            }, label = { Text("Credits") })
        }
        VerticalSeparator()
        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)) {
            Button(modifier = Modifier.fillMaxSize(), onClick = { onSave() }, enabled = saveEnabled) {
                Text("Save", fontSize = 40.sp)
            }
        }
    }
}

@Composable
private fun SwapPaletteControl(
    modifier: Modifier,
    onSwap: () -> Unit,
    color1: Color,
    color2: Color,
) {
    Column(modifier.padding(4.dp)) {
        Text("Shiny palette", fontSize = 24.sp)
        Row(Modifier.fillMaxWidth().weight(1f)) {
            ColorPreview(modifier = Modifier.align(Alignment.CenterVertically), color = color1)
            Box(Modifier.fillMaxHeight().weight(1f)) {
                Image(
                    painter = painterResource("swap.svg"),
                    contentDescription = "Swap palettes",
                    modifier = Modifier.align(Alignment.Center)
                        .size(40.dp)
                        .clickable { onSwap() }
                )
            }
            ColorPreview(modifier = Modifier.align(Alignment.CenterVertically), color = color2)
        }
    }
}

@Composable
private fun ColorPreview(
    color: Color,
    modifier: Modifier,
) {
    Box(modifier.size(60.dp).background(color))
}

@Composable
private fun PreviewImage(
    text: String,
    images: List<ImageBitmap>,
    modifier: Modifier,
    extra: @Composable BoxScope.(Int) -> Unit = {},
) {
    Column(modifier.padding(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(text, fontSize = 30.sp, modifier = Modifier.align(Alignment.Center))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(images.size) {
                BoxWithConstraints {
                    Box(Modifier.size(maxWidth * 1f).align(Alignment.Center)) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = images[it],
                            contentDescription = null,
                            filterQuality = FilterQuality.None,
                        )
                        extra(it)
                    }
                }
            }
        }
    }
}

@Serializable
private data class JsonFile(
    val animationFrameDurations: List<Long>,
    val credits: String,
    val palette: JsonPalette,
)

@Serializable
private data class JsonPalette(
    val color1: Pair<String, String>,
    val color2: Pair<String, String>,
)

private data class Data(
    val conversion: CompleteConversion,
    val swapPalette: Boolean,
    val name: String,
    val credits: String,
)


private sealed interface PreviewState {
    object Loading : PreviewState

    object PickSaveFile : PreviewState

    data class Ready(
        val front: List<ImageBitmap>,
        val back: List<ImageBitmap>,
        val animation: List<ImageBitmap>,
        val durations: List<TimeSpan>,
        val color1: Color,
        val color2: Color,
    ) : PreviewState
}
