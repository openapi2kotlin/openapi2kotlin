plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(project(":configuration"))
}

gradlePlugin {
    plugins {
        create("openApi2KotlinPlugin") {
            id = "dev.openapi2kotlin"
            implementationClass = "dev.openapi2kotlin.gradleplugin.OpenApi2KotlinPlugin"
            displayName = "OpenApi2Kotlin Gradle Plugin"
            description = "OpenAPI → Kotlin generator engineered for complex polymorphic schemas with full oneOf & allOf support."
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenOpenApi2KotlinPlugin") {
            from(components["java"])
            artifactId = "openapi2kotlin-gradle-plugin"
            // group + version inherited from root
        }
    }
    repositories {
        mavenLocal()
    }
}