FROM jenkins/jenkins:lts

USER root

# Install Java 17 Temurin
RUN apt-get update && \
    apt-get install -y wget apt-transport-https && \
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add - && \
    echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y temurin-17-jdk && \
    apt-get clean

# Install Docker CLI
RUN apt-get update && \
    apt-get install -y docker.io && \
    apt-get clean

# Set Java 17 as default
ENV JAVA_HOME=/usr/lib/jvm/temurin-17
ENV PATH=$JAVA_HOME/bin:$PATH

ENV JAVA_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3 -Dhudson.model.DownloadService.noSignatureCheck=true"
ENV MAVEN_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3"

USER jenkins