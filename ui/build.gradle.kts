plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm()

    android {
        namespace = "com.maximx1.fads.ui"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    sourceSets {
        commonMain.dependencies {
            // What the apps need to call into this module is passed on to them.
            api(project(":core"))
            api(libs.compose.mp.runtime)
            api(libs.compose.mp.foundation)
            api(libs.compose.mp.ui)
            api(libs.compose.mp.material3)
            api(libs.lifecycle.mp.viewmodel)
            implementation(libs.lifecycle.mp.runtime.compose)
            // The system back button and gesture, on every platform that has one.
            implementation(libs.compose.mp.ui.backhandler)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
