@Suppress(
    // known false positive: https://youtrack.jetbrains.com/issue/KTIJ-19369
    "DSL_SCOPE_VIOLATION"
)
plugins {
    java
    alias(libs.plugins.spotless)
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

spotless {
    isEnforceCheck = false
    java {
        palantirJavaFormat()
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(libs.gdx.core)
    nativesDesktop(libs.gdx.platform)
}

fun DependencyHandlerScope.nativesDesktop(
    provider: Provider<MinimalExternalModuleDependency>,
) = runtimeOnly(variantOf(provider) { classifier("natives-desktop") })
