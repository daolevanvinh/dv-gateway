FROM openjdk:24-ea-24-slim-bookworm AS build

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY gradle.properties ./

# Install curl and nano in the build stage
RUN apt-get update && \
    apt-get install -y curl nano && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN chmod +x ./gradlew

RUN ./gradlew dependencies
COPY src src

RUN ./gradlew build -x test

FROM openjdk:24-ea-24-slim-bookworm
WORKDIR /app

# Install curl and nano in the final stage
RUN apt-get update && \
    apt-get install -y curl nano && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=90.0", "-jar", "app.jar"]