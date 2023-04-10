package io.github.fourlastor.composer

import com.soywiz.klock.TimeSpan
import java.io.File

data class CompleteConversion(
    val front: List<File>,
    val frontShiny: List<File>,
    val frontInverted: List<File>,
    val back: File,
    val backShiny: File,
    val backInverted: File,
    val overworld: File,
    val overworldShiny: File,
    val palette: ShinyPalette,
    val durations: List<TimeSpan>,
)
