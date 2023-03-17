package io.github.fourlastor.composer

import com.soywiz.korim.color.RGBA

typealias ShinyPalette = Pair<Pair<RGBA, RGBA>, Pair<RGBA, RGBA>>
fun ShinyPalette.swap(): ShinyPalette = (first.first to second.second) to (second.first to first.second)
