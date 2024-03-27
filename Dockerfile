FROM gcr.io/distroless/java17@sha256:2f01c2ff0c0db866ed73085cf1bb5437dd162b48526f89c1baa21dd77ebb5e6d
COPY target/ms-identity-status-*.jar /ms-identity-status.jar

#COPY --from=pik94420.live.dynatrace.com/linux/oneagent-codemodules:java / /
#ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/ms-identity-status.jar"]
