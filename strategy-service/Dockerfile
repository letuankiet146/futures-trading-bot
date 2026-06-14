FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY shared-contracts/pom.xml shared-contracts/pom.xml
COPY strategy-service/pom.xml strategy-service/pom.xml
COPY execute-service/pom.xml execute-service/pom.xml
COPY simulate-service/pom.xml simulate-service/pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY . .
RUN mvn -q -DskipTests -pl strategy-service -am clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/strategy-service/target/strategy-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
