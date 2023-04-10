package io.github.fourlastor.composer

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.essenty.parcelable.Parcelable
import io.github.fourlastor.composer.conversion.ConversionComponent
import io.github.fourlastor.composer.pick.PickImageComponent
import io.github.fourlastor.composer.preview.PreviewComponent
import java.io.File

class NavHostComponent(
    componentContext: ComponentContext,
) : Component, ComponentContext by componentContext {
    private val navigation = StackNavigation<ScreenConfig>()
    private val stack = childStack(
        source = navigation,
        childFactory = ::createScreenComponent,
        initialConfiguration = ScreenConfig.PickFiles,
    )

    private fun createScreenComponent(
        screenConfig: ScreenConfig,
        context: ComponentContext,
    ): Component = when (screenConfig) {
        is ScreenConfig.PickFiles -> PickImageComponent(
            context = context,
            goToConversion = ::goToConversion
        )

        is ScreenConfig.Preview -> PreviewComponent(
            context,
            screenConfig.conversion,
            ::goToPickFiles
        )

        is ScreenConfig.Conversion -> ConversionComponent(
            context = context,
            goToPreview = ::goToPreview,
            front = screenConfig.front,
            back = screenConfig.back,
            shiny = screenConfig.shiny,
            overworld = screenConfig.overworld,
            overworldshiny = screenConfig.overworldshiny,
        )
    }

    private fun goToPickFiles() {
        navigation.replaceCurrent(ScreenConfig.PickFiles)
    }

    private fun goToConversion(front: File, back: File, shiny: File, overworld: File, overworldshiny: File) {
        navigation.replaceCurrent(ScreenConfig.Conversion(front, back, shiny, overworld, overworldshiny))
    }


    private fun goToPreview(
        conversion: CompleteConversion,
    ) {
        navigation.replaceCurrent(
            ScreenConfig.Preview(conversion)
        )
    }


    @Composable
    override fun render() {
        Children(
            stack = stack,
        ) {
            it.instance.render()
        }
    }

    private sealed class ScreenConfig : Parcelable {
        object PickFiles : ScreenConfig()
        data class Preview(
            val conversion: CompleteConversion,
        ) : ScreenConfig()

        data class Conversion(
            val front: File,
            val back: File,
            val shiny: File,
            val overworld: File,
            val overworldshiny: File,
        ) : ScreenConfig()
    }
}
