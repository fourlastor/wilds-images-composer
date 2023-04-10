package io.github.fourlastor.composer.pick

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import io.github.fourlastor.composer.Component
import io.github.fourlastor.composer.extensions.toBitmap
import io.github.fourlastor.composer.ui.FileLoadDialog
import io.github.fourlastor.composer.ui.HorizontalSeparator
import io.github.fourlastor.composer.ui.VerticalSeparator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PickImageComponent(
    private val context: ComponentContext,
    private val goToConversion: (File, File, File, File, File) -> Unit,
) : Component, ComponentContext by context {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val imagesState = MutableStateFlow(Images())
    private val imageFiles = MutableStateFlow(ImageFiles())

    init {
        lifecycle.doOnDestroy {
            scope.cancel()
        }
    }

    @Composable
    override fun render() {
        val images by imagesState.collectAsState()
        val files by imageFiles.collectAsState()
        PickImage(
            modifier = Modifier.fillMaxSize(),
            onContinue = goToConversion,
            onImagePicked = ::onImagePicked,
            images = images,
            files = files,
        )
    }

    private fun onImagePicked(file: File, pickImage: PickImage) {
        scope.launch {
            val image = withContext(Dispatchers.IO) { file.toBitmap() }
            imagesState.update {
                when (pickImage) {
                    PickImage.NONE -> it
                    PickImage.FRONT -> it.copy(front = image)
                    PickImage.BACK -> it.copy(back = image)
                    PickImage.SHINY -> it.copy(shiny = image)
                    PickImage.OVERWORLD -> it.copy(overworld = image)
                    PickImage.OVERWORLDSHINY -> it.copy(overworldshiny = image)
                }
            }
            imageFiles.update {
                when (pickImage) {
                    PickImage.NONE -> it
                    PickImage.FRONT -> it.copy(front = file)
                    PickImage.BACK -> it.copy(back = file)
                    PickImage.SHINY -> it.copy(shiny = file)
                    PickImage.OVERWORLD -> it.copy(overworld = file)
                    PickImage.OVERWORLDSHINY -> it.copy(overworldshiny = file)
                }
            }
        }
    }
}

@Composable
private fun PickImage(
    modifier: Modifier = Modifier,
    onContinue: (File, File, File, File, File) -> Unit,
    onImagePicked: (File, PickImage) -> Unit,
    images: Images,
    files: ImageFiles,
) {
    var pickImage: PickImage by remember { mutableStateOf(PickImage.NONE) }
    val continueState by remember(files) {
        derivedStateOf {
            if (files.front != null && files.back != null && files.shiny != null && files.overworld != null && files.overworldshiny != null) {
                Continue.Enabled(files.front, files.back, files.shiny, files.overworld, files.overworldshiny)
            } else {
                Continue.Disabled
            }
        }
    }
    var lastPath: String? by remember { mutableStateOf(null) }
    if (pickImage != PickImage.NONE) {
        FileLoadDialog(
            onCloseRequest = {
                if (it != null) {
                    lastPath = it.parentFile.absolutePath
                    onImagePicked(it, pickImage)
                }
                pickImage = PickImage.NONE
            },
            filterList = pickImage.filter,
            initialPath = lastPath
        )
    }
    Column(modifier = modifier) {
        Box(Modifier.fillMaxWidth().height(80.dp).padding(8.dp)) {
            Text("You can also drag and drop files on this window (WIP)", fontSize = 18.sp)
            val onClick: () -> Unit by remember(continueState, onContinue) {
                derivedStateOf {
                    continueState.let {
                        if (it is Continue.Enabled) {
                            { onContinue(it.front, it.back, it.shiny, it.overworld, it.overworldshiny) }
                        } else {
                            {}
                        }
                    }
                }
            }

            Button(
                onClick = onClick,
                modifier = Modifier.align(Alignment.CenterEnd),
                enabled = continueState is Continue.Enabled
            ) {
                Text("Continue")
            }
        }
        HorizontalSeparator()
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            ImageArea(
                text = "Front",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onSelectFile = { pickImage = PickImage.FRONT },
                image = images.front,
            )
            VerticalSeparator()
            ImageArea(
                text = "Back",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onSelectFile = { pickImage = PickImage.BACK },
                image = images.back,
            )
            VerticalSeparator()
            ImageArea(
                text = "Shiny",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onSelectFile = { pickImage = PickImage.SHINY },
                image = images.shiny,
            )
            VerticalSeparator()
            ImageArea(
                text = "Overworld",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onSelectFile = { pickImage = PickImage.OVERWORLD },
                image = images.overworld,
            )
            VerticalSeparator()
            ImageArea(
                text = "Overworld Shiny",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onSelectFile = { pickImage = PickImage.OVERWORLDSHINY },
                image = images.overworldshiny,
            )
        }
    }
}

@Composable
private fun ImageArea(text: String, modifier: Modifier = Modifier, onSelectFile: () -> Unit, image: ImageBitmap?) =
    Column(modifier = modifier) {
        Box(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
            Text(text, fontSize = 30.sp, modifier = Modifier.align(Alignment.Center))
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Box(modifier = Modifier.size(maxWidth * 0.8f).align(Alignment.Center)) {
                if (image != null) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        bitmap = image,
                        contentDescription = null,
                        filterQuality = FilterQuality.None
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Black))
                }

            }
        }
        Button(onClick = onSelectFile, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Select file", fontSize = 20.sp)
        }
    }

data class ImageFiles(
    val front: File? = null,
    val back: File? = null,
    val shiny: File? = null,
    val overworld: File? = null,
    val overworldshiny: File? = null,
)

sealed interface Continue {
    object Disabled : Continue
    data class Enabled(
        val front: File,
        val back: File,
        val shiny: File,
        val overworld: File,
        val overworldshiny: File,
    ) : Continue
}

data class Images(
    val front: ImageBitmap? = null,
    val back: ImageBitmap? = null,
    val shiny: ImageBitmap? = null,
    val overworld: ImageBitmap? = null,
    val overworldshiny: ImageBitmap? = null,
)

enum class PickImage(val filter: List<String>) {
    NONE(emptyList()), FRONT(listOf("gif")), BACK(listOf("png")), SHINY(listOf("png")), OVERWORLD(listOf("png")), OVERWORLDSHINY(listOf("png"))
}
