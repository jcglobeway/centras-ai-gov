plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:document-registry"))
    implementation(project(":modules:organization-directory"))
}

kotlin {
    jvmToolchain(21)
}

