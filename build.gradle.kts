// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        compilerOptions {
            val enableMetricsProvider =
                project.providers.gradleProperty("enableComposeCompilerMetrics")
            val relativePath = projectDir.relativeTo(rootDir)
            val buildDir = layout.buildDirectory.get().asFile
            val enableMetrics = (enableMetricsProvider.orNull == "true")
            if (enableMetrics) {
                val metricsFolder = buildDir.resolve("compose-metrics").resolve(relativePath)
                compilerOptions.freeCompilerArgs.addAll(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" + metricsFolder.absolutePath
                )
            }

            val enableReportsProvider =
                project.providers.gradleProperty("enableComposeCompilerReports")
            val enableReports = (enableReportsProvider.orNull == "true")
            if (enableReports) {
                val reportsFolder = buildDir.resolve("compose-reports").resolve(relativePath)
                compilerOptions.freeCompilerArgs.addAll(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + reportsFolder.absolutePath
                )
            }
        }
    }
}