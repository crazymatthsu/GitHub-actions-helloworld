# Dockerfile

The `Dockerfile` builds the app in one image, then throws that image away and ships a smaller one that only runs the jar. Read it from top to bottom. Each `FROM` starts a new stage, and only the last stage is the image that gets pushed.

## How to read it

A Dockerfile is a recipe of instructions. Docker runs them in order and saves the result of each one as a layer.

| Instruction | What it means |
| --- | --- |
| `FROM` | Start from an existing image. A new `FROM` starts a new stage. |
| `AS name` | Name that stage so a later stage can copy files out of it. |
| `ENV` | Set an environment variable for later instructions and for the running container. |
| `RUN` | Run a shell command while building the image. |
| `COPY` | Copy files from the project into the image. |
| `WORKDIR` | Set the current directory for the instructions that follow. |
| `USER` | Which user the container process runs as. |
| `EXPOSE` | Document the port the app listens on. It does not publish the port. |
| `ENTRYPOINT` | The command that runs when the container starts. |

There are three stages. The first is named `corretto`. The second is named `build` and starts from `corretto`. The third has no name and also starts from `corretto`. The third one is the image GitHub publishes.

## One base, two uses

One Dockerfile produces two different images from the same Ubuntu and Corretto base. Only the last one is published and used to run the app.

The first stage, named `corretto`, is Ubuntu 24.04 with Amazon Corretto 21 installed. Nothing is compiled there.

The `build` stage starts from that base, copies the source, and runs Gradle to produce `app.jar`. That stage is only a temporary workspace. It is not tagged or pushed to GHCR, and it is discarded after the jar is copied out.

The last stage starts again from the same `corretto` base, not from the build stage. It copies in only `app.jar` and runs `java -jar`. The source tree and the Gradle cache stay behind.

The JDK base is shared, but the image you run does not contain the build. Both stages have the full Corretto JDK because Amazon does not publish a separate JRE package.

## Stage 1: Ubuntu plus Corretto 21

```dockerfile
FROM ubuntu:24.04 AS corretto
ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
    && curl -fsSL https://apt.corretto.aws/corretto.key \
        | gpg --dearmor -o /usr/share/keyrings/corretto-keyring.gpg \
    && echo "deb [signed-by=/usr/share/keyrings/corretto-keyring.gpg] https://apt.corretto.aws stable main" \
        > /etc/apt/sources.list.d/corretto.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends java-21-amazon-corretto-jdk \
    && apt-get purge -y --auto-remove curl gnupg \
    && rm -rf /var/lib/apt/lists/*
ENV PATH="${JAVA_HOME}/bin:${PATH}"
```

This starts from Ubuntu 24.04 and installs Amazon Corretto 21 from Amazon's apt repository. The official `amazoncorretto:21` tag is Amazon Linux 2023, not Ubuntu, so the JDK is installed onto Ubuntu instead of using that tag.

`JAVA_HOME` points at `/usr/lib/jvm/java-21-amazon-corretto`, and that directory is put on `PATH`, so `java` and `javac` are Corretto.

The `RUN` line is one layer. It adds Amazon's signing key, adds the Corretto repository, installs `java-21-amazon-corretto-jdk`, then deletes `curl`, `gnupg`, and the apt package lists so they are not left in the image.

## Stage 2: compile the jar

```dockerfile
FROM corretto AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY framework/build.gradle.kts framework/build.gradle.kts
COPY sb-hello-world/build.gradle.kts sb-hello-world/build.gradle.kts
RUN chmod +x gradlew && ./gradlew :sb-hello-world:dependencies --no-daemon

COPY framework framework
COPY sb-hello-world sb-hello-world
RUN ./gradlew :sb-hello-world:bootJar --no-daemon -x test \
    && find sb-hello-world/build/libs -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} /src/app.jar \; \
    && test -s /src/app.jar
```

`FROM corretto AS build` takes the Ubuntu and Corretto image and keeps going. Source code is copied in and Gradle builds `:sb-hello-world:bootJar`.

The copies are split on purpose. The first copy is only the Gradle build files. `./gradlew :sb-hello-world:dependencies` downloads libraries. That layer stays cached until those build files change. The second copy brings in the Java source, and the next `RUN` compiles it. A source-only change does not download dependencies again.

