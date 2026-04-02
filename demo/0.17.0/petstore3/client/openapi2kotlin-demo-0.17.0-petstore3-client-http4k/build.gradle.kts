plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.openapi2kotlin)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.bundles.http4k)
    testImplementation(libs.bundles.test)
}

tasks.test {
    useJUnitPlatform()
}

openapi2kotlin {
    inputSpec = "$projectDir/src/main/resources/openapi.yaml"
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "dev.openapi2kotlin.demo.model"
        serialization = KotlinX
    }

    client {
        packageName = "dev.openapi2kotlin.demo.client"
        library = Http4k
    }
}
