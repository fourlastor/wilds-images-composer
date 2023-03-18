package io.github.fourlastor.composer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.nfd.NativeFileDialog
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
fun PickFolderDialog(
    onCloseRequest: (result: File?) -> Unit,
    initialPath: String? = null,
) {
    FileDialog(
        type = Type.PickFolder,
        onCloseRequest = onCloseRequest,
        initialPath = initialPath,
    )
}

@Composable
private fun FileDialog(
    type: Type,
    initialPath: String?,
    filterList: List<String> = emptyList(),
    onCloseRequest: (result: File?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        val job = scope.launch {
            val path = (initialPath ?: System.getProperty("user.home"))
                .let {
                    if (inWindows()) {
                        it.replace("/", "\\")
                    } else {
                        it
                    }
                }

            val pathPointer = MemoryUtil.memAllocPointer(1)

            try {
                val status = when (type) {
                    Type.Load -> NativeFileDialog.NFD_OpenDialog(filterList.joinToString(","), path, pathPointer)
                    Type.Save -> NativeFileDialog.NFD_SaveDialog(filterList.joinToString(","), path, pathPointer)
                    Type.PickFolder -> NativeFileDialog.NFD_PickFolder(path, pathPointer)
                }


                if (status == NativeFileDialog.NFD_CANCEL) {
                    onCloseRequest(null)
                    return@launch
                }

                if (status != NativeFileDialog.NFD_OKAY) {
                    println("Error with native dialog")
                    onCloseRequest(null)
                    return@launch
                }

                val result = pathPointer.getStringUTF8(0)
                NativeFileDialog.nNFD_Free(pathPointer.get(0))
                onCloseRequest(File(result))
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                MemoryUtil.memFree(pathPointer)
            }
        }

        onDispose {
            job.cancel()
        }
    }
}

private fun inWindows() = System.getProperty("os.name").lowercase().contains("win")

private enum class Type {
    Load, Save, PickFolder
}
