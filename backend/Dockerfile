# --- Étape 1 : build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Étape 2 : image finale, légère ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S nexa && adduser -S nexa -G nexa
COPY --from=build /app/target/nexa-backend-*.jar app.jar
RUN mkdir -p /app/uploads && chown -R nexa:nexa /app
USER nexa
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
