import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// 서버 주소는 local.properties(버전관리 제외)의 API_BASE_URL. 없으면 에뮬레이터에서 본 내 컴퓨터(10.0.2.2).
// 빌드할 때 -PAPI_BASE_URL=...로 덮어쓸 수 있다(배포 서버를 쓰다가 시연용 재생 서버로 잠깐 바꿀 때).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.nowstation.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    // 정식 빌드 서명(업로드 키). local.properties에 RELEASE_* 가 있을 때만(없으면 서명 안 된 release가 나온다)
    val releaseStoreFile = localProperties.getProperty("RELEASE_STORE_FILE")
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "com.taektoy.nowstation" // 스토어 이름. 코드 패키지(namespace)와 따로: com.nowstation.app은 다른 개발자가 선점
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("API_BASE_URL").orNull ?: localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:8080/")}\"")
    }

    buildTypes {
        release {
            // R8: 쓰지 않는 코드·리소스를 빼고 이름을 줄인다. 직렬화·Retrofit·Hilt 규칙은 각 라이브러리에 들어 있다
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("dagger.fullBindingGraphValidation", "ERROR")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// 정식 빌드가 http 서버를 보면 막는다(release는 http를 허용하지 않아 앱이 서버에 닿지 못한다)
val releaseApiUrl: String = providers.gradleProperty("API_BASE_URL").orNull ?: localProperties.getProperty("API_BASE_URL", "")
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val url = releaseApiUrl
    doFirst {
        check(url.startsWith("https://")) { "정식 빌드의 API_BASE_URL은 https여야 한다(지금: '$url'). local.properties를 확인" }
    }
}
