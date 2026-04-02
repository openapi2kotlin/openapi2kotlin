rootProject.name = "openapi2kotlin-test-petstore3-client-restclient"

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
