plugins {
    id("adengine.java-conventions")
}

description = "Traffic and pacing simulator running the real engine with a fake clock."

dependencies {
    implementation(project(":application"))
}
