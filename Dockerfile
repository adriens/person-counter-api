FROM openjdk
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} PersonCounterAPI.jar
ENTRYPOINT ["java","-jar","/PersonCounterAPI.jar"]