## Start from an official maven image
# FROM maven:3-openjdk-11-slim  # Version issue
# FROM maven:3-openjdk-18  # No method to install X11?
# FROM maven:3-jdk-11  # Unsupported major.minor version 61.0
# FROM maven:3-jdk-11  # Unsupported major.minor version 61.0
FROM maven:3.8.6-ibm-semeru-17-focal


LABEL maintainer="MDW <MDW@private.fr>"

## Set environment variables
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8


RUN apt-get update && apt-get install -y --no-install-recommends openjfx && rm -rf /var/lib/apt/lists/*

# && mvn clean install && DISPLAY=localhost:0.0

# Not on Debian: firefox firefox-geckodriver

COPY pom.xml /tmp/pom.xml
RUN mvn -B -f /tmp/pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
# RUN mvn -B -f /tmp/pom.xml -s /usr/share/maven/ref/settings-docker.xml javafx:resolve

# RUN mvn -B -f /tmp/pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve
# RUN mvn -B -f /tmp/pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve

