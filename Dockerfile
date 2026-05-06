FROM eclipse-temurin:17-jre

ARG MODULE
ARG VERSION=0.1.0-SNAPSHOT

ENV JAVA_OPTS=""
WORKDIR /app

RUN groupadd --system emall \
    && useradd --system --gid emall --home-dir /app --shell /usr/sbin/nologin emall

COPY ${MODULE}/target/${MODULE}-${VERSION}.jar /app/app.jar
RUN chown -R emall:emall /app

USER emall

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
