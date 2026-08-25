import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.chenfa.openclashinstaller"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chenfa.openclashinstaller"
        minSdk = 28
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.0"
    }

    // 签名配置：从环境变量读 keystore（CI 通过 GitHub Secrets 注入）
    // 没配置则兜底用 debug 签名，保证 CI 永不因签名缺失失败
    val keystoreFile = System.getenv("SIGNING_KEYSTORE_FILE")
    val keystorePass = System.getenv("SIGNING_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("SIGNING_KEY_ALIAS")
    val keyPass = System.getenv("SIGNING_KEY_PASSWORD")
    val hasKeystore = !keystoreFile.isNullOrBlank() && File(keystoreFile).isFile

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = File(keystoreFile!!)
                storePassword = keystorePass
                this.keyAlias = keyAlias
                keyPassword = keyPass
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 配了 keystore 用正式签名，否则兜底 debug 让 CI 不挂
            signingConfig = if (hasKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.jsch.mwiede)
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.timber)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
