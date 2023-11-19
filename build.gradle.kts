import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.io.FileInputStream
import java.util.*

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    id("codegpt.java-conventions")
    id("org.jetbrains.changelog") version "2.2.0"

    id(libs.plugins.kotlin.get().pluginId) version '1.6.10' // Kotlin support
    id(libs.plugins.gradleIntelliJPlugin.get().pluginId) // Gradle IntelliJ Plugin
//    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    id(libs.plugins.qodana.get().pluginId) // Gradle Qodana Plugin
    id(libs.plugins.kover.get().pluginId) // Gradle Kover Plugin
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()
    gradlePluginPortal()
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
//    implementation(libs.annotations)
  implementation(project(":codegpt-core"))
  implementation(project(":codegpt-telemetry"))

  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.16.0")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")
  implementation("com.vladsch.flexmark:flexmark-all:0.64.8") {
    // vulnerable transitive dependency
    exclude(group = "org.jsoup", module = "jsoup")
  }
  implementation("org.jsoup:jsoup:1.16.2")
  implementation("org.apache.commons:commons-text:1.11.0")
  implementation("com.knuddels:jtokkit:0.6.1")
  implementation("org.quartz-scheduler:quartz:2.3.2")

  testImplementation("org.assertj:assertj-core:3.24.2")
  testImplementation("org.awaitility:awaitility:4.2.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.1")
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    @Suppress("UnstableApiUsage")
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.JETBRAINS
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath = provider { file(".qodana").canonicalPath }
    reportPath = provider { file("build/reports/inspections").canonicalPath }
    saveReport = true
    showReport = environment("QODANA_SHOW_REPORT").map { it.toBoolean() }.getOrElse(false)
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }
    
  verifyPlugin {
    enabled = true
  }

  runPluginVerifier {
    enabled = true
  }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with (it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }
    
  buildPlugin {
    enabled = true
  }

    publishPlugin {
        dependsOn("patchChangelog")
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = properties("pluginVersion").map { listOf(it.split('-').getOrElse(1) { "default" }.split('.').first()) }
    }
    
  runIde {
    enabled = true
    environment("ENVIRONMENT", "LOCAL")
  }

  test {
    exclude("**/testsupport/*")
    useJUnitPlatform()
    testLogging {
      events("started", "passed", "skipped", "failed")
      exceptionFormat = TestExceptionFormat.FULL
      showStandardStreams = true
    }
  }
}
