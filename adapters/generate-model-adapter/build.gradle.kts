plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":application:application-core"))
    implementation(project(":tools:generator-tools"))
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinpoet)
    testImplementation(libs.kotlin.test.junit)
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.17")
}
