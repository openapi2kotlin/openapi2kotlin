plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":application:application-core"))
    implementation(project(":adapters:adapter-tools"))
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinpoet)
}