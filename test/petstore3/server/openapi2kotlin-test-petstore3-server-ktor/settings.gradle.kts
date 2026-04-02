rootProject.name = "openapi2kotlin-test-petstore3-server-ktor"

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
