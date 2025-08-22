import groovy.json.JsonSlurper
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("jacoco")
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
    // Firebase plugins
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.multisensor.recording"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.multisensor.recording"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-beta"
        testInstrumentationRunner = "com.multisensor.recording.CustomTestRunner"
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
        }
    }

    packaging {
        resources {
            pickFirsts.add("META-INF/LICENSE.md")
            pickFirsts.add("META-INF/LICENSE-notice.md")
            excludes.add("META-INF/kotlinx-coroutines-core.kotlin_module")
        }
        jniLibs {
            useLegacyPackaging = false
            pickFirsts.addAll(listOf(
                "lib/arm64-v8a/libUSBUVCCamera.so", "lib/arm64-v8a/libencrypt.so",
                "lib/arm64-v8a/libusbcamera.so", "lib/arm64-v8a/libircmd.so",
                "lib/arm64-v8a/libirparse.so", "lib/arm64-v8a/libirprocess.so",
                "lib/arm64-v8a/libirtemp.so", "lib/arm64-v8a/libomp.so",
                "lib/arm64-v8a/libopencv_java4.so",
                "lib/armeabi-v7a/libUSBUVCCamera.so", "lib/armeabi-v7a/libencrypt.so",
                "lib/armeabi-v7a/libusbcamera.so", "lib/armeabi-v7a/libircmd.so",
                "lib/armeabi-v7a/libirparse.so", "lib/armeabi-v7a/libirprocess.so",
                "lib/armeabi-v7a/libirtemp.so", "lib/armeabi-v7a/libomp.so",
                "lib/armeabi-v7a/libopencv_java4.so",
                "lib/x86/libUSBUVCCamera.so", "lib/x86/libencrypt.so",
                "lib/x86/libusbcamera.so", "lib/x86/libircmd.so",
                "lib/x86/libirparse.so", "lib/x86/libirprocess.so",
                "lib/x86/libirtemp.so", "lib/x86/libomp.so",
                "lib/x86/libopencv_java4.so",
                "lib/x86_64/libUSBUVCCamera.so", "lib/x86_64/libencrypt.so",
                "lib/x86_64/libusbcamera.so", "lib/x86_64/libircmd.so",
                "lib/x86_64/libirparse.so", "lib/x86_64/libirprocess.so",
                "lib/x86_64/libirtemp.so", "lib/x86_64/libomp.so",
                "lib/x86_64/libopencv_java4.so"
            ))
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")
            buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "BUILD_TYPE", "\"release\"")
            buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
        create("staging") {
            initWith(getByName("debug"))
            isDebuggable = false
            buildConfigField("String", "BUILD_TYPE", "\"staging\"")
            buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")
        }
    }

    flavorDimensions.add("environment")
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "ENVIRONMENT", "\"development\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-Xjsr305=strict"
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    testOptions {
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.jvmArgs(
                    "-XX:MaxMetaspaceSize=2048m",
                    "-Djava.awt.headless=true",
                    "-Dfile.encoding=UTF-8",
                    "--add-opens=java.base/java.lang=ALL-UNNAMED"
                )
                it.maxHeapSize = "2048m"
                it.useJUnitPlatform {
                    includeEngines("junit-jupiter", "junit-vintage", "kotest")
                }
                it.systemProperty("robolectric.useWindowsCompatibleTempDir", "true")
                it.reports.html.required.set(true)
                it.reports.junitXml.required.set(true)
            }
        }
        managedDevices {
            // Updated to use allDevices instead of deprecated devices API
            allDevices {
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel2api35") {
                    device = "Pixel 2"
                    apiLevel = 35
                    systemImageSource = "google"
                }
            }
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("${layout.buildDirectory.get()}/generated/source/config")
        }
    }
}

dependencies {

    // Firebase BOM - manages all Firebase library versions
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.ui)

    // Debug tooling for Compose
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.bundles.core.ui)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.material)
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    implementation(libs.bundles.lifecycle)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.bundles.activity.fragment)
    implementation(libs.xxpermissions)

    implementation(libs.bundles.camera)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    implementation(libs.bundles.networking)

    // Charting library for real-time data visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Security dependencies - Updated from alpha to stable versions
    // Addresses Low Priority recommendation: "Evaluate alpha/beta dependency risk"
    implementation("androidx.security:security-crypto:1.1.0-alpha06")  // Restored higher version for MasterKey support
    // Removed security-identity-credential as it's still alpha and not critical for core functionality

    implementation(files("src/main/libs/shimmerandroidinstrumentdriver-3.2.3_beta.aar"))
    implementation(files("src/main/libs/shimmerbluetoothmanager-0.11.4_beta.jar"))
    implementation(files("src/main/libs/shimmerdriver-0.11.4_beta.jar"))
    implementation(files("src/main/libs/shimmerdriverpc-0.11.4_beta.jar"))
    implementation(files("src/main/libs/topdon_1.3.7.aar"))
    implementation(files("src/main/libs/libusbdualsdk_1.3.4_2406271906_standard.aar"))
    implementation(files("src/main/libs/opengl_1.3.2_standard.aar"))
    implementation(files("src/main/libs/suplib-release.aar"))

    testImplementation(libs.bundles.enhanced.unit.testing)
    testImplementation(libs.hilt.android.testing)
    testImplementation("org.apache.commons:commons-math3:3.6.1")
    kspTest(libs.hilt.compiler)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.bundles.enhanced.integration.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestUtil("androidx.test:orchestrator:1.5.0")
    kspAndroidTest(libs.hilt.compiler)

    implementation("org.opencv:opencv:4.9.0")

    val ktlint by configurations.getting
    ktlint(libs.ktlint)
    detektPlugins(libs.detekt.formatting)
}

