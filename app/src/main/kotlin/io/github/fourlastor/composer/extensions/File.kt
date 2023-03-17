package io.github.fourlastor.composer.extensions

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.io.File

fun File.toBitmap(): ImageBitmap =
    inputStream().buffered().use(::loadImageBitmap)
