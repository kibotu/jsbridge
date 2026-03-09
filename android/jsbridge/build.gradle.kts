plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "net.kibotu.jsbridge"
    compileSdk = libs.versions.compileSdk.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xexplicit-backing-fields",
        )
    }
}

val isJitpack = System.getenv("JITPACK") == "true"

if (!isJitpack) {
    mavenPublishing {
        publishToMavenCentral()
        signAllPublications()
    }
}

if (isJitpack) {
    apply(plugin = "maven-publish")
    afterEvaluate {
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("release") {
                    from(components["release"])
                    groupId = property("GROUP") as String
                    artifactId = property("POM_ARTIFACT_ID") as String
                    version = version
                }
            }
        }
    }
}

dependencies {
    implementation(libs.timber)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.webkit)
    compileOnly(libs.tink.android)
    compileOnly(libs.androidx.security.crypto)
}
