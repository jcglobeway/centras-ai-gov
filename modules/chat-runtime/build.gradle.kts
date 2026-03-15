plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:organization-directory"))
    implementation(project(":modules:document-registry"))
}

kotlin {
    jvmToolchain(21)
}

