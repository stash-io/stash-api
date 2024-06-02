FROM gradle:8.7.0-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

FROM openjdk:21
EXPOSE 5482:5482
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/stash.jar
COPY --from=build /home/gradle/src/www /www
ENTRYPOINT ["java","-jar","/app/stash.jar"]