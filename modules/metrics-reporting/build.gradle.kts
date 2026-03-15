plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:chat-runtime"))
    implementation(project(":modules:qa-review"))
}

kotlin {
    jvmToolchain(21)
}

