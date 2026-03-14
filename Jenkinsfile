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
                sh 'mvn failsafe:integration-test failsafe:verify -s settings.xml'
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                }
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