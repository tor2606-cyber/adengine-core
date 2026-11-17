plugins {
    id("adengine.java-conventions")
}

description = "Spring Boot main, wiring and the serve/admin/worker roles."

dependencies {
    implementation(project(":adapters:persistence"))
    implementation(project(":adapters:web-admin"))
    implementation(project(":adapters:web-decision"))
    implementation(project(":adapters:consent-tcf"))
    implementation(project(":adapters:geo-dbip"))
    implementation(project(":adapters:device-uap"))
    implementation(project(":adapters:context-rules"))
    implementation(project(":adapters:counters-valkey"))
    implementation(project(":adapters:events-kafka"))
    implementation(project(":adapters:demand-adsense"))
}
