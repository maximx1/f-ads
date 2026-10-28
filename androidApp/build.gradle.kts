plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.maximx1.fads"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.maximx1.fads"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Robolectric runs the UI tests on this machine, no phone or emulator needed.
            isIncludeAndroidResources = true
            all {
                // Robolectric's Android 17 support reaches into this JDK-internal package.
                it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
                // On the JetBrains runtime that ships with Android Studio, java.io goes through NIO, and a file
                // renamed over another one is missing for a moment. AtomicFile saves by doing just that, so a test
                // reading a save could find nothing. A phone renames in one step, and so does this without NIO.
                it.systemProperty("jbr.java.io.use.nio", "false")
            }
        }
    }
}

dependencies {
    implementation(project(":ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
