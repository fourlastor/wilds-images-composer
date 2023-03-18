import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress(
    // known false positive: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.gradle.download)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.spotless)
}

group = "io.github.fourlastor"
version = "1.0-SNAPSHOT"
val currentOs: OperatingSystem = OperatingSystem.current()

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
    kotlinOptions.jvmTarget = "17"
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

val downloadJdk = tasks.create<Download>("downloadJdk") {
    val fileName =
        if (currentOs.isWindows) "jbrsdk-17.0.6-windows-x64-b829.5.tar.gz" else "jbrsdk-17.0.6-linux-x64-b829.5.tar.gz"
    src("https://cache-redirector.jetbrains.com/intellij-jbr/$fileName")
    dest(buildDir.resolve(fileName))
}

val unzipJdk = tasks.create<Copy>("unzipJdk") {
    dependsOn(downloadJdk)
    val jdkDir = downloadJdk.dest
    from(tarTree(jdkDir)) {
        val includePath = jdkDir.name.removeSuffix(".tar.gz")
        include("$includePath/**")
        includeEmptyDirs = false
        eachFile {
            this.relativePath = RelativePath(!isDirectory, *relativePath.segments.drop(1).toTypedArray())
        }
    }
    into(rootProject.file("jdk"))
}

compose.desktop {
    application {
        javaHome = rootDir.resolve("jdk").absolutePath
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "wilds-image-composer"
            packageVersion = "1.0.0"
        }
    }
}
