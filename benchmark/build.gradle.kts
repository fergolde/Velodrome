plugins {
    id("com.android.application")
}

android {
    namespace = "com.fergolde.velodrome.benchmark"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.fergolde.velodrome.benchmark"
        minSdk = 34
        targetSdk = 37
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.benchmark.junit4)
    implementation(project(":app"))
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}
