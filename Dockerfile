FROM amazoncorretto:17.0.11-alpine3.17@sha256:73510f7ea8cf975f6fe0b76edb517f23a7b695cd379eb944d6bd3b9f7231c39e
COPY target/ms-identity-status-*.jar /ms-identity-status.jar

COPY --from=pik94420.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/ms-identity-status.jar"]
