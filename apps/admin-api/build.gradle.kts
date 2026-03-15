plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:identity-access"))
    implementation(project(":modules:organization-directory"))
    implementation(project(":modules:chat-runtime"))
    implementation(project(":modules:document-registry"))
    implementation(project(":modules:ingestion-ops"))
    implementation(project(":modules:qa-review"))
    implementation(project(":modules:metrics-reporting"))

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    jvmToolchain(21)
}

