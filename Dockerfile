FROM openjdk:17-jdk-alpine
MAINTAINER Burduzhan Ruslan
VOLUME /main-app
ADD target/catsbot-0.0.1-SNAPSHOT.jar catsbot.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","catsbot.jar"]