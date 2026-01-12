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

include("adapters:adapter-tools")
include("adapters:parse-spec-adapter")
include("adapters:generate-server-ktor-adapter")
include("adapters:generate-server-spring-adapter")
include("adapters:generate-client-adapter")
include("adapters:generate-model-adapter")