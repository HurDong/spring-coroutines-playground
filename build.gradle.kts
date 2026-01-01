import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test

plugins {
    id("org.springframework.boot") version "3.2.1" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.22" apply false
}

subprojects {
    group = "com.example.performance"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    // In subprojects block, accessors like 'java' or 'dependencies' are not available directly.
    // We need to use configure<JavaPluginExtension> or standard Gradle API.
    
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "implementation"("io.micrometer:micrometer-registry-prometheus")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
