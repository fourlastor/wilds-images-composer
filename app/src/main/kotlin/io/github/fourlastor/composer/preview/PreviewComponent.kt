package io.github.fourlastor.composer.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
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
import com.soywiz.korio.util.DynamicJvm.toInt
import io.github.fourlastor.composer.CompleteConversion
import io.github.fourlastor.composer.Component
import io.github.fourlastor.composer.extensions.toBitmap
import io.github.fourlastor.composer.swap
import io.github.fourlastor.composer.ui.HorizontalSeparator
import io.github.fourlastor.composer.ui.PickFolderDialog
import io.github.fourlastor.composer.ui.VerticalSeparator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import org.jetbrains.skiko.MainUIDispatcher
import java.awt.image.BufferedImage
import java.io.File
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO


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
            withContext(Dispatchers.IO) {
                File(location, "${current.name}-v08.zip")
                    .outputStream().buffered()
                    .let { ZipOutputStream(it) }.use { zip ->

                        // Note: the fact that this is ARBG and not RGBA may cause issues.
                        // https://stackoverflow.com/questions/65569243/getting-a-rgba-byte-array-from-a-bufferedimage-java
                        val firstPng = ImageIO.read(current.conversion.front[0])
                        val combinedHeight = current.conversion.front.sumBy { ImageIO.read(it).height }
                        val maxWidth = current.conversion.front.map { ImageIO.read(it).width }.maxOrNull() ?: 0
                        //val combinedImage = firstPng.colorModel.createCompatibleWritableRaster(maxWidth, combinedHeight)
                        val combinedImage = BufferedImage(maxWidth, combinedHeight, BufferedImage.TYPE_INT_ARGB)

                        var index = 0
                        val animAsm = mutableListOf<String>()
                        var currentY = 0
                        for (file in current.conversion.front) {
                            val image = ImageIO.read(file)
                            val convertedImg = BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB)
                            convertedImg.graphics.drawImage(image, 0, 0, null)
                            combinedImage.graphics.drawImage(convertedImg, 0, currentY, null)
                            currentY += image.height

                            // anim.asm
                            var duration = (current.conversion.durations[index].milliseconds / 1000.0) * 60.0;
                            animAsm.add("	frame %d, %02d".format(index, toInt(duration)))
                            index += 1
                        }
                        animAsm.add("	endanim")

                        zip.putNextEntry(ZipEntry("front.png"))
                        ImageIO.write(combinedImage, "png", zip)
                        zip.putNextEntry(ZipEntry("back.png"))
                        current.conversion.back.inputStream().buffered().copyTo(zip)

                        // Write anim asm lines to file.
                        zip.putNextEntry(ZipEntry("anim.asm"))
                        // Note: outputStream.close() just calls zip.close,
                        // so I think it's safe to not close() outputStream.
                        var outputStream = OutputStreamWriter(zip, Charsets.UTF_8)
                        animAsm.forEachIndexed { index, string ->
                            outputStream.write(string)
                            if (index != animAsm.lastIndex) {
                                outputStream.write(System.lineSeparator())
                            }
                        }
                        outputStream.flush()

                        // Write credits to file.
                        zip.putNextEntry(ZipEntry("credits.txt"))
                        outputStream = OutputStreamWriter(zip, Charsets.UTF_8)
                        outputStream.write(current.credits)
                        outputStream.flush()

                        // Write shiny.pal to file
                        val shinyLines = mutableListOf<String>("")
                        shinyLines.add("\tRGB %02d, %02d, %02d".format(palette.second.first.r/8, palette.second.first.g/8, palette.second.first.b/8))
                        shinyLines.add("\tRGB %02d, %02d, %02d".format(palette.first.second.r/8, palette.first.second.g/8, palette.first.second.b/8))
                        zip.putNextEntry(ZipEntry("shiny.pal"))
                        outputStream = OutputStreamWriter(zip, Charsets.UTF_8)
                        shinyLines.forEach { string ->
                            outputStream.write(string)
                            outputStream.write(System.lineSeparator())
                        }
                        outputStream.flush()
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
            }, label = { Text("Mon Name") }, singleLine = true)
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
