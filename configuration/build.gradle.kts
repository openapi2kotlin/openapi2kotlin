plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":application:application-usecase"))
    implementation(project(":application:application-core"))
    implementation(project(":adapters:parse-spec-adapter"))
    implementation(project(":adapters:generate-server-ktor-adapter"))
    implementation(project(":adapters:generate-server-spring-adapter"))
    implementation(project(":adapters:generate-model-adapter"))
    implementation(project(":adapters:generate-client-adapter"))
}
