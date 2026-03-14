pipeline {
    agent any

    tools {
        maven 'maven'
    }

    environment {
        NEXUS_URL = 'http://nexus:8081'
        APP_NAME = 'notesapp'
        APP_VERSION = '0.0.1-SNAPSHOT'
        DOCKER_REGISTRY = 'host.docker.internal:8084'
        MAVEN_OPTS = '-Dhttps.protocols=TLSv1.2 -Dmaven.wagon.http.pool=false -Dmaven.wagon.http.retryHandler.count=3'
        MAVEN_ARGS = '-Dmaven.repo.local=.m2/repository -B -U'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Cleaning possible corrupted Maven cache...'
                sh '''
                    mkdir -p .m2/repository
                    rm -rf .m2/repository/org/springframework/boot || true
                '''

                retry(3) {
                    sh '''
                        mvn $MAVEN_ARGS clean package -DskipTests
                    '''
                }
            }
        }

        stage('Integration Test') {
            steps {
                echo 'Running Integration Tests...'
                sh '''
                    java -jar target/${APP_NAME}-${APP_VERSION}.jar --server.port=8888 &
                    APP_PID=$!
                    echo "Waiting for app to start..."
                    sleep 20

                    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8888/api/notes)
                    echo "Notes endpoint status: $STATUS"

                    if [ "$STATUS" = "200" ] || [ "$STATUS" = "204" ]; then
                        echo "Integration tests passed!"
                        kill $APP_PID
                    else
                        echo "Integration tests failed!"
                        kill $APP_PID
                        exit 1
                    fi
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                sh """
                    docker build -t ${DOCKER_REGISTRY}/${APP_NAME}:1.0.${BUILD_NUMBER} .
                    docker tag ${DOCKER_REGISTRY}/${APP_NAME}:1.0.${BUILD_NUMBER} ${DOCKER_REGISTRY}/${APP_NAME}:latest
                """
            }
        }

        stage('Deploy to Nexus') {
            steps {
                echo 'Deploying artifact to Nexus...'
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-docker',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    sh '''
                        curl -u $NEXUS_USER:$NEXUS_PASS \
                        --upload-file target/${APP_NAME}-${APP_VERSION}.jar \
                        ${NEXUS_URL}/repository/maven-snapshots/org/example/${APP_NAME}/${APP_VERSION}/${APP_NAME}-${APP_VERSION}.jar
                    '''
                }
            }
        }

        stage('Push Docker Image to Nexus') {
            steps {
                echo 'Pushing Docker image to Nexus...'
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-docker',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    sh """
                        echo \$NEXUS_PASS | docker login ${DOCKER_REGISTRY} -u \$NEXUS_USER --password-stdin
                        docker push ${DOCKER_REGISTRY}/${APP_NAME}:1.0.${BUILD_NUMBER}
                        docker push ${DOCKER_REGISTRY}/${APP_NAME}:latest
                    """
                }
            }
        }

        stage('Deploy Green') {
            steps {
                echo 'Deploying to GREEN environment...'
                sh """
                    docker rm -f app_green || true
                    docker run -d --name app_green \
                        --network notesapp_app-net \
                        -e COLOR=GREEN \
                        -p 8086:8080 \
                        ${DOCKER_REGISTRY}/${APP_NAME}:latest
                    echo "Waiting for GREEN to start..."
                    sleep 15
                """
            }
        }

        stage('Smoke Test Green') {
            steps {
                echo 'Smoke testing GREEN...'
                sh '''
                    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8086/api/notes)
                    echo "GREEN status: $STATUS"
                    if [ "$STATUS" = "200" ] || [ "$STATUS" = "204" ]; then
                        echo "GREEN is healthy!"
                    else
                        echo "GREEN failed with status: $STATUS"
                        exit 1
                    fi
                '''
            }
        }

        stage('Switch Traffic to Green') {
            steps {
                echo 'Switching traffic from BLUE to GREEN...'
                sh '''
                    docker cp nginx/nginx-green.conf nginx_proxy:/etc/nginx/nginx.conf
                    docker exec nginx_proxy nginx -t
                    docker exec nginx_proxy nginx -s reload
                    echo "Traffic switched to GREEN!"
                '''
            }
        }

        stage('Cleanup Blue') {
            steps {
                echo 'Updating BLUE environment...'
                sh """
                    docker rm -f app_blue || true
                    docker run -d --name app_blue \
                        --
