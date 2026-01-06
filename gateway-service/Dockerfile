FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/gateway-service.jar
COPY ${JAR_FILE} gateway-service.jar
ENTRYPOINT ["java", "-jar", "/gateway-service.jar"]