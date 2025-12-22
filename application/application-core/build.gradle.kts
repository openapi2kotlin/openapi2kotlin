plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":application:application-usecase"))
    implementation(libs.kotlin.logging)
    testImplementation(libs.kotlin.test.junit)
}
