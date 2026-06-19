// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register<Copy>("copyApkToDownloadFolder") {
  from(file("${rootDir}/.build-outputs/app-debug.apk"))
  into(file("${rootDir}/APK_DOWNLOAD"))
}



