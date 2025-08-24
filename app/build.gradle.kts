import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.kotlin.kapt")
}

android.buildFeatures.buildConfig = true

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun String.asBuildConfigString(): String =
    "\"" + this.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.example.myaiapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myaiapp"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            run {
                val fromLocal = localProps.getProperty("OPEN_ROUTER_API_KEY") ?: ""
                val fromGradle = providers.gradleProperty("OPEN_ROUTER_API_KEY").orNull
                val fromEnv = providers.environmentVariable("OPEN_ROUTER_API_KEY").orNull
                //noinspection WrongGradleMethod
                val key = sequenceOf(fromGradle, fromLocal, fromEnv).firstOrNull { !it.isNullOrBlank() } ?: ""
                buildConfigField("String", "OPEN_ROUTER_API_KEY", key.asBuildConfigString())
            }

            // --- НОВОЕ: GitHub PAT для GitHub Actions dispatch ---
            run {
                val fromLocal = localProps.getProperty("GITHUB_PAT") ?: ""
                val fromGradle = providers.gradleProperty("GITHUB_PAT").orNull
                val fromEnv = providers.environmentVariable("GITHUB_PAT").orNull
                //noinspection WrongGradleMethod
                val key = sequenceOf(fromGradle, fromLocal, fromEnv).firstOrNull { !it.isNullOrBlank() } ?: ""
                buildConfigField("String", "GITHUB_PAT", key.asBuildConfigString())
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // На всякий случай в релиз не кладём ничего
            buildConfigField("String", "OPEN_ROUTER_API_KEY", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle)
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.constraintlayout)
    implementation(libs.compose.preview)
    implementation(libs.compose.material)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    //Navigation
    implementation(libs.androidx.navigation)

    //Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    //DI
    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

    //Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)

    //Okhttp3
    implementation(libs.okhttp3.logging.interceptor)

    //Moshi
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    //Accompanist
    implementation(libs.accompanist.systemuicontroller)

    //Coil
    implementation(libs.coil)

    //Kotlinx
    implementation(libs.kotlinx.serialization.json)

    //SSH
    implementation(libs.sshj)

    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.compose.ui.test.manifest)
}