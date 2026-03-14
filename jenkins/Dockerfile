FROM jenkins/jenkins:lts

USER root

# Install Docker CLI
RUN apt-get update && \
    apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release && \
    curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce-cli && \
    apt-get clean

# Configure insecure registry for Docker
RUN mkdir -p /etc/docker && \
    echo '{"insecure-registries": ["localhost:8082"]}' > /etc/docker/daemon.json

# Add jenkins user to docker group
RUN groupadd -f docker && usermod -aG docker jenkins

ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=$JAVA_HOME/bin:$PATH
ENV JAVA_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3 -Dhudson.model.DownloadService.noSignatureCheck=true"
ENV MAVEN_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3"

USER jenkins