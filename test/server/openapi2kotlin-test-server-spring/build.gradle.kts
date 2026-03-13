import java.util.Properties

fun readRepoJvmVersion(): Int =
    generateSequence(projectDir) { it.parentFile }
        .map { it.resolve("gradle.properties") }
        .filter { it.isFile }
        .mapNotNull { propertiesFile ->
            Properties().apply {
                propertiesFile.inputStream().use(::load)
            }.getProperty("openapi2kotlin.jvm")?.toInt()
        }
        .firstOrNull()
        ?: error("Could not locate openapi2kotlin.jvm in repository gradle.properties from $projectDir")

val repoJvmVersion = readRepoJvmVersion()

plugins {
    alias(libs.plugins.kotlin.plugin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi2kotlin)
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(repoJvmVersion)
}

sourceSets {
    main {
        kotlin.srcDir(layout.buildDirectory.dir("generated/src/main/kotlin"))
    }
}

tasks.named("compileKotlin") {
    dependsOn("openapi2kotlin")
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.swagger.annotations.jakarta)
    testImplementation(libs.spring.boot.starter.test)
}

openapi2kotlin {
    inputSpec = "$projectDir/src/main/resources/openapi.yaml"
    outputDir = layout.buildDirectory.dir("generated/src/main/kotlin").get().asFile.path

    model {
        packageName = "e2e.server.spring.generated.model"
    }

    server {
        packageName = "e2e.server.spring.generated.server"
        library = Spring
    }
}
