package io.github.fourlastor.composer

import java.io.File

data class CompleteConversion(
    val front: List<File>,
    val frontShiny: List<File>,
    val frontInverted: List<File>,
    val back: File,
    val backShiny: File,
    val backInverted: File,
    val palette: ShinyPalette,
)
