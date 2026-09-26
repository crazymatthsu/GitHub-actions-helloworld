# syntax=docker/dockerfile:1

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

FROM corretto
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
COPY --from=build --chown=app:app /src/app.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
