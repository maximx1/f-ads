import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(project(":ui"))
    implementation(compose.desktop.currentOs)
    // Gives coroutines a main thread to run on, which is Swing's on the desktop.
    implementation(libs.kotlinx.coroutines.swing)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(compose.desktop.uiTestJUnit4)
}

compose.desktop {
    application {
        mainClass = "com.maximx1.fads.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Fuck Ads"
            // Installers want a version of 1.0.0 or more, so this cannot follow the Android app's 0.1.0.
            packageVersion = "1.0.0"
            description = "FreeCell with no ads. Ever."
            licenseFile.set(rootProject.file("LICENSE"))
        }
    }
}
