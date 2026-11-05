plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// ---- امضای Release از متغیرهای محیطی (GitHub Secrets) ----
val releaseKeystorePath: String? = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val releaseStorePassword: String? = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val releaseKeyAlias: String? = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
// رمز کلید خالی باشد، همان رمز keystore استفاده می‌شود (حالت پیش‌فرض keystoreهای PKCS12)
val releaseKeyPassword: String? = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: releaseStorePassword
val hasReleaseKey: Boolean =
    releaseKeystorePath != null && releaseStorePassword != null && releaseKeyAlias != null &&
        file(releaseKeystorePath).let { it.isFile && it.length() > 0 }

android {
    namespace = "app.yadavar"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.yadavar"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        // فقط وقتی کلید Release کامل و معتبر در دسترس باشد ساخته می‌شود (CI از Secretها پرش می‌کند).
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // بدون کلید Release، با کلید دیباگ امضا می‌شود تا APK قابل نصب بماند (برای انتشار عمومی کلید شخصی لازم است).
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    val room = "2.6.1"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
