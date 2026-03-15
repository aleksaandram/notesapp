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
                    docker build -t localhost:8082/docker-hosted/notesapp:1.0.${BUILD_NUMBER} .
                    docker tag localhost:8082/docker-hosted/notesapp:1.0.${BUILD_NUMBER} localhost:8082/docker-hosted/notesapp:latest
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
                        docker login localhost:8082 -u $NEXUS_USER -p $NEXUS_PASS
                        docker push localhost:8082/docker-hosted/notesapp:1.0.${BUILD_NUMBER}
                        docker push localhost:8082/docker-hosted/notesapp:latest
                    """
                }
            }
        }

        stage('Build Frontend Docker Image') {
            steps {
                echo 'Building Frontend Docker image...'
                sh """
                docker build -t localhost:8082/docker-hosted/notesapp-frontend:1.0.${BUILD_NUMBER} .
                docker tag localhost:8082/docker-hosted/notesapp-frontend:1.0.${BUILD_NUMBER} localhost:8082/docker-hosted/notesapp-frontend:latest
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
                        docker login localhost:8082 -u $NEXUS_USER -p $NEXUS_PASS
                        docker push localhost:8082/docker-hosted/notesapp-frontend:1.0.${BUILD_NUMBER}
                        docker push localhost:8082/docker-hosted/notesapp-frontend:latest
                    '''
                }
            }
        }



       stage('Deploy Green') {
           steps {
               echo 'Deploying to GREEN environment...'
               withCredentials([usernamePassword(
                   credentialsId: 'nexus-docker',
                   usernameVariable: 'NEXUS_USER',
                   passwordVariable: 'NEXUS_PASS'
               )]) {
                   sh """
                       echo \$NEXUS_PASS | docker login ${DOCKER_REGISTRY} -u \$NEXUS_USER --password-stdin
                       docker pull localhost:8082/docker-hosted/notesapp:latest
                       docker rm -f app_green || true
                       docker run -d --name app_green \\
                           --network notesapp_app-net \\
                           -e COLOR=GREEN \
                           localhost:8082/docker-hosted/notesapp:latest
                       echo "Waiting for GREEN to start..."
                       sleep 30
                   """
               }
           }
       }


     stage('Smoke Test Green') {
         steps {
             sh '''
                echo "Smoke testing GREEN at http://app_green:8080 ..."
                               for i in $(seq 1 15); do
                                 if curl -fsS --max-time 2 http://app_green:8080/ > /dev/null; then
                                   echo "GREEN is healthy!"
                                   exit 0
                                 fi
                                 echo "Waiting for GREEN... ($i/15)"
                                 sleep 2
                               done
                               echo "GREEN failed health check!"
                               exit 1
             '''
         }
     }

        stage('Switch Traffic to Green') {
             steps {
                 echo 'Switching traffic to GREEN...'
                 sh '''
                     echo "Switching Nginx config to GREEN..."
                     docker exec nginx_proxy sh -lc "cp /etc/nginx/nginx-green.conf /etc/nginx/conf.d/default.conf"
                     docker exec nginx_proxy nginx -t
                     docker exec nginx_proxy nginx -s reload
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