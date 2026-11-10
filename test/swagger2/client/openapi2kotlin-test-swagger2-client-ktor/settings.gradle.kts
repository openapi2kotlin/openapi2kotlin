rootProject.name = "openapi2kotlin-test-swagger2-client-ktor"

pluginManagement {
    includeBuild("../../../../")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

includeBuild("../../../../")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
