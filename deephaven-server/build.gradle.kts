plugins {
    base
}

val verifyCompose = tasks.register("verifyCompose") {
    val composeFile = layout.projectDirectory.file("docker-compose.yml")
    inputs.file(composeFile)
    doLast {
        val text = composeFile.asFile.readText()
        check(text.contains("ghcr.io/deephaven/server:42.4")) {
            "docker-compose.yml must run Deephaven Community 42.4"
        }
    }
}

tasks.named("check") {
    dependsOn(verifyCompose)
}
