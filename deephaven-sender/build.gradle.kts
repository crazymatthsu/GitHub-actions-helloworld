plugins {
    java
    application
    alias(libs.plugins.dependency.management)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}")
    }
}

dependencies {
    implementation(libs.deephaven.java.client.flight.dagger)
    implementation(libs.deephaven.qst)
    implementation(libs.jackson.databind)
    testImplementation(libs.spring.boot.starter.test)
}

application {
    mainClass.set("com.example.deephaven.PublishEquityOrders")
    applicationDefaultJvmArgs = listOf("--add-opens=java.base/java.nio=ALL-UNNAMED")
}

tasks.withType<Test> {
    jvmArgs("--add-opens=java.base/java.nio=ALL-UNNAMED")
}

tasks.withType<JavaExec> {
    jvmArgs("--add-opens=java.base/java.nio=ALL-UNNAMED")
}
