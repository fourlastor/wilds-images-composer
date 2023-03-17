import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.fourlastor.composer.NavHostComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.MainUIDispatcher

@OptIn(ExperimentalDecomposeApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()
    val root = runBlocking { withContext(MainUIDispatcher) { NavHostComponent(DefaultComponentContext(lifecycle)) } }

    application {
        Window(onCloseRequest = ::exitApplication) {
            val windowState = rememberWindowState(size = DpSize(900.dp, 700.dp))

            LifecycleController(lifecycle, windowState)
            MaterialTheme(colors = lightColors(primary = Color(0xffa9c484))) { root.render() }
        }
    }
}
