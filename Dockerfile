FROM node:20-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "apk add --no-cache openjdk21-jre && java -jar app.jar"]