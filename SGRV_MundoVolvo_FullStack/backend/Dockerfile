FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B -q test package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 sgrv
COPY --from=builder /build/target/sgrv-api-1.0.0.jar app.jar
USER sgrv
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
