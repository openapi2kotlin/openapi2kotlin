plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":application:application-core"))
    implementation(project(":tools:api-generator"))
    implementation(project(":tools:generator-tools"))
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinpoet)
}
