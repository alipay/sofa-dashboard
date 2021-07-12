FROM java:8

COPY *.jar /app.jar

CMD ["--server.port=8099"]

EXPOSE 8099

ENTRYPOINT ["java","-jar","/app.jar"]