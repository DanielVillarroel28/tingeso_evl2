FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/loan-service.jar
COPY ${JAR_FILE} loan-service.jar
ENTRYPOINT ["java", "-jar", "/loan-service.jar"]