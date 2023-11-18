plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "IntelliJ Platform Plugin Template"
include("src:main:java")
findProject(":src:main:java")?.name = "java"
