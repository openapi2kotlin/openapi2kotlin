rootProject.name = "openapi2kotlin-test-swagger2-server-spring"

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
