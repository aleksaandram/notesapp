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
                echo 'Building application...'
                sh '''
                    find /var/jenkins_home/.m2/repository -name "_remote.repositories" -delete
                    mvn clean package -DskipTests -o -s settings.xml -Dmaven.repo.local=/var/jenkins_home/.m2/repository
                '''
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

        stage('Build Frontend Docker Image') {
            steps {
                echo 'Building Frontend Docker image...'
                sh """
                docker build -t ${DOCKER_REGISTRY}/notesapp-frontend:1.0.${BUILD_NUMBER} .
                docker tag ${DOCKER_REGISTRY}/notesapp-frontend:1.0.${BUILD_NUMBER} ${DOCKER_REGISTRY}/notesapp-frontend:latest
                """
            }
        }

        stage('Push Frontend Docker Image to Nexus') {
            steps {
                echo 'Pushing Frontend Docker image to Nexus...'
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-docker',
                    usernameVariable: 'NEXUS_USER',
                    passwordVariable: 'NEXUS_PASS'
                )]) {
                    sh '''
                        echo $NEXUS_PASS | docker login ${DOCKER_REGISTRY} -u $NEXUS_USER --password-stdin
                        docker push ${DOCKER_REGISTRY}/notesapp-frontend:1.0.${BUILD_NUMBER}
                        docker push ${DOCKER_REGISTRY}/notesapp-frontend:latest
                    '''
                }
            }
        }


           post {
            always {
                      echo 'Pipeline finished.'
             }
               success {
                   echo 'Blue-Green deployment completed successfully!'
               }
               failure {
                             echo 'Something failed. Check the logs.'
                             sh '''
                               echo "ROLLBACK -> switching Nginx back to BLUE"
                               docker exec nginx_proxy sh -lc "cp /etc/nginx/nginx-blue.conf /etc/nginx/conf.d/default.conf" || true
                               docker exec nginx_proxy nginx -s reload || true
                             '''
                           }
           }
       }