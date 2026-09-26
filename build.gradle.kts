plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
}

subprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
            }
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        dependencies {
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }
    }
}
