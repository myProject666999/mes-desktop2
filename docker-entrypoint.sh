#!/bin/bash

Xvfb :1 -screen 0 1920x1080x24 &
sleep 1

fluxbox -display :1 &
sleep 1

x11vnc -display :1 -forever -shared -passwd ${VNC_PASSWORD} -bg

mkdir -p /usr/share/novnc
cd /usr/share/novnc
websockify --web=/usr/share/novnc 6080 localhost:5900 &

sleep 2

cd /app
java --module-path /opt/javafx-sdk-21.0.2/lib --add-modules javafx.controls,javafx.fxml \
     --add-exports javafx.graphics/com.sun.javafx.util=ALL-UNNAMED \
     -jar app.jar
