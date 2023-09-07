FROM gcr.io/distroless/java11@sha256:a4dcd554d29a3977a57eba4e8305867f6a7f231261202e4fc93359642ef73807
COPY target/ms-identity-status-*.jar /ms-identity-status.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/ms-identity-status.jar"]
