plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:chat-runtime"))
}

kotlin {
    jvmToolchain(21)
}

