import com.android.build.api.variant.BuildConfigField
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

class ReleaseApiBaseUrlField : Transformer<BuildConfigField<String>, String>, java.io.Serializable {
    override fun transform(configuredUrl: String): BuildConfigField<String> {
        val url = normalizeApiBaseUrl(configuredUrl, "wattVisionReleaseApiUrl", httpsRequired = true)
        return BuildConfigField("String", "\"$url\"", null)
    }

    companion object {
        fun normalizeApiBaseUrl(configuredUrl: String, propertyName: String, httpsRequired: Boolean): String {
            val url = configuredUrl.trim()
            if (url.isEmpty() && !httpsRequired) return ""

            val uri = runCatching { URI(url) }.getOrNull()
            val validScheme = uri?.scheme == "https" || (!httpsRequired && uri?.scheme == "http")
            val allowedSchemes = if (httpsRequired) "HTTPS" else "HTTP ou HTTPS"
            require(
                uri != null && validScheme && uri.host != null &&
                    (uri.port == -1 || uri.port in 1..65535) &&
                    uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null
            ) {
                "Configure $propertyName com uma URL absoluta $allowedSchemes, sem credenciais, query ou fragmento."
            }

            return uri.toASCIIString().let { if (it.endsWith('/')) it else "$it/" }
        }
    }
}

val debugApiUrl = ReleaseApiBaseUrlField.normalizeApiBaseUrl(
    providers.gradleProperty("wattVisionDebugApiUrl").getOrElse(""),
    "wattVisionDebugApiUrl",
    httpsRequired = false
)
val releaseApiField = providers.gradleProperty("wattVisionReleaseApiUrl")
    .orElse("")
    .map(ReleaseApiBaseUrlField())

android {
    namespace = "com.example.watt_vision_mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.watt_vision_mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["apiCleartextTraffic"] = "false"
        manifestPlaceholders["apiNetworkSecurityConfig"] = "@xml/network_security_config"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
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
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        requireNotNull(variant.buildConfigFields).put(
            "API_BASE_URL", BuildConfigField("String", "\"$debugApiUrl\"", null)
        )
        val localSeriesEnabled = providers.gradleProperty("wattVisionDebugLocalSeries").orNull == "true"
        requireNotNull(variant.buildConfigFields).put(
            "LOCAL_SERIES_ENABLED", BuildConfigField("boolean", localSeriesEnabled.toString(), null)
        )
        if (debugApiUrl.startsWith("http://")) {
            variant.manifestPlaceholders.put("apiCleartextTraffic", "true")
            variant.manifestPlaceholders.put("apiNetworkSecurityConfig", "@xml/network_security_config_debug")
        }
    }
    onVariants(selector().withBuildType("release")) { variant ->
        requireNotNull(variant.buildConfigFields).put("API_BASE_URL", releaseApiField)
        requireNotNull(variant.buildConfigFields).put(
            "LOCAL_SERIES_ENABLED", BuildConfigField("boolean", "false", null)
        )
    }
}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}