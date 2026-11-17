plugins {
    id("adengine.java-conventions")
}

description = "Synthetic demo tenant and traffic generator."

dependencies {
    implementation(project(":app"))
}
