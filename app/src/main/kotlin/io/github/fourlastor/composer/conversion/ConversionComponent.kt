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
import io.github.fourlastor.composer.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversionComponent(
    private val context: ComponentContext,
    private val goToPreview: () -> Unit,
) : Component, ComponentContext by context {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val progress = MutableStateFlow(0f)

    init {
        lifecycle.doOnCreate {
            scope.launch {
                delay(500)
                progress.update { 0.3f }
                delay(500)
                progress.update { 0.6f }
                delay(500)
                progress.update { 1f }
            }
        }

        lifecycle.doOnDestroy {
            scope.cancel()
        }
    }

    @Composable
    override fun render() {
        val progress by progress.collectAsState()

        LaunchedEffect(progress == 1f) {
            if (progress == 1f) {
                delay(200)
                goToPreview()
            }
        }
        Conversion(progress, Modifier.fillMaxSize())
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
