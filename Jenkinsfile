pipeline {
    agent any

    environment {
        NEXUS_URL = 'http://nexus:8081'
        NEXUS_REPO = 'maven-releases'
        APP_NAME = 'notesapp'
        APP_VERSION = '0.0.1-SNAPSHOT'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out code from Gitea...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building application...'
                sh './mvnw clean package -DskipTests -f pom.xml'
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                sh './mvnw test -f pom.xml'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image...'
                sh 'docker build -t localhost:8082/${APP_NAME}:${APP_VERSION} .'
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

        stage('Deploy') {
            steps {
                echo 'Deploying application...'
                sh 'docker compose up -d --force-recreate backend frontend'
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