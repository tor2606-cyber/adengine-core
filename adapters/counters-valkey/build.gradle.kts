plugins {
    id("adengine.java-conventions")
}

description = "CounterPort implementation using Valkey (Lettuce + Lua)."

dependencies {
    implementation(project(":application"))
}
