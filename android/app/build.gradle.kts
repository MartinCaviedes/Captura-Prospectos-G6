import java.net.URI
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// URL pública del gateway, sin credenciales. Permite USB/adb reverse durante pruebas físicas.
val gatewayBaseUrl = providers.gradleProperty("g6ApiBaseUrl").orElse("http://10.0.2.2:8080/").get()
val gatewayUri = URI(gatewayBaseUrl)
require(gatewayUri.host != null && gatewayUri.rawUserInfo == null && gatewayUri.rawQuery == null &&
    gatewayUri.rawFragment == null && gatewayUri.rawPath in listOf("", "/") &&
    (gatewayUri.scheme == "https" || (gatewayUri.scheme == "http" && gatewayUri.host in listOf("127.0.0.1", "localhost", "10.0.2.2")))) {
    "g6ApiBaseUrl debe ser HTTPS o HTTP de desarrollo local, sin credenciales, query ni fragmento"
}

android {
    namespace = "io.github.grupo6.capturaprospectos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.grupo6.capturaprospectos"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        if (providers.gradleProperty("g6AndroidCoverage").map(String::toBoolean).orElse(false).get()) {
            testInstrumentationRunnerArguments["notAnnotation"] = "io.github.grupo6.capturaprospectos.testing.RequiresExternalFixture"
        }
        buildConfigField("String", "API_BASE_URL", "\"$gatewayBaseUrl\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("demo") {
            dimension = "environment"
            applicationIdSuffix = ".demo"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = providers.gradleProperty("g6AndroidCoverage").map(String::toBoolean).orElse(false).get()
        }
        release {
            optimization {
                enable = false
            }
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
    testCoverage { jacocoVersion = "0.8.16" }
}

room { schemaDirectory("$projectDir/schemas") }

// AGP 9.4: informe conjunto sobre las clases originales, no las instrumentadas.
tasks.register<JacocoReport>("jacocoDemoDebugCombinedReport") {
    dependsOn("testDemoDebugUnitTest", "createDemoDebugAndroidTestCoverageReport")
    executionData.from(fileTree(layout.buildDirectory) {
        include("outputs/unit_test_code_coverage/demoDebugUnitTest/*.exec")
        include("outputs/code_coverage/demoDebugAndroidTest/connected/**/*.ec")
    })
    classDirectories.from(
        layout.buildDirectory.dir("intermediates/built_in_kotlinc/demoDebug/compileDemoDebugKotlin/classes"),
        layout.buildDirectory.dir("intermediates/javac/demoDebug/compileDemoDebugJavaWithJavac/classes"),
    )
    sourceDirectories.from("src/main/java")
    reports {
        html.required = true
        xml.required = true
        csv.required = true
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    // Modelo incluido en la APK: lectura QR disponible sin descarga ni backend.
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.sqlcipher)
    implementation(libs.sqlite)
    implementation(libs.coroutines.core)
    ksp(libs.room.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
