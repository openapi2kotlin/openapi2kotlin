plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":application:application-core"))
    implementation(libs.kotlin.logging)
    implementation(libs.swagger.parser)
    testImplementation(libs.kotlin.test.junit)
}
