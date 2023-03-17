package io.github.fourlastor.composer.conversion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.GIF
import com.soywiz.korim.format.PNG
import com.soywiz.korio.file.std.toVfs
import io.github.fourlastor.composer.CompleteConversion
import io.github.fourlastor.composer.Component
import io.github.fourlastor.composer.ShinyPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class ConversionComponent(
    private val context: ComponentContext,
    private val goToPreview: (CompleteConversion) -> Unit,
    front: File,
    back: File,
    shiny: File,
) : Component, ComponentContext by context {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val progress = MutableStateFlow<Progress>(Progress.InProgress(0f))

    init {
        lifecycle.doOnCreate {
            scope.launch {
                val frontImage = withContext(Dispatchers.IO) { GIF.readImage(front.toVfs().readAsSyncStream()) }
                val backImage = withContext(Dispatchers.IO) { PNG.readImage(back.toVfs().readAsSyncStream()) }
                val shinyImage = withContext(Dispatchers.IO) { PNG.readImage(shiny.toVfs().readAsSyncStream()) }
                val frontFrames = frontImage.frames.map { it.bitmap }

                progress.update { Progress.InProgress(0.3f) }
                frontFrames.forEach { it.fixColors() }
                val backBmp = backImage.mainBitmap
                backBmp.fixColors()
                val shinyBmp = shinyImage.mainBitmap
                shinyBmp.fixColors()
                progress.update { Progress.InProgress(0.6f) }
                val palette = shinyBmp.findPalette(backBmp)
                val invertedPalette = palette.invert()

                val frontFiles =
                    withContext(Dispatchers.IO) {
                        List(frontFrames.size) {
                            File.createTempFile("front_", ".png").also { it.deleteOnExit() }
                        }
                    }
                val frontShinyFiles =
                    withContext(Dispatchers.IO) {
                        List(frontFrames.size) {
                            File.createTempFile("front_s", ".png").also { it.deleteOnExit() }
                        }
                    }
                val frontInvertedFiles =
                    withContext(Dispatchers.IO) {
                        List(frontFrames.size) {
                            File.createTempFile("front_si", ".png").also { it.deleteOnExit() }
                        }
                    }
                val backFile =
                    withContext(Dispatchers.IO) { File.createTempFile("back_", ".png").also { it.deleteOnExit() } }
                val backShinyFile =
                    withContext(Dispatchers.IO) { File.createTempFile("back_s", ".png").also { it.deleteOnExit() } }
                val backInvertedFile =
                    withContext(Dispatchers.IO) { File.createTempFile("back_si", ".png").also { it.deleteOnExit() } }

                frontFrames.forEachIndexed { index, bitmap ->
                    bitmap.saveTo(frontFiles[index])
                    bitmap.toShiny(palette).saveTo(frontShinyFiles[index])
                    bitmap.toShiny(invertedPalette).saveTo(frontInvertedFiles[index])
                }
                backBmp.saveTo(backFile)
                backBmp.toShiny(palette).saveTo(backShinyFile)
                backBmp.toShiny(invertedPalette).saveTo(backInvertedFile)
                progress.update {
                    Progress.Complete(
                        CompleteConversion(
                            frontFiles,
                            frontShinyFiles,
                            frontInvertedFiles,
                            backFile,
                            backShinyFile,
                            backInvertedFile,
                            palette
                        )
                    )
                }
            }
        }

        lifecycle.doOnDestroy {
            scope.cancel()
        }
    }

    private fun Bitmap.saveTo(file: File) = file.outputStream().buffered().use {
        it.write(PNG.encode(this))
    }

    private fun ShinyPalette.invert(): ShinyPalette = (first.first to second.second) to (second.first to first.second)

    private fun Bitmap.toShiny(palette: ShinyPalette): Bitmap = clone().apply {
        forEach { _, x, y ->
            when (getRgba(x, y)) {
                palette.first.first -> setRgba(x, y, palette.first.second)
                palette.second.first -> setRgba(x, y, palette.second.second)
            }
        }
    }

    private fun Bitmap.findPalette(original: Bitmap): ShinyPalette {
        var min = Colors.WHITE
        var minOriginal = Colors.WHITE
        var max = Colors.BLACK
        var maxOriginal = Colors.BLACK
        forEach { _, x, y ->
            val color = getRgba(x, y)
            if (color.rgb != Colors.BLACK.rgb && color.rgb != Colors.WHITE.rgb) {
                when {
                    color.brightness > max.brightness -> {
                        max = color
                        maxOriginal = original.getRgba(x, y)
                    }

                    color.brightness < min.brightness -> {
                        min = color
                        minOriginal = original.getRgba(x, y)
                    }
                }
            }
        }

        return (minOriginal to min) to (maxOriginal to max)
    }

    private val RGBA.brightness: Int
        get() = r + g + b


    private fun Bitmap.fixColors() {
        forEach { _, x, y ->
            val color = getRgba(x, y)
            when {
                color.a == 0 -> {
                    setRgba(x, y, Colors.TRANSPARENT_BLACK)
                }

                color.withinTolerance(TOLERANCE, Colors.WHITE) -> {
                    setRgba(x, y, Colors.WHITE)
                }

                color.withinTolerance(TOLERANCE, Colors.BLACK) -> {
                    setRgba(x, y, Colors.BLACK)
                }
            }
        }
        val queue = LinkedList<Pair<Int, Int>>()
        for (x in 0 until width) {
            queue.push(x to 0)
            queue.push(x to height - 1)
        }
        for (y in 0 until height) {
            queue.push(y to 0)
            queue.push(y to width - 1)
        }

        var e = queue.poll()
        while (e != null) {
            val (x, y) = e
            if (getRgba(x, y) == Colors.WHITE) {
                setRgba(x, y, Colors.TRANSPARENT_BLACK)
                if (x > 0) queue.push(x - 1 to y)
                if (x < width - 1) queue.push(x + 1 to y)
                if (y > 0) queue.push(x to y - 1)
                if (y < height - 1) queue.push(x to y + 1)
            }
            e = queue.poll()
        }
    }

    private fun RGBA.withinTolerance(tolerance: Int, other: RGBA) =
        (r < other.r + tolerance && g < other.g + tolerance && b < other.b + tolerance)
                && (r > other.r - tolerance && g > other.g - tolerance && b > other.b - tolerance)

    @Composable
    override fun render() {
        val progress by progress.collectAsState()

        LaunchedEffect(progress is Progress.Complete) {
            val complete = progress as? Progress.Complete ?: return@LaunchedEffect
            delay(200)
            goToPreview(complete.conversion)
        }
        Conversion(progress.progress, Modifier.fillMaxSize())
    }

    companion object {
        const val TOLERANCE = 8
    }
}

@Composable
fun Conversion(progress: Float, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Loading...")
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(0.8f).height(2.dp)
        )
    }
}

private sealed interface Progress {

    val progress: Float

    data class InProgress(override val progress: Float) : Progress
    data class Complete(
        val conversion: CompleteConversion,
    ) : Progress {
        override val progress: Float = 1f
    }
}
