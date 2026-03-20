plugins {
    id("org.springframework.boot") version "3.5.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.1.21" apply false
    kotlin("plugin.spring") version "2.1.21" apply false
    kotlin("plugin.jpa") version "2.1.21" apply false
}

allprojects {
    group = "com.publicplatform.ragops"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/release") }
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            "testImplementation"(kotlin("test"))
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

