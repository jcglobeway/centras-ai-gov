plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modules:shared-kernel"))
}

kotlin {
    jvmToolchain(21)
}

