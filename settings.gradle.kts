rootProject.name = "openapi2kotlin"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("gradle-plugin")

include("configuration")

include("application:application-usecase")
include("application:application-core")

include("adapters:parse-spec-adapter")
include("adapters:generate-server-adapter")
include("adapters:generate-client-adapter")
include("adapters:generate-model-adapter")