val outputDir = file("${layout.buildDirectory.get()}/generated/source/config")
tasks.register("generateConstants") {
    group = "build"
    description = "Generates Kotlin constants from config.json."
    val configFile = file("src/main/assets/config.json")
    val outputFile = file("$outputDir/com/multisensor/recording/config/CommonConstants.kt")
    inputs.file(configFile)
    outputs.file(outputFile)
    doLast {
        val json = JsonSlurper().parse(configFile) as Map<*, *>
        val network = json["network"] as Map<*, *>
        val devices = json["devices"] as Map<*, *>
        val resolution = devices["resolution"] as Map<*, *>
        val calibration = json["calibration"] as Map<*, *>

        outputDir.mkdirs()
        outputFile.writeText(
            """

        package com.multisensor.recording.config
        object CommonConstants {
            const val PROTOCOL_VERSION: Int = ${json["protocol_version"]}
            const val APP_VERSION: String = "${json["version"]}"

            object Network {
                const val HOST: String = "${network["host"]}"
                const val PORT: Int = ${network["port"]}
                const val TIMEOUT_SECONDS: Int = ${network["timeout_seconds"]}
            }

            object Devices {
                const val CAMERA_ID: Int = ${devices["camera_id"]}
                const val FRAME_RATE: Int = ${devices["frame_rate"]}
                const val RESOLUTION_WIDTH: Int = ${resolution["width"]}
                const val RESOLUTION_HEIGHT: Int = ${resolution["height"]}
            }

            object Calibration {
                const val PATTERN_TYPE: String = "${calibration["pattern_type"]}"
                const val PATTERN_ROWS: Int = ${calibration["pattern_rows"]}
                const val PATTERN_COLS: Int = ${calibration["pattern_cols"]}
                const val SQUARE_SIZE_M: Double = ${calibration["square_size_m"]}
            }
        }
        """.trimIndent()
        )
        println("Generated CommonConstants.kt from config.json")
    }
}

tasks.named("generateConstants") {
    notCompatibleWithConfigurationCache("Task uses script object references which are not cache-compatible.")
}

tasks.named("preBuild") {
    dependsOn("generateConstants")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$projectDir/../detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    group = "Reporting"
    description = "Generates Jacoco coverage reports for all variants."
    dependsOn("testDevDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*",
        "**/*_Factory.*", "**/*_MembersInjector.*", "**/*Module*.*",
        "**/databinding/*", "**/generated/**/*.*",
        "**/Hilt_*.*", "**/DaggerHilt*.*", "**/*_HiltModules*.*",
        "**/di/**/*.*"
    )

    val kotlinClasses = fileTree("${layout.buildDirectory.get().asFile}/tmp/kotlin-classes/devDebug") {
        exclude(fileFilter)
    }
    val javaClasses = fileTree("${layout.buildDirectory.get().asFile}/intermediates/javac/devDebug/classes") {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(listOf(kotlinClasses, javaClasses)))
    sourceDirectories.setFrom(files(listOf("$projectDir/src/main/java", "$projectDir/src/main/kotlin")))

    executionData.setFrom(fileTree(layout.buildDirectory.get().asFile) {
        include(listOf(
            "outputs/unit_test_code_coverage/devDebugUnitTest/*.exec",
            "jacoco/testDevDebugUnitTest.exec"
        ))
    })

    doFirst {
        executionData.setFrom(files(executionData.files.filter { it.exists() }))
    }
}

// Task to run tests and generate coverage report in one command
tasks.register("testWithCoverage") {
    group = "verification"
    description = "Runs tests and generates coverage report"
    dependsOn("testDevDebugUnitTest", "jacocoTestReport")
}

tasks.register<JavaExec>("formatKotlin") {
    group = "formatting"
    description = "Format Kotlin code with ktlint."
    val ktlint by configurations.getting
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F", "src/**/*.kt")
}

tasks.register<JavaExec>("lintKotlin") {
    group = "verification"
    description = "Check Kotlin code style with ktlint."
    val ktlint by configurations.getting
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("src/**/*.kt")
}

tasks.named("check") {
    dependsOn("detekt", "lintKotlin")
}

tasks.named("build") {
    finalizedBy("jacocoTestReport")
}

tasks.register("runIDEIntegrationUITest") {
    group = "integration-testing"
    description = "Run IDE integration UI test on connected device"
    dependsOn("assembleDebug", "assembleDebugAndroidTest")

    doLast {
        // Updated to use ExecOperations.exec instead of deprecated exec(Action<ExecSpec>)
        providers.exec {
            commandLine(
                "adb", "shell", "am", "instrument", "-w",
                "-e", "class", "com.multisensor.recording.IDEIntegrationUITest",
                "com.multisensor.recording.test/androidx.test.runner.AndroidJUnitRunner"
            )
        }
    }
}
