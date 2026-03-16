plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:organization-directory"))
    implementation(project(":modules:document-registry"))

    // JPA
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.springframework.data:spring-data-jpa:3.2.0")
    implementation("org.springframework:spring-context:6.2.7")
    implementation("org.springframework:spring-tx:6.2.7")
}

kotlin {
    jvmToolchain(25)
}