`-x test` skips tests inside the image. GitHub Actions already runs them before this image is built. The `find` line copies the executable Spring Boot jar to `/src/app.jar` and skips the `*-plain.jar` file, which is a library jar and cannot be started with `java -jar`. `test -s` fails the build if that file is missing or empty.

## Stage 3: the image that actually runs

```dockerfile
FROM corretto
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
COPY --from=build --chown=app:app /src/app.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

This `FROM corretto` starts again from the Ubuntu and Corretto image. It does not contain the Gradle cache or the source tree from the build stage. The only file taken from that stage is `/src/app.jar`, copied in with `COPY --from=build`.

The process runs as the user `app`, not as root. `EXPOSE 8080` records that the app listens on port 8080. Starting the container runs `java -jar /app/app.jar`, which starts the Spring Boot app.

## `USER app`

`app` is just the Linux username this image creates. Another name works, as long as that same name is used everywhere the user is created and referenced.

```dockerfile
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
COPY --from=build --chown=app:app /src/app.jar /app/app.jar
USER app
```

Those three `app` names are the group, the user, and the owner of the jar. `USER app` only works because `useradd` created that account on the line above. Changing it to `hello` means changing all three spots together:

```dockerfile
RUN groupadd --system hello && useradd --system --gid hello --uid 10001 hello
COPY --from=build --chown=hello:hello /src/app.jar /app/app.jar
USER hello
```

The `/app` in `/app/app.jar` is a directory, set by `WORKDIR /app`. It is not the username, so that path can stay as it is.

A username has to be a normal Linux account name: it usually starts with a letter, and then uses letters, digits, `_`, or `-`. `USER 10001` also works, because that is the numeric id from `--uid 10001`, but the account still has to exist.

## Gradle on the host does not use this image

`./gradlew build` on your machine does not open the Dockerfile. It uses the JDK already installed on that computer.

On a push to `main`, the pipeline compiles twice, with two different installations of Amazon Corretto 21.

1. **Build and JUnit** runs on the GitHub Actions machine. `actions/setup-java` installs Corretto 21 there, and `./gradlew build` compiles both projects and runs the tests. The jars from this job are not passed to Docker.
2. **Publish to GHCR** runs `docker build`. Inside the Ubuntu and Corretto image, Gradle runs again: first `:sb-hello-world:dependencies`, then `:sb-hello-world:bootJar -x test`. That second compile exists only to produce `app.jar` for the image that gets pushed. `-x test` skips the tests, because they already ran in the first job.

A pull request runs only the first job. The Docker rebuild happens on a push to `main`, on a `v*` tag, or when the publish workflow is started by hand.

## Why the image does not copy the Gradle jar

The rebuild is not required. The Dockerfile does it because `docker build` only sees the source files, not the jar from `./gradlew build`.

The publish job checks out the repo and runs `docker build`. It does not download the jar produced by **Build and JUnit**. Those jobs run on separate machines, so the first jar never reaches the second machine. `.dockerignore` also excludes every `build/` directory, so a jar built on your own computer is left out of the Docker build as well.

The image build therefore runs Gradle itself, inside Ubuntu with Corretto, and keeps only `app.jar`. That makes `docker build` work on its own, without a JDK installed on the host. The cost is a second compile. Tests are not repeated.

Copying a prebuilt jar is a normal alternative. The test job would publish `app.jar`, the publish job would download it, and the Dockerfile would only install Corretto and copy that file in. The image would no longer be buildable from source alone.

## What stays installed, and which shell `RUN` uses

`ca-certificates` stays in the image. The first `apt-get install` installs `ca-certificates`, `curl`, and `gnupg`. The later `apt-get purge` removes only `curl` and `gnupg`. `--no-install-recommends` skips extra recommended packages; it still installs the three packages named on that line. Both the build stage and the final image start from `corretto`, so both keep the CA bundle.

Ubuntu 24.04 includes Bash at `/bin/bash`. Docker does not use Bash for `RUN` lines unless the Dockerfile says so. The default is `/bin/sh -c`, and on Ubuntu `/bin/sh` is Dash, not Bash. The commands in this file only use `&&` and pipes, which Dash supports. The container's main process is still `java -jar`, not a shell. Bash is there if you start one yourself, for example `docker exec -it <container> bash`.
