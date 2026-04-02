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

include("tools:generator-tools")
include("tools:api-generator")
include("tools:detekt-tools")

include("adapters:parse-spec-adapter")

include("adapters:generate-server-ktor-adapter")
include("adapters:generate-server-spring-adapter")

include("adapters:generate-client-ktor-adapter")
include("adapters:generate-client-http4k-adapter")
include("adapters:generate-client-restclient-adapter")

include("adapters:generate-model-adapter")
include("adapters:generate-server-http4k-adapter")
