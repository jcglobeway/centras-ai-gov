plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:chat-runtime"))

    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.springframework.data:spring-data-jpa:3.2.0")
    implementation("org.springframework:spring-context:6.2.7")
    implementation("org.springframework:spring-tx:6.2.7")
    implementation("org.springframework:spring-web:6.2.7")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}
