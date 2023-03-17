package io.github.fourlastor.composer.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import io.github.fourlastor.composer.Component
import io.github.fourlastor.composer.ShinyPalette
import io.github.fourlastor.composer.extensions.toBitmap
import io.github.fourlastor.composer.ui.HorizontalSeparator
import io.github.fourlastor.composer.ui.VerticalSeparator
import java.io.File

class PreviewComponent(
    private val context: ComponentContext,
    val front: List<File>,
    val frontShiny: List<File>,
    val frontInverted: List<File>,
    val back: File,
    val backShiny: File,
    val backInverted: File,
    val shinyPalette: ShinyPalette,
) : Component, ComponentContext by context {

    @Composable
    override fun render() {
        val frontImages by remember(front, frontShiny) {
            derivedStateOf {
                listOf(
                    front[0],
                    frontShiny[0]
                ).map { it.toBitmap() }
            }
        }
        val backImages by remember(back, backShiny) { derivedStateOf { listOf(back, backShiny).map { it.toBitmap() } } }
        val animationImages by remember(front) { derivedStateOf { front.map { it.toBitmap() } } }
        Preview(
            modifier = Modifier.fillMaxSize(),
            front = frontImages,
            back = backImages,
            animation = animationImages,
            palette = shinyPalette,
        )
    }
}

@Composable
private fun Preview(
    modifier: Modifier,
    front: List<ImageBitmap>,
    back: List<ImageBitmap>,
    animation: List<ImageBitmap>,
    palette: ShinyPalette,
) {
    Column(modifier) {
        Row(modifier = Modifier.weight(5f).fillMaxWidth()) {
            PreviewImage(text = "Front", images = front, modifier = Modifier.weight(1f).fillMaxHeight())
            VerticalSeparator()
            PreviewImage(text = "Back", images = back, modifier = Modifier.weight(1f).fillMaxHeight())
            VerticalSeparator()
            PreviewImage(text = "Animation", images = animation, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        HorizontalSeparator()
        Row(modifier = Modifier.weight(3f).fillMaxWidth()) {
            SwapPaletteControl(modifier = Modifier.weight(1f).fillMaxHeight(), palette = palette)
            VerticalSeparator()
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                var name by remember { mutableStateOf("") }
                var credits by remember { mutableStateOf("") }
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                TextField(value = credits, onValueChange = { credits = it }, label = { Text("Credits") })
            }
            VerticalSeparator()
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp)) {
                Button(modifier = Modifier.fillMaxSize(), onClick = {}) {
                    Text("Save", fontSize = 40.sp)
                }
            }
        }
    }
}

@Composable
private fun SwapPaletteControl(
    modifier: Modifier,
    palette: ShinyPalette,
) {
    val color1 = remember { palette.first.second.let { Color(it.r, it.g, it.b) } }
    val color2 = remember { palette.second.second.let { Color(it.r, it.g, it.b) } }
    Column(modifier.padding(4.dp)) {
        Text("Shiny palette", fontSize = 24.sp)
        Row(Modifier.fillMaxWidth().weight(1f)) {
            ColorPreview(modifier = Modifier.align(Alignment.CenterVertically), color = color1)
            Box(Modifier.fillMaxHeight().weight(1f)) {
                Image(
                    painter = painterResource("swap.svg"),
                    contentDescription = "Swap palettes",
                    modifier = Modifier.align(Alignment.Center)
                        .size(40.dp).clickable { }
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
private fun PreviewImage(text: String, images: List<ImageBitmap>, modifier: Modifier) {
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
                    Image(
                        modifier = Modifier.size(maxWidth * 1f).align(Alignment.Center),
                        bitmap = images[it],
                        contentDescription = null,
                        filterQuality = FilterQuality.None,
                    )
                }
            }
        }
    }
}