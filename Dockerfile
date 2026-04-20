FROM eclipse-temurin:8-jdk

RUN apt-get update && apt-get install -y \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    xauth \
    x11-apps \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /usr/src/app

COPY target/si-sma-jade-1.0-SNAPSHOT.jar app.jar

ENV DISPLAY=host.docker.internal:0.0

ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=false"