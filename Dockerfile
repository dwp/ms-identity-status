FROM gcr.io/distroless/java17-debian12@sha256:839615e6fb501595ff8091bc91587cc43afcd2cb09d29e9b75b1f9ff48a9ef52
COPY target/ms-identity-status-*.jar /ms-identity-status.jar

COPY --from=pik94420.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

EXPOSE 8080

#Run bash script
USER nonroot

ENTRYPOINT ["java", "-jar", "/ms-identity-status.jar", "\"$@\""]
