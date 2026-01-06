FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/fine-service.jar
COPY ${JAR_FILE} fine-service.jar
ENTRYPOINT ["java", "-jar", "/fine-service.jar"]