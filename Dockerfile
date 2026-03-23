FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --gid 1001 app \
    && useradd --uid 1001 --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
RUN chown -R app:app /app
USER app
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar"]
