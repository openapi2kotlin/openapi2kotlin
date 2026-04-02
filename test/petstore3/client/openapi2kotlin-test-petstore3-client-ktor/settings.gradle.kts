rootProject.name = "openapi2kotlin-test-petstore3-client-ktor"

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
