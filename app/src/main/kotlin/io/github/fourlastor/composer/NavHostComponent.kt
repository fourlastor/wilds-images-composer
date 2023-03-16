package io.github.fourlastor.composer

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.push
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
        initialConfiguration = ScreenConfig.PickFiles,
        childFactory = ::createScreenComponent
    )

    private fun createScreenComponent(
        screenConfig: ScreenConfig,
        context: ComponentContext,
    ): Component = when (screenConfig) {
        is ScreenConfig.PickFiles -> PickImageComponent(
            context = context,
            goToConversion = ::goToConversion
        )

        ScreenConfig.Preview -> PreviewComponent(context)
        ScreenConfig.Conversion -> ConversionComponent(context = context, goToPreview = ::goToPreview)
    }

    private fun goToConversion(front: File, back: File, shiny: File) {
        navigation.push(ScreenConfig.Conversion)
    }


    private fun goToPreview() {
        navigation.push(ScreenConfig.Preview)
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
        object Preview : ScreenConfig()
        object Conversion : ScreenConfig()
    }
}
