FROM openjdk:17-jdk-alpine

MAINTAINER agodkidaniil@gmail.com
LABEL version="1" authors="Daniil Yagodkin", name="SearchingMachine"

WORKDIR /app
COPY target/SearchEngine-1.0-SNAPSHOT.jar /app/SearchEngine-1.0-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "SearchEngine-1.0-SNAPSHOT.jar"]