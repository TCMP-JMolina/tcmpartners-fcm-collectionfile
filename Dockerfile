FROM gradle:7.6.0-jdk21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ src/

RUN mvn package -DskipTests

FROM amazoncorretto:17.0.8-alpine3.18

WORKDIR /app

COPY --from=build /app/target/collect-24.0828.jar app.jar

EXPOSE 80

CMD ["java", "-jar", "/app/app.jar"]