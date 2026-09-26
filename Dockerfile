# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
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

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --uid 10001 app
COPY --from=build --chown=app:app /src/app.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
