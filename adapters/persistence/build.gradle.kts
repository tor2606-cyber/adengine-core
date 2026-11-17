plugins {
    id("adengine.java-conventions")
}

description = "jOOQ + Flyway persistence for configuration and events."

dependencies {
    implementation(project(":application"))
}
