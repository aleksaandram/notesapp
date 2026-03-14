pipeline {
    agent any

    tools {
        maven 'maven'
    }

    environment {
        NEXUS_URL = 'http://nexus:8081'
        APP_NAME = 'notesapp'
        APP_VERSION = '0.0.1-SNAPSHOT'
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
                sh 'docker build -t localhost:8082/${APP_NAME}:${APP_VERSION} .'
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
                sh '''
                    docker login localhost:8082 -u admin -p Kloi12345
                    docker push localhost:8082/${APP_NAME}:${APP_VERSION}
                '''
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}