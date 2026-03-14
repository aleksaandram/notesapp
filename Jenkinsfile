pipeline {
    agent any

    tools {
        maven 'maven'
    }

    environment {
        NEXUS_URL = 'http://nexus:8081'
        APP_NAME = 'notesapp'
        APP_VERSION = '0.0.1-SNAPSHOT'
        DOCKER_REGISTRY = 'nexus:8082'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code....'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building application...'
                sh 'mvn -Dmaven.wagon.http.retryHandler.count=3 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 clean package -DskipTests'
            }
        }

        stage('Integration Test') {
            steps {
                echo 'Running Integration Tests...'
                sh '''
                    java -jar target/${APP_NAME}-${APP_VERSION}.jar --server.port=8888 &
                    APP_PID=$!

                    echo "Waiting for app to start..."
                    sleep 15

                    # Test 1: Home endpoint
                    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8888/)
                    echo "Home endpoint status: $STATUS"

                    # Test 2: Notes endpoint
                    STATUS2=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8888/api/notes)
                    echo "Notes endpoint status: $STATUS2"

                    if [ "$STATUS" = "200" ] && [ "$STATUS2" = "200" ]; then
                        echo "All integration tests passed!"
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
                sh 'docker build -t ${APP_NAME}:${APP_VERSION} .'
            }
        }

        stage('Deploy to Nexus') {
            steps {
                echo 'Deploying artifact to Nexus...'
                sh '''
                    curl -u admin:Kloi12345 \
                    --upload-file target/${APP_NAME}-${APP_VERSION}.jar \
                    http://nexus:8081/repository/maven-snapshots/org/example/${APP_NAME}/${APP_VERSION}/${APP_NAME}-${APP_VERSION}.jar
                '''
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
                    sh '''
                        echo $NEXUS_PASS | docker login host.docker.internal:8084 -u $NEXUS_USER --password-stdin
                        docker tag ${APP_NAME}:${APP_VERSION} host.docker.internal:8084/${APP_NAME}:${APP_VERSION}
                        docker push host.docker.internal:8084/${APP_NAME}:${APP_VERSION}
                    '''
                }
            }
        }

       stage('Deploy Green') {
           steps {
               echo 'Deploying to GREEN environment...'
               sh '''
                   docker rm -f app_green || true
                   docker run -d --name app_green \
                       --network notesapp_app-net \
                       -p 8086:8080 \
                       host.docker.internal:8084/${APP_NAME}:${APP_VERSION}
                   echo "Waiting for GREEN to start..."
                   sleep 15
               '''
           }
       }

               stage('Smoke Test Green') {
                   steps {
                       echo 'Smoke testing GREEN...'
                       sh '''
                           STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://app_green:8080/notes)
                           echo "GREEN status: $STATUS"
                           if [ "$STATUS" = "200" ] || [ "$STATUS" = "204" ]; then
                               echo "GREEN is healthy!"
                           else
                               echo "GREEN failed!"
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
                       echo 'Stopping old BLUE environment...'
                       sh '''
                           docker rm -f app_blue || true
                           docker run -d --name app_blue \
                               --network notesapp_app-net \
                               -p 8085:8080 \
                               ${DOCKER_REGISTRY}/${APP_NAME}:${APP_VERSION}
                           echo "BLUE updated and ready for next deployment!"
                       '''
                   }
               }
    }

    post {
           success {
               echo 'Blue-Green deployment completed successfully!'
           }
           failure {
               echo 'Deployment failed! Rolling back to BLUE...'
               sh '''
                   docker cp nginx/nginx-blue.conf nginx_proxy:/etc/nginx/nginx.conf
                   docker exec nginx_proxy nginx -s reload
                   echo "Rolled back to BLUE!"
               '''
           }
       }
   }