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
COPY framework framework
COPY sb-hello-world sb-hello-world
COPY deephaven-server deephaven-server
COPY deephaven-sender deephaven-sender
RUN chmod +x gradlew \
    && ./gradlew :deephaven-sender:installDist --no-daemon -x test

FROM corretto
COPY --from=build /src/deephaven-sender/build/install/deephaven-sender /opt/deephaven-sender
ENTRYPOINT ["/opt/deephaven-sender/bin/deephaven-sender"]
