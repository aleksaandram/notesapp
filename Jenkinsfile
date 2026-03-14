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
                       docker pull ${DOCKER_REGISTRY}/${APP_NAME}:latest
                       docker rm -f app_green || true
                       docker run -d --name app_green \
                           --network notesapp_app-net \
                           -e COLOR=GREEN \
                           -p 8086:8080 \
                           ${DOCKER_REGISTRY}/${APP_NAME}:latest
                       echo "Waiting for GREEN to start..."
                       sleep 30
                   """
               }
           }
       }


      stage('Smoke Test Green') {
          steps {
              sh '''
                  echo "Smoke testing GREEN..."
                  GREEN_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' app_green)
                  echo "GREEN IP: $GREEN_IP"
                  for i in $(seq 1 20); do
                      STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://${GREEN_IP}:8080/api/notes)
                      echo "Attempt $i - Status: $STATUS"
                      if [ "$STATUS" = "200" ] || [ "$STATUS" = "204" ]; then
                          echo "GREEN is healthy!"
                          exit 0
                      fi
                      sleep 5
                  done
                  echo "GREEN failed!"
                  exit 1
              '''
          }
      }

        stage('Switch Traffic to Green') {
             steps {
                 echo 'Switching traffic to GREEN...'
                 sh '''
                     docker exec nginx_proxy sh -c "echo 'upstream app { server app_green:8080; } server { listen 80; location / { proxy_pass http://app; } }' > /etc/nginx/conf.d/default.conf"
                     docker exec nginx_proxy nginx -s reload
                     echo "Traffic switched to GREEN!"
                 '''
             }
         }

        stage('Cleanup Blue') {
                   steps {
                       echo 'Updating BLUE...'
                       sh '''
                           docker rm -f app_blue || true
                           docker run -d --name app_blue \
                               --network notesapp_app-net \
                               -e COLOR=BLUE \
                               -p 8085:8080 \
                               ${DOCKER_REGISTRY}/${APP_NAME}:latest
                       '''
                   }
               }
           }

           post {
               success {
                   echo 'Blue-Green deployment completed successfully!'
               }
               failure {
                   echo 'Rolling back to BLUE...'
                   sh '''
                       docker exec nginx_proxy sh -c "echo 'upstream app { server app_blue:8080; } server { listen 80; location / { proxy_pass http://app; } }' > /etc/nginx/conf.d/default.conf"
                       docker exec nginx_proxy nginx -s reload || true
                   '''
               }
           }
       }