plugins {
    id("adengine.java-conventions")
}

description = "EventPublisherPort producer and event consumers for Kafka."

dependencies {
    implementation(project(":application"))
}
