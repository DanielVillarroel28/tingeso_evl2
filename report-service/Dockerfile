FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/report-service.jar
COPY ${JAR_FILE} report-service.jar
ENTRYPOINT ["java", "-jar", "/report-service.jar"]