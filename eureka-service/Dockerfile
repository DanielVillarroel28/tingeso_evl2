FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/eureka-service.jar
COPY ${JAR_FILE} eureka-service.jar
ENTRYPOINT ["java", "-jar", "/eureka-service.jar"]