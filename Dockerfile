FROM gcr.io/distroless/java11@sha256:e97b56c50a651ec41a1e40cf2f8f96eb7ce4a4f42e1094e8462ab47a949c79e9
COPY target/ms-identity-status-*.jar /ms-identity-status.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/ms-identity-status.jar"]
