package io.github.fourlastor.composer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File

@Composable
fun FileLoadDialog(
    onCloseRequest: (result: File?) -> Unit,
    filterList: List<String> = emptyList(),
    initialPath: String? = null,
) {
    FileDialog(
        type = Type.Load,
        filterList = filterList,
        onCloseRequest = onCloseRequest,
        initialPath = initialPath,
    )
}

@Composable
fun FileSaveDialog(
    onCloseRequest: (result: File?) -> Unit,
    filterList: List<String> = emptyList(),
    initialPath: String? = null,
) {
    FileDialog(
        type = Type.Save,
        filterList = filterList,
        onCloseRequest = onCloseRequest,
        initialPath = initialPath,
    )
}

@Composable
private fun FileDialog(
    type: Type,
    initialPath: String?,
    filterList: List<String>,
    onCloseRequest: (result: File?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val job = scope.launch {
            val path = (initialPath ?: System.getProperty("user.home"))
                .let { if (it.endsWith("/")) it else "$it/" }
                .let {
                    if (inWindows()) {
                        it.replace("/", "\\")
                    } else {
                        it
                    }
                }
            MemoryStack.stackPush().use { stack ->
                val filter: PointerBuffer? = filterList.takeIf { it.isNotEmpty() }
                    ?.let { stack.mallocPointer(filterList.size) }
                    ?.also { filter -> filterList.forEach { filter.put(stack.UTF8("*.$it")) } }
//                println(filterList)
                println(filter)
                val filterDescription = (if (filterList.isEmpty()) "*.*" else filterList.asSequence().map { "*.$it" }
                    .joinToString(", ")).let { "Files ($it)" }
                val result = if (type == Type.Load) {
                    TinyFileDialogs.tinyfd_openFileDialog("Pick a file", path, filter, filterDescription, false)
                } else {
                    TinyFileDialogs.tinyfd_saveFileDialog("Pick a file", path, filter, filterDescription)
                }
                onCloseRequest(result?.let { File(it) })
            }
        }

        onDispose {
            job.cancel()
        }
    }
}

private fun inWindows() = System.getProperty("os.name").lowercase().contains("win")

private enum class Type {
    Load, Save
}
