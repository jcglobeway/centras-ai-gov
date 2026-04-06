plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
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
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

kotlin {
    jvmToolchain(25)
}

tasks.withType<Test> {
    // Colima 환경에서 Testcontainers Ryuk이 소켓을 마운트할 수 없으므로 비활성화
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
    environment("DOCKER_HOST", System.getenv("DOCKER_HOST") ?: "unix:///Users/parkseokje/.colima/default/docker.sock")
}
