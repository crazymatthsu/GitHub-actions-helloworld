import com.google.protobuf.gradle.id
import org.gradle.api.plugins.jvm.JvmTestSuite

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(project(":framework"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.grpc.server)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.protoc.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                id("grpc")
            }
        }
    }
}

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(libs.spring.boot.starter.test)
                implementation(libs.spring.boot.starter.grpc.server.test)
                implementation(libs.grpc.netty)
                runtimeOnly("org.junit.platform:junit-platform-launcher")
            }
            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(tasks.named("test"))
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}
