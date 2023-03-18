import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress(
    // known false positive: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.spotless)
}

group = "io.github.fourlastor"
version = "1.0-SNAPSHOT"

spotless {
    isEnforceCheck = false
    kotlin {
        ktfmt()
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation(compose.desktop.common)
    runtimeOnly(compose.desktop.linux_x64)
    runtimeOnly(compose.desktop.macos_x64)
    runtimeOnly(compose.desktop.macos_arm64)
    runtimeOnly(compose.desktop.windows_x64)
    implementation(libs.decompose.core)
    implementation(libs.decompose.jetbrains)
    implementation(libs.korge.korim)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.lwjgl.core)
    implementation(libs.lwjgl.nfd)
    for (it in listOf("linux", "macos", "macos-arm64", "windows")) {
        natives(libs.lwjgl.core, it)
        natives(libs.lwjgl.nfd, it)
    }
}

fun DependencyHandlerScope.natives(
    provider: Provider<MinimalExternalModuleDependency>,
    classifier: String,
) = runtimeOnly(variantOf(provider) { classifier("natives-$classifier") })

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "wilds-image-composer"
            packageVersion = "1.0.0"
        }
    }
}
