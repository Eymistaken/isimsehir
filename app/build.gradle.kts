import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Kendi imza anahtarını bağlamak için kökte keystore.properties oluştur:
//   storeFile=C:/yol/uploader.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
// Dosya yoksa release, debug anahtarıyla imzalanır (yan yükleme için yeterli,
// Play Store yüklemesi için değil).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null

// Sürüm numarası: temel ad + derleme sayacı. GitHub Actions her çalışmada
// BUILD_NUMBER geçer (run number), yerel derlemede 0 kalır.
val baseVersionName = "2.0"
val buildNumber = (System.getenv("BUILD_NUMBER") ?: "0").toIntOrNull() ?: 0
// Geliştirici sürümlerinde "-dev" gibi bir ek; iş akışı geçiyor. Aynı ek
// Ayarlar'daki geliştirici bölümünü de açıyor: deneysel şeyler yalnızca
// ön sürümlerde görünsün.
val versionSuffix = System.getenv("VERSION_SUFFIX").orEmpty()
val isDevBuild = versionSuffix.contains("dev", ignoreCase = true)
val appVersionName =
    if (buildNumber == 0) baseVersionName else "$baseVersionName.$buildNumber$versionSuffix"
val appVersionCode = 1 + buildNumber

android {
    namespace = "com.eymistaken.isimsehir"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.eymistaken.isimsehir"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Geliştirici bölümü yalnızca -dev sürümlerinde görünsün.
            buildConfigField("boolean", "DEV_BUILD", isDevBuild.toString())
            // R8 kapalı: cihazda çalıştırıp doğrulayamadığımız için ilk sürümde risk alınmıyor.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            // Yerelde derlerken geliştirici bölümü hep açık.
            buildConfigField("boolean", "DEV_BUILD", "true")
            applicationIdSuffix = ".debug"
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
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// CI, release etiketini bu görevden okur — sürüm tek yerde tanımlı kalsın diye.
tasks.register("printVersionName") {
    val version = appVersionName
    doLast { println(version) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.ui.tooling)
}
