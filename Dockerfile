FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="MES System"

USER root

ENV DEBIAN_FRONTEND=noninteractive
ENV DISPLAY=:1
ENV VNC_PASSWORD=mes123456
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

RUN apt-get update && apt-get install -y \
    libx11-6 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libgl1-mesa-glx \
    libgtk-3-0 \
    libxrandr2 \
    libxcursor1 \
    libxdamage1 \
    libxinerama1 \
    x11vnc \
    xvfb \
    fluxbox \
    wget \
    unzip \
    python3 \
    python3-pip \
    python3-numpy \
    fonts-wqy-zenhei \
    fonts-wqy-microhei \
    fonts-noto-cjk \
    && pip3 install websockify \
    && rm -rf /var/lib/apt/lists/*

RUN wget -q https://download2.gluonhq.com/openjfx/21.0.2/openjfx-21.0.2_linux-x64_bin-sdk.zip -O /tmp/openjfx.zip && \
    unzip /tmp/openjfx.zip -d /opt && \
    rm /tmp/openjfx.zip

ENV PATH_TO_FX=/opt/javafx-sdk-21.0.2/lib

WORKDIR /app

COPY target/javafx-mes-1.0.0.jar /app/app.jar

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 5900 6080

ENTRYPOINT ["/docker-entrypoint.sh"]
