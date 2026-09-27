plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "github-actions-helloworld"

include("framework")
include("sb-hello-world")
include("deephaven-server")
include("deephaven-sender")
