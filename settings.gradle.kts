pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Downloads the Java 25 toolchain when it is not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "adengine-core"

// Hexagonal layout (ADR-19). Allowed dependencies: docs/architecture/architecture.md §5.3.
include("domain")
include("application")
include("adapters:persistence")
include("adapters:web-admin")
include("adapters:web-decision")
include("adapters:consent-tcf")
include("adapters:geo-dbip")
include("adapters:device-uap")
include("adapters:context-rules")
include("adapters:counters-valkey")
include("adapters:events-kafka")
include("adapters:demand-adsense")
include("app")
include("simulator")
include("demo")

// Front end, built by Node tooling and wired into Gradle in M0-08 (admin-ui) and M0-09 (tag).
include("admin-ui")
include("tag")
