plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":application:application-core"))
    implementation(project(":tools:generator-tools"))
    implementation(project(":tools:api-generator"))
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinpoet)
}