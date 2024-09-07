FROM amazoncorretto:21-alpine-jdk
COPY build/libs/spring-auth-template-0.0.1.jar spring-auth-template.jar

EXPOSE 8082

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "spring-auth-template.jar"]