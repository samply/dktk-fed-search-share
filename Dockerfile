FROM openjdk:17

RUN microdnf upgrade

COPY target/dktk-fed-search-share.jar /app/

WORKDIR /app
USER 1001

CMD ["java", "-jar", "dktk-fed-search-share.jar"]